package org.figuramc.figura.lua.api.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.Sound;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.SoundManagerAccessor;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaMethodOverload;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.mixin.sound.SoundHandlerAccessor;
import org.figuramc.figura.permissions.Permissions;
import org.figuramc.figura.utils.LuaUtils;
import org.figuramc.figura.utils.TextUtils;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaError;

@LuaWhitelist
@LuaTypeDoc(
        name = "Sound",
        value = "sound"
)
public class LuaSound {

    private final Avatar owner;
    private final String id;
    public final byte[] buffer;
    public final Sound sound;

    private boolean playing = false;

    private FiguraVec3 pos = FiguraVec3.of();
    private float pitch = 1f;
    private float volume = 1f;
    private float attenuation = 1f;
    private boolean loop = false;
    private ITextComponent subtitleText;
    private String subtitle;
    private final SoundHandler soundHandler;

    @Nullable
    public String handle;

    public LuaSound(byte[] buffer, String id, Avatar owner) {
        this(null, buffer, id, new TextComponentString(id), owner);
    }

    public LuaSound(Sound sound, String id, ITextComponent subtitle, Avatar owner) {
        this(sound, null, id, subtitle, owner);
    }

    public LuaSound(Sound sound, byte[] buffer, String id, ITextComponent subtitle, Avatar owner) {
        this.owner = owner;
        this.id = id;
        this.buffer = buffer;
        this.sound = sound;
        this.subtitleText = subtitle;
        this.subtitle = subtitle == null ? null : subtitle.getFormattedText();
        this.soundHandler = Minecraft.getMinecraft().getSoundHandler();
    }

    public String getHandle() {
        return handle;
    }

    public ITextComponent getSubtitleText() {
        return subtitleText;
    }

    public String getId() {
        return id;
    }

    private float calculateVolume() {
        return SoundAPI.getSoundEngine().figura$getVolume(SoundCategory.PLAYERS) * (owner.permissions.get(Permissions.VOLUME) / 100f);
    }

    @LuaWhitelist
    @LuaMethodDoc("sound.play")
    public LuaSound play() {
        if (this.playing)
            return this;

        if (!owner.soundsRemaining.use()) {
            owner.noPermissions.add(Permissions.SOUNDS);
            return this;
        }

        owner.noPermissions.remove(Permissions.SOUNDS);
        String name = ((SoundManagerAccessor)((SoundHandlerAccessor)soundHandler).getSoundEngine()).figura$getSoundBuffersInv().get(sound);
        // if handle exists, the sound was previously played. Unpause it
        if (sound != null && name != null) {
            ((SoundManagerAccessor) ((SoundHandlerAccessor)soundHandler).getSoundEngine()).figura$playLuaSound(this);
            this.playing = true;
            return this;
        }

        // if there is no sound data, exit early
        if (sound == null)
            return this;

        float vol = calculateVolume();
        // if nobody can hear you scream, are you really screaming? No.
        if (vol <= 0)
            return this;

        // Technically I am setting playing to true earlier than I am supposed to,
        // but the function cannot exit early past here (other than crashing) so its fine
        this.playing = true;
        AvatarManager.executeAll("playSoundEvent", avatar -> {
            boolean cancel = avatar.playSoundEvent(
                this.getId(),
                this.getPos(),
                this.getVolume(), this.getPitch(),
                this.isLooping(),
                SoundCategory.PLAYERS.name(),
                null
            );
            if (avatar.permissions.get(Permissions.CANCEL_SOUNDS) >= 1) {
                avatar.noPermissions.remove(Permissions.CANCEL_SOUNDS);
                if (cancel)
                    this.playing = false;
            }
            else {
                avatar.noPermissions.add(Permissions.CANCEL_SOUNDS);
            }
        });
        // If the sound was cancelled, exit.
        if (!this.playing) {
            return this;
        }

        if (buffer != null)
            this.handle = SoundAPI.getSoundEngine().figura$createHandle(owner.owner, id, this);
        else
            this.handle = SoundAPI.getSoundEngine().figura$createHandle(owner.owner, id, this);

        // I dunno why this check was here in the first place and I aint about to ruin things by removing it.
        if (handle == null) {
            this.playing = false; // explicit set just incase
            return this;
        }

        SoundAPI.getSoundEngine().figura$addSound(this);

        if (buffer != null) {
            ((SoundManagerAccessor)((SoundHandlerAccessor)soundHandler).getSoundEngine()).getSoundSystem().setPitch(handle, pitch);
            ((SoundManagerAccessor)((SoundHandlerAccessor)soundHandler).getSoundEngine()).getSoundSystem().setVolume(handle, volume * vol);
            ((SoundManagerAccessor)((SoundHandlerAccessor)soundHandler).getSoundEngine()).getSoundSystem().setAttenuation(handle, ISound.AttenuationType.LINEAR.getTypeInt());
            ((SoundManagerAccessor)((SoundHandlerAccessor)soundHandler).getSoundEngine()).getSoundSystem().setLooping(handle, loop);
            ((SoundManagerAccessor)((SoundHandlerAccessor)soundHandler).getSoundEngine()).getSoundSystem().setPosition(handle, (float) pos.x, (float) pos.y, (float) pos.z);
            ((SoundManagerAccessor)((SoundHandlerAccessor)soundHandler).getSoundEngine()).getSoundSystem().setDistOrRoll(handle, attenuation * 16.0f);
            ((SoundManagerAccessor)((SoundHandlerAccessor)soundHandler).getSoundEngine()).getSoundSystem().feedRawAudioData(handle, buffer);
            ((SoundManagerAccessor) ((SoundHandlerAccessor)soundHandler).getSoundEngine()).figura$playLuaSound(this);
        } else {
            ((SoundManagerAccessor)((SoundHandlerAccessor)soundHandler).getSoundEngine()).getSoundSystem().setPitch(handle, pitch);
            ((SoundManagerAccessor)((SoundHandlerAccessor)soundHandler).getSoundEngine()).getSoundSystem().setVolume(handle, volume * vol);
            ((SoundManagerAccessor)((SoundHandlerAccessor)soundHandler).getSoundEngine()).getSoundSystem().setAttenuation(handle, ISound.AttenuationType.LINEAR.getTypeInt());
            ((SoundManagerAccessor)((SoundHandlerAccessor)soundHandler).getSoundEngine()).getSoundSystem().setLooping(handle, loop && !sound.isStreaming());
            ((SoundManagerAccessor)((SoundHandlerAccessor)soundHandler).getSoundEngine()).getSoundSystem().setPosition(handle, (float) pos.x, (float) pos.y, (float) pos.z);
            ((SoundManagerAccessor)((SoundHandlerAccessor)soundHandler).getSoundEngine()).getSoundSystem().setDistOrRoll(handle, attenuation * 16.0f);
            ((SoundManagerAccessor) ((SoundHandlerAccessor)soundHandler).getSoundEngine()).figura$playLuaSound(this);
        }

        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc("sound.is_playing")
    public boolean isPlaying() {
        if (sound != null) {
            this.playing = ((SoundManagerAccessor)((SoundHandlerAccessor)soundHandler).getSoundEngine()).isFiguraSoundPlaying(this);
        }
        return this.playing;
    }

    @LuaWhitelist
    @LuaMethodDoc("sound.pause")
    public LuaSound pause() {
        this.playing = false;
        if (sound != null) {
            ((SoundManagerAccessor) ((SoundHandlerAccessor)soundHandler).getSoundEngine()).figura$pauseLuaSound(this);
        }
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc("sound.stop")
    public LuaSound stop() {
        this.playing = false;
        if (handle != null){
            ((SoundManagerAccessor) ((SoundHandlerAccessor)soundHandler).getSoundEngine()).figura$stopLuaSound(this);
        }
        handle = null;
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc("sound.get_pos")
    public FiguraVec3 getPos() {
        return pos;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "pos"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            aliases = "pos",
            value = "sound.set_pos")
    public LuaSound setPos(Object x, Double y, Double z) {
        this.pos = LuaUtils.parseVec3("setPos", x, y, z);
        if (handle != null) {
            ((SoundManagerAccessor)((SoundHandlerAccessor)soundHandler).getSoundEngine()).getSoundSystem().setPosition(handle, (float) pos.x, (float) pos.y, (float) pos.z);
        }
        return this;
    }

    @LuaWhitelist
    public LuaSound pos(Object x, Double y, Double z) {
        return setPos(x, y, z);
    }

    @LuaWhitelist
    @LuaMethodDoc("sound.get_volume")
    public float getVolume() {
        return volume;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = Float.class,
                    argumentNames = "volume"
            ),
            aliases = "volume",
            value = "sound.set_volume")
    public LuaSound setVolume(float volume) {
        this.volume = Math.min(volume, 1);
        if (handle != null) {
            float f = calculateVolume();
            if (f <= 0) {
                ((SoundManagerAccessor)((SoundHandlerAccessor)soundHandler).getSoundEngine()).getSoundSystem().stop(handle);
            } else {
                ((SoundManagerAccessor)((SoundHandlerAccessor)soundHandler).getSoundEngine()).getSoundSystem().setVolume(handle, this.volume * f);
            }
        }
        return this;
    }

    @LuaWhitelist
    public LuaSound volume(float volume) {
        return setVolume(volume);
    }

    @LuaWhitelist
    @LuaMethodDoc("sound.get_attenuation")
    public float getAttenuation() {
        return attenuation;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = Float.class,
                    argumentNames = "attenuation"
            ),
            aliases = "attenuation",
            value = "sound.set_attenuation")
    public LuaSound setAttenuation(float attenuation) {
        this.attenuation = Math.max(attenuation, 1);
        if (handle != null)
            ((SoundManagerAccessor)((SoundHandlerAccessor)soundHandler).getSoundEngine()).getSoundSystem().setDistOrRoll(handle, this.attenuation * 16.0f);
        return this;
    }

    @LuaWhitelist
    public LuaSound attenuation(float attenuation) {
        return setAttenuation(attenuation);
    }

    @LuaWhitelist
    @LuaMethodDoc("sound.get_pitch")
    public float getPitch() {
        return pitch;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = Float.class,
                    argumentNames = "pitch"
            ),
            aliases = "pitch",
            value = "sound.set_pitch")
    public LuaSound setPitch(float pitch) {
        this.pitch = Math.max(pitch, 0);
        if (handle != null)
            ((SoundManagerAccessor)((SoundHandlerAccessor)soundHandler).getSoundEngine()).getSoundSystem().setPitch(handle, this.pitch);
        return this;
    }

    @LuaWhitelist
    public LuaSound pitch(float pitch) {
        return setPitch(pitch);
    }

    @LuaWhitelist
    @LuaMethodDoc("sound.is_looping")
    public boolean isLooping() {
        return loop;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = Boolean.class,
                    argumentNames = "loop"
            ),
            aliases = "loop",
            value = "sound.set_loop")
    public LuaSound setLoop(boolean loop) {
        this.loop = loop;
        if (handle != null)
            ((SoundManagerAccessor)((SoundHandlerAccessor)soundHandler).getSoundEngine()).getSoundSystem().setLooping(handle, this.loop);
        return this;
    }

    @LuaWhitelist
    public LuaSound loop(boolean loop) {
        return setLoop(loop);
    }

    @LuaWhitelist
    @LuaMethodDoc("sound.get_subtitle")
    public String getSubtitle() {
        return subtitle;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "subtitle"
            ),
            aliases = "subtitle",
            value = "sound.set_subtitle")
    public LuaSound setSubtitle(String subtitle) {
        this.subtitle = subtitle;
        if (subtitle == null) {
            this.subtitleText = null;
        } else {
            this.subtitleText = TextUtils.tryParseJson(subtitle);
            if (this.subtitleText.getUnformattedText().length() > 48)
                throw new LuaError("Text length exceeded limit of 48 characters");
        }
        return this;
    }

    @LuaWhitelist
    public LuaSound subtitle(String subtitle) {
        return setSubtitle(subtitle);
    }

    @Override
    public String toString() {
        return id + " (Sound)";
    }
}
