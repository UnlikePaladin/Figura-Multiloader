package org.figuramc.figura.ducks;

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
    Map<LuaSound, String> figura$getSoundBuffersInv();
    Map<String, LuaSound> figura$getSoundBuffers();
    boolean figura$isPlaying(UUID owner);
    SoundManager.SoundSystemStarterThread getSoundSystem();
    List<String> figura$getPausedBuffers();
    void figura$playLuaSound(LuaSound sound);
    void figura$pauseLuaSound(LuaSound sound);
    void figura$stopLuaSound(LuaSound sound);

    String figura$createHandle(UUID owner, String name, LuaSound sound);
    boolean isFiguraSoundPlaying(LuaSound sound);

    String getSoundStatistics();
}
