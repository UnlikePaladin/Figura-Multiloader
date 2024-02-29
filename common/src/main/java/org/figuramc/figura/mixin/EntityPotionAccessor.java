package org.figuramc.figura.mixin;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityPotion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EntityPotion.class)
public interface EntityPotionAccessor {
    @Invoker("isWaterSensitiveEntity")
    static boolean invokeIsWaterSensitiveEntity(EntityLivingBase entityLivingBase) {
        throw new AssertionError();
    }
}
