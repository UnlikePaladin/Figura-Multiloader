package org.figuramc.figura.mixin.sound;

import com.google.common.collect.Multimap;
import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.Library;
import com.mojang.blaze3d.audio.Listener;

import net.minecraft.client.Options;
import net.minecraft.client.audio.*;
import net.minecraft.client.gui.GuiSubtitleOverlay;
import net.minecraft.client.gui.components.SubtitleOverlay;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.client.sounds.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.phys.Vec3;

import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.ISoundAccessor;
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

import java.util.*;

@Mixin(SoundManager.class)
public abstract class SoundManagerMixin implements SoundManagerAccessor {
    @Shadow private SoundManager.SoundSystemStarterThread sndSystem;
    @Shadow private boolean loaded;
    @Shadow protected abstract float getVolume(@Nullable SoundCategory category);
    @Shadow @Final private List<ISoundEventListener> listeners;
    @Shadow public abstract void addListener(ISoundEventListener listener);

    @Shadow public abstract void stopSound(ISound sound);

    @Shadow @Final private Map<String, ISound> playingSounds;

    @Shadow protected abstract float getClampedVolume(ISound soundIn);

    @Shadow protected abstract float getClampedPitch(ISound soundIn);

    @Shadow @Final private Map<ISound, String> invPlayingSounds;
    @Shadow @Final private Multimap<SoundCategory, String> categorySounds;
    @Unique
    private final List<LuaSound> figuraHandlers = Collections.synchronizedList(new ArrayList<>());

    @Inject(at = @At("RETURN"), method = "updateAllSounds")
    private void tick(CallbackInfo ci) {

        for(ITickableSound iTickableSound : this.figura$tickableSounds) {
            iTickableSound.update();
            if (iTickableSound.isDonePlaying()) {
                this.stopSound(iTickableSound);
            } else {
                String string = figura$playingSoundsInv.get(iTickableSound);
                this.sndSystem.setVolume(string, this.getClampedVolume(iTickableSound));
                this.sndSystem.setPitch(string, this.getClampedPitch(iTickableSound));
                this.sndSystem.setPosition(string, iTickableSound.getXPosF(), iTickableSound.getYPosF(), iTickableSound.getZPosF());
            }
        }
    }

    @Inject(at = @At("RETURN"), method = "tickNonPaused")
    private void tickNonPaused(CallbackInfo ci) {
        Iterator<LuaSound> iterator = figuraHandlers.iterator();
        while (iterator.hasNext()) {
            LuaSound sound = iterator.next();
            ChannelAccess.ChannelHandle handle = sound.getHandle();
            if (handle == null) {
                iterator.remove();
            } else if (getVolume(SoundCategory.PLAYERS) <= 0f) {
                handle.execute(Channel::stop);
                iterator.remove();
            } else if (handle.isStopped()) {
                iterator.remove();
            }
        }
    }

    @Inject(at = @At("RETURN"), method = "stopAll")
    private void stopAll(CallbackInfo ci) {
        figura$stopAllSounds();
    }

    @Inject(at = @At("RETURN"), method = "pause")
    private void pause(CallbackInfo ci) {
        if (this.loaded) figuraChannel.executeOnChannels(stream -> stream.forEach(Channel::pause));
    }

    @Inject(at = @At("RETURN"), method = "resume")
    private void resume(CallbackInfo ci) {
        if (this.loaded) figuraChannel.executeOnChannels(stream -> stream.forEach(Channel::unpause));
    }

    @Inject(at = @At("RETURN"), method = "updateCategoryVolume")
    private void updateCategoryVolume(SoundSource category, float volume, CallbackInfo ci) {
        if (!this.loaded || category != SoundSource.PLAYERS)
            return;

        for (LuaSound sound : figuraHandlers)
            sound.volume(sound.getVolume());
    }

    @Inject(at = @At("RETURN"), method = "stop(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/sounds/SoundSource;)V")
    private void stop(ResourceLocation id, SoundSource category, CallbackInfo ci) {
        if (category == SoundSource.PLAYERS)
            figura$stopAllSounds();
    }

    // targeting getGain is more likely to target more versions of Minecraft, but targeting the `listeners` list blocks subtitles. I believe blocking subtitles was more important.
    // If we end up targeting a version of minecraft with no subtitles, use getGain. In vanilla, `listeners` is only used for subtitles and may not be in versions without subtitles.
    //@Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/audio/Listener;getGain()Z"), method = "play", cancellable = true)
    @Inject(at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"), method = "playSound", cancellable = true)
    public void play(ISound sound, CallbackInfo c) {
        // "Can hear sound" check stolen from 381 of SoundEngine
        float g = Math.max(sound.getVolume(), 1.0F) * (float)sound.getSound().getAttenuationDistance();
        Vec3 pos = new Vec3(sound.getX(), sound.getY(), sound.getZ());
        if (sound.isRelative() || sound.getAttenuation() == Attenuation.NONE || this.listener.getListenerPosition().distanceToSqr(pos) < (double)(g * g)){
            // Run sound event
            AvatarManager.executeAll("playSoundEvent", avatar -> {
                boolean cancel = avatar.playSoundEvent(
                    sound.getLocation().toString(),
                    FiguraVec3.fromVec3(pos),
                    sound.getVolume(), sound.getPitch(),
                    sound.isLooping(),
                    sound.getSource().name(),
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
            ISoundAccessor accessor = (ISoundAccessor) sound.getHandle();
            if (accessor != null && (owner == null || (accessor.getFigura$owner().equals(owner) && (name == null || accessor.getFigura$name().equals(name))))) {
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
            figuraChannel.clear();
        }
    }

    @Override @Intrinsic
    public Channel figura$createHandle(UUID owner, String name, Library.Pool pool) {
        return figuraChannel.createHandle(pool).thenApply(channelHandle -> {
            if (channelHandle != null) {
                ((ISoundAccessor) channelHandle).setFigura$owner(owner);
                ((ISoundAccessor) channelHandle).setFigura$name(name);
            }
            return channelHandle;
        }).join();
    }

    @Override @Intrinsic
    public float figura$getVolume(SoundCategory category) {
        return getVolume(category);
    }

    @Override @Intrinsic
    public Map<ISound, String> figura$getSoundBuffersInv() {
        return this.invPlayingSounds;
    }

    @Override @Intrinsic
    public Map<String, ISound> figura$getSoundBuffers() {
        return this.playingSounds;
    }

    @Override @Intrinsic
    public boolean figura$isPlaying(UUID owner) {
        if (!this.loaded)
            return false;
        for (LuaSound sound : new ArrayList<>(figuraHandlers)) {
            ISoundAccessor accessor = (ISoundAccessor) sound.getHandle();
            if (sound.isPlaying() && accessor != null && accessor.getFigura$owner().equals(owner))
                return true;
        }
        return false;
    }

    @Override @Intrinsic
    public SoundManager.SoundSystemStarterThread getSoundSystem() {
        return sndSystem;
    }

    @Override @Intrinsic
    public Multimap<SoundCategory, String> figura$getCategorySoundsMap() {
        return categorySounds;
    }
}
