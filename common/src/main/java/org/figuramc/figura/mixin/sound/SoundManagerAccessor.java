package org.figuramc.figura.mixin.sound;

import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SoundHandler.class)
public interface SoundManagerAccessor {
    @Intrinsic
    @Accessor("sndManager")
    SoundManager getSoundEngine();
}
