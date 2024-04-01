package org.figuramc.figura.mixin.sound;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.jcraft.jorbis.Info;
import io.netty.util.internal.ThreadLocalRandom;
import net.minecraft.client.audio.*;
import net.minecraft.client.gui.GuiSubtitleOverlay;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.SoundAccessor;
import org.figuramc.figura.ducks.SoundManager$SoundSystemStarterThreadAccessor;
import org.figuramc.figura.ducks.SoundManagerAccessor;
import org.figuramc.figura.ducks.SubtitleOverlayAccessor;
import org.figuramc.figura.lua.api.sound.LuaSound;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.permissions.Permissions;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import paulscode.sound.SoundSystemConfig;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;

@Mixin(SoundManager.class)
public abstract class SoundManagerMixin implements SoundManagerAccessor {
    @Shadow private SoundManager.SoundSystemStarterThread sndSystem;
    @Shadow private boolean loaded;
    @Shadow protected abstract float getVolume(@Nullable SoundCategory category);
    @Shadow @Final private List<ISoundEventListener> listeners;

    @Unique
    private final Map<String, LuaSound> figura$playingSounds = HashBiMap.create();

    @Unique
    private final Map<LuaSound, String> figura$invPlayingSounds = ((BiMap)this.figura$playingSounds).inverse();
    @Unique
    private final Map<String, Integer> figura$playingSoundsStopTime = new HashMap<>();
    @Unique
    private final Map<LuaSound, Integer> figura$delayedSounds = new HashMap<>();
    @Unique
    private final List<String> figura$pausedSounds =  new ArrayList<>();

    @Unique
    private final Multimap<SoundCategory, String> figura$categorySounds = HashMultimap.create();
    @Shadow private int playTime;
    @Shadow @Final private static Marker LOG_MARKER;
    @Shadow @Final private SoundHandler sndHandler;
    @Shadow @Final private static Set<ResourceLocation> UNABLE_TO_PLAY;
    @Shadow @Final private static Logger LOGGER;

    @Shadow
    private static URL getURLForSoundResource(ResourceLocation resourceLocation) {
        return null;
    }

    @Unique
    private final List<LuaSound> figuraHandlers = Collections.synchronizedList(new ArrayList<>());

    @Unique
    static AudioFormat figura$OggAudioFormat = null;
    private static AudioFormat figura$getAudioFormat(LuaSound sound) {
        try {
            return AudioSystem.getAudioFileFormat(new ByteArrayInputStream(sound.buffer)).getFormat();
        } catch (UnsupportedAudioFileException | IOException e) {
            FiguraMod.LOGGER.warn("Failed to decode sound format for sound: " + sound.getId());
        }
        if (figura$OggAudioFormat == null) {
            Info jorbisInfo = new Info();
            int channels = jorbisInfo.channels;
            int rate = jorbisInfo.rate;

            figura$OggAudioFormat = new AudioFormat( (float) rate, 16, channels, true,
                    false );
        }
        return figura$OggAudioFormat;
    }

    @Inject(at = @At("RETURN"), method = "updateAllSounds")
    private void tick(CallbackInfo ci) {
        Iterator<Map.Entry<String, LuaSound>> playingIterator = this.figura$playingSounds.entrySet().iterator();
        while(playingIterator.hasNext()) {
            Map.Entry<String, LuaSound> entry = playingIterator.next();
            String soundId = entry.getKey();
            LuaSound luaSound = entry.getValue();
            if (!this.sndSystem.playing(soundId)) {
                int stopTime = this.figura$playingSoundsStopTime.get(soundId);
                if (stopTime <= this.playTime) {
                    if (luaSound.sound != null)
                        updateFiguraSound(luaSound, soundId);
                    playingIterator.remove();
                }
            }
        }

        Iterator<Map.Entry<LuaSound, Integer>> delayedSoundIterator = this.figura$delayedSounds.entrySet().iterator();

        while(delayedSoundIterator.hasNext()) {
            Map.Entry<LuaSound, Integer> delayedEntry = delayedSoundIterator.next();
            if (this.playTime >= delayedEntry.getValue()) {
                LuaSound luaSound = delayedEntry.getKey();
                if (luaSound instanceof ITickableSound) {
                    ((ITickableSound)luaSound).update();
                }

                this.playFiguraSound(luaSound);
                delayedSoundIterator.remove();
            }
        }

        Iterator<LuaSound> handlerIterator = figuraHandlers.iterator();
        while (handlerIterator.hasNext()) {
            LuaSound sound = handlerIterator.next();
            String handle = sound.getHandle();
            if (handle == null) {
                handlerIterator.remove();
            } else if (getVolume(SoundCategory.PLAYERS) <= 0f) {
                sndSystem.stop(handle);
                handlerIterator.remove();
            } else if (!sndSystem.playing(handle)) {
                handlerIterator.remove();
            }
        }
    }

    private void updateFiguraSound(LuaSound sound, String soundId) {
        if (!this.loaded)
            return;

        if (sound.isLooping()) {
            this.figura$delayedSounds.put(sound, this.playTime);
        }

        FiguraMod.LOGGER.debug(LOG_MARKER, "Removed channel {} because it's not playing anymore", soundId);
        this.sndSystem.removeSource(soundId);
        this.figura$playingSoundsStopTime.remove(soundId);

        try {
            this.figura$categorySounds.remove(SoundCategory.PLAYERS, soundId);
        } catch (RuntimeException ignored) {
        }
    }

    @Inject(at = @At("RETURN"), method = "stopAllSounds")
    private void stopAll(CallbackInfo ci) {
        figura$stopAllSounds();
    }

    @Inject(at = @At("RETURN"), method = "pauseAllSounds")
    private void pause(CallbackInfo ci) {
        if (this.loaded) {
            for (Map.Entry<String, LuaSound> entry : figura$playingSounds.entrySet()) {
                String string = entry.getKey();
                boolean wasPlaying = this.isFiguraSoundPlaying(entry.getValue());
                if (wasPlaying) {
                    FiguraMod.LOGGER.debug(LOG_MARKER, "Pausing channel {}", string);
                    this.sndSystem.pause(string);
                    this.figura$pausedSounds.add(string);
                }
            }
        }
    }

    @Unique
    @Override
    public boolean isFiguraSoundPlaying(LuaSound sound) {
        if (!this.loaded || sound.handle == null)
            return false;

        String soundId = figura$invPlayingSounds.get(sound);
        if (soundId == null)
            return false;

        return this.sndSystem.playing(soundId) || this.figura$playingSoundsStopTime.containsKey(soundId) && this.figura$playingSoundsStopTime.get(soundId) <= this.playTime;
    }

    @Inject(at = @At("RETURN"), method = "resumeAllSounds")
    private void resume(CallbackInfo ci) {
        if (this.loaded) {
            figura$pausedSounds.forEach(s -> sndSystem.play(s));
            figura$pausedSounds.clear();
        }
    }

    @Inject(at = @At("RETURN"), method = "setVolume")
    private void updateCategoryVolume(SoundCategory category, float volume, CallbackInfo ci) {
        if (!this.loaded || category != SoundCategory.PLAYERS)
            return;

        for (LuaSound sound : figuraHandlers)
            sound.volume(sound.getVolume());
    }

    @Inject(at = @At("RETURN"), method = "stop")
    private void stop(String string, SoundCategory category, CallbackInfo ci) {
        if (category == SoundCategory.PLAYERS)
            figura$stopAllSounds();

        if (category != null) {
            for (String soundId : this.figura$categorySounds.get(category)) {
                LuaSound luaSound = this.figura$playingSounds.get(soundId);
                if (string.isEmpty()) {
                    this.figura$stopLuaSound(luaSound);
                } else if (luaSound.sound.getSoundLocation().equals(new ResourceLocation(string))) {
                    this.figura$stopLuaSound(luaSound);
                }
            }
        } else {
            for (LuaSound luaSound : this.figura$playingSounds.values()) {
                Sound sound = luaSound.sound;
                if (sound.getSoundLocation().equals(new ResourceLocation(string))) {
                    figura$stopLuaSound(luaSound);
                }
            }
        }
    }


    // targeting getGain is more likely to target more versions of Minecraft, but targeting the `listeners` list blocks subtitles. I believe blocking subtitles was more important.
    // If we end up targeting a version of minecraft with no subtitles, use getGain. In vanilla, `listeners` is only used for subtitles and may not be in versions without subtitles.
    //@Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/audio/Listener;getGain()Z"), method = "play", cancellable = true)
    @Inject(at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"), method = "playSound", cancellable = true)
    public void play(ISound sound, CallbackInfo c) {
        // "Can hear sound" check stolen from 381 of SoundEngine
        Vec3d pos = new Vec3d(sound.getXPosF(), sound.getYPosF(), sound.getZPosF());
        if (sound.getSound().isStreaming() || sound.getAttenuationType() == ISound.AttenuationType.NONE){
            // Run sound event
            AvatarManager.executeAll("playSoundEvent", avatar -> {
                boolean cancel = avatar.playSoundEvent(
                    sound.getSoundLocation().toString(),
                    FiguraVec3.fromVec3(pos),
                    sound.getVolume(), sound.getPitch(),
                    sound.canRepeat(),
                    sound.getCategory().name(),
                    sound.getSound().getSoundLocation().toString()
                );
                if (avatar.permissions.get(Permissions.CANCEL_SOUNDS) >= 1) {
                    avatar.noPermissions.remove(Permissions.CANCEL_SOUNDS);
                    if (cancel)
                        c.cancel(); // calling cancel multple times is fine, right?
                }
                else {
                    avatar.noPermissions.add(Permissions.CANCEL_SOUNDS);
                }
            });
        }
    }

    @Override @Intrinsic
    public void figura$addSound(LuaSound sound) {
        figuraHandlers.add(sound);
        for (ISoundEventListener listener : this.listeners) {
            if (listener instanceof GuiSubtitleOverlay) {
                GuiSubtitleOverlay overlay = (GuiSubtitleOverlay) listener;
                ((SubtitleOverlayAccessor) overlay).figura$PlaySound(sound);
            }
        }
    }

    @Override @Intrinsic
    public void figura$stopSound(UUID owner, String name) {
        if (!this.loaded)
            return;

        Iterator<LuaSound> iterator = figuraHandlers.iterator();
        while (iterator.hasNext()) {
            LuaSound sound = iterator.next();
            String handle = sound.getHandle();
            LuaSound luaSound = figura$playingSounds.get(handle);
            SoundAccessor accessor = (SoundAccessor) sound.sound;
            if (handle != null && (owner == null || (accessor.getFigura$owner().equals(owner) && (name == null || accessor.getFigura$name().equals(name))))) {
                sound.stop();
                iterator.remove();
            }
        }
    }

    @Override @Intrinsic
    public void figura$stopAllSounds() {
        if (this.loaded) {
            for (LuaSound sound : figuraHandlers)
                sound.stop();
            figuraHandlers.clear();

            figura$playingSounds.keySet().forEach(s -> this.sndSystem.stop(s));
            figura$playingSoundsStopTime.clear();
        }
    }

    @Override @Intrinsic
    public float figura$getVolume(SoundCategory category) {
        return getVolume(category);
    }

    @Override @Intrinsic
    public Map<LuaSound, String> figura$getSoundBuffersInv() {
        return this.figura$invPlayingSounds;
    }

    @Override @Intrinsic
    public Map<String, LuaSound> figura$getSoundBuffers() {
        return this.figura$playingSounds;
    }

    @Override @Intrinsic
    public boolean figura$isPlaying(UUID owner) {
        if (!this.loaded)
            return false;
        for (LuaSound sound : new ArrayList<>(figuraHandlers)) {
            String name = sound.getHandle();
            SoundAccessor accessor = (SoundAccessor) figura$playingSounds.get(name);
            if (sound.isPlaying() && accessor != null && accessor.getFigura$owner().equals(owner))
                return true;
        }
        return false;
    }

    @Override @Intrinsic
    public SoundManager.SoundSystemStarterThread getSoundSystem() {
        return sndSystem;
    }

    @Override
    public String figura$createHandle(UUID owner, String name, LuaSound luaSound) {
        String soundId = MathHelper.getRandomUUID(ThreadLocalRandom.current()).toString();
        figura$playingSounds.put(soundId, luaSound);

        float posX = (float) luaSound.getPos().x;
        float posY = (float) luaSound.getPos().y;
        float posZ = (float) luaSound.getPos().z;
        if (luaSound.sound != null) {
            ((SoundAccessor) luaSound.sound).setFigura$owner(owner);
            ((SoundAccessor) luaSound.sound).setFigura$name(name);
            Sound sound = luaSound.sound;
            float attenuation = 16.0F;
            float volume = sound.getVolume();
            ResourceLocation soundIdentifier = sound.getSoundAsOggLocation();
            if (volume > 1.0F) {
                attenuation *= volume;
            }

            if (sound.isStreaming()) {
                this.sndSystem
                        .newStreamingSource(
                                false,
                                soundId,
                                getURLForSoundResource(soundIdentifier),
                                soundIdentifier.toString(),
                                luaSound.isLooping(),
                                posX,
                                posY,
                                posZ,
                                ISound.AttenuationType.LINEAR.getTypeInt(),
                                attenuation
                        );
            } else {
                this.sndSystem
                        .newSource(
                                false,
                                soundId,
                                getURLForSoundResource(soundIdentifier),
                                soundIdentifier.toString(),
                                luaSound.isLooping(),
                                posX,
                                posY,
                                posZ,
                                ISound.AttenuationType.LINEAR.getTypeInt(),
                                attenuation
                        );
            }
        } else if (luaSound.buffer != null) {
            this.sndSystem
                    .rawDataStream(
                            figura$getAudioFormat(luaSound), false, soundId, posX, posY,posZ,
                            ISound.AttenuationType.LINEAR.getTypeInt(), 16.0f
                    );
        }
        return soundId;
    }

    @Override
    public void figura$playLuaSound(LuaSound sound) {
        playFiguraSound(sound);
    }

    @Unique
    public void playFiguraSound(LuaSound luaSound) {
        if (this.loaded) {
            if (luaSound.sound != null) {
                ResourceLocation iSoundLocation = luaSound.sound.getSoundLocation();

                if (this.sndSystem.getMasterVolume() <= 0.0F) {
                    LOGGER.debug(LOG_MARKER, "Skipped playing soundEvent: {}, master volume was zero", iSoundLocation);
                } else {
                    Sound sound = luaSound.sound;
                    if (sound == SoundHandler.MISSING_SOUND) {
                        if (UNABLE_TO_PLAY.add(iSoundLocation)) {
                            LOGGER.warn(LOG_MARKER, "Unable to play empty soundEvent: {}", iSoundLocation);
                        }
                    } else {
                        float h = this.getClampedVolume(luaSound);
                        if (h == 0.0F) {
                            LOGGER.debug(LOG_MARKER, "Skipped playing sound {}, volume was zero.", sound.getSoundLocation());
                        } else {
                            String soundId = figura$invPlayingSounds.get(luaSound);
                            LOGGER.debug(LOG_MARKER, "Playing sound {} for event {} as channel {}", sound.getSoundLocation(), iSoundLocation, soundId);
                            this.sndSystem.play(soundId);
                            this.figura$playingSoundsStopTime.put(soundId, this.playTime + 20);
                            this.figura$categorySounds.put(SoundCategory.PLAYERS, soundId);
                        }
                    }
                }
            } else if (luaSound.buffer != null) {
                if (this.sndSystem.getMasterVolume() <= 0.0F) {
                    LOGGER.debug(LOG_MARKER, "Skipped playing soundEvent: {}, master volume was zero", luaSound.getId());
                } else {
                    float h = this.getClampedVolume(luaSound);
                    if (h == 0.0F) {
                        LOGGER.debug(LOG_MARKER, "Skipped playing sound {}, volume was zero.", luaSound.getId());
                    } else {
                        String soundId = figura$invPlayingSounds.get(luaSound);
                        this.sndSystem.play(soundId);
                        this.figura$playingSoundsStopTime.put(soundId, this.playTime + 20);
                        this.figura$playingSounds.put(soundId, luaSound);
                        this.figura$categorySounds.put(SoundCategory.PLAYERS, soundId);
                    }
                }
            }
        }
    }

    @Unique
    private float getClampedVolume(LuaSound soundIn) {
        return MathHelper.clamp(soundIn.getVolume() * this.getVolume(SoundCategory.PLAYERS), 0.0F, 1.0F);
    }

    @Override
    public void figura$pauseLuaSound(LuaSound sound) {
        boolean wasPlaying = this.isFiguraSoundPlaying(sound);
        if (this.loaded && wasPlaying && sound.handle != null) {
            FiguraMod.LOGGER.debug(LOG_MARKER, "Pausing channel {}", sound.handle);
            this.sndSystem.pause(sound.handle);
            this.figura$pausedSounds.add(sound.handle);
        }
    }

    @Override
    public void figura$stopLuaSound(LuaSound sound) {
        if (this.loaded && sound.handle != null) {
            this.sndSystem.stop(sound.handle);
            this.figura$pausedSounds.remove(sound.handle);
            this.figura$playingSounds.remove(sound.handle);
            this.figura$delayedSounds.remove(sound);
        }
    }

    @Override
    public String getSoundStatistics(){
        return String.format("Sounds: %d/%d + %d/%d", ((LibraryAccessor)((SoundManager$SoundSystemStarterThreadAccessor)this.sndSystem).figura$getSoundLibrary()).figura$getNormalChannels().size(), SoundSystemConfig.getNumberNormalChannels(), ((LibraryAccessor)((SoundManager$SoundSystemStarterThreadAccessor)this.sndSystem).figura$getSoundLibrary()).figura$getStreamingChannels().size(), SoundSystemConfig.getNumberStreamingChannels());
    }
}
