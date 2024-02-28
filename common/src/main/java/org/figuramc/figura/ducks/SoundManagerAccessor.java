package org.figuramc.figura.ducks;

import com.google.common.collect.Multimap;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.util.SoundCategory;
import org.figuramc.figura.lua.api.sound.LuaSound;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SoundManagerAccessor {

    void figura$addSound(LuaSound sound);
    void figura$stopSound(UUID owner, String name);
    void figura$stopAllSounds();
    float figura$getVolume(SoundCategory category);
    Map<ISound, String> figura$getSoundBuffersInv();
    Map<String, ISound> figura$getSoundBuffers();
    Multimap<SoundCategory, String> figura$getCategorySoundsMap();
    boolean figura$isPlaying(UUID owner);
    SoundManager.SoundSystemStarterThread getSoundSystem();
    List<String> getPausedSounds();
}
