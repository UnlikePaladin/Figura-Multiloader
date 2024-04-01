package org.figuramc.figura.mixin.sound;

import net.minecraft.client.audio.SoundManager;
import org.figuramc.figura.ducks.SoundManager$SoundSystemStarterThreadAccessor;
import org.spongepowered.asm.mixin.Mixin;
import paulscode.sound.Library;
import paulscode.sound.SoundSystem;

@Mixin(SoundManager.SoundSystemStarterThread.class)
public abstract class SoundManager$SoundSystemStarterThreadMixin extends SoundSystem implements SoundManager$SoundSystemStarterThreadAccessor {
    @Override
    public Library figura$getSoundLibrary() {
        return soundLibrary;
    }
}
