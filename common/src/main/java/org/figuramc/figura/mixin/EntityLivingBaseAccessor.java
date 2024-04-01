package org.figuramc.figura.mixin;

import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityLivingBase.class)
public interface EntityLivingBaseAccessor {
    @Intrinsic
    @Accessor("isJumping")
    boolean isJumping();

    @Intrinsic
    @Accessor("swingProgressInt")
    int getSwingDuration();
}
