package org.figuramc.figura.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.lua.api.world.ItemStackAPI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityLivingBase.class)
public abstract class EntityLivingBaseMixin extends Entity {

    public EntityLivingBaseMixin(World world) {
        super(world);
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isHandActive()Z"), method = "updateItemUse", cancellable = true)
    private void triggerItemUseEffects(ItemStack stack, int particleCount, CallbackInfo ci) {
        Avatar avatar = AvatarManager.getAvatar(this);
        if (avatar != null && avatar.useItemEvent(ItemStackAPI.verify(stack), stack.getItemUseAction().name(), particleCount))
            ci.cancel();
    }
}
