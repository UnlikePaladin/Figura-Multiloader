package org.figuramc.figura.mixin.render.renderers;

import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;
import org.figuramc.figura.ducks.SkullBlockRendererAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityItemStackRenderer.class)
public class TileEntityItemStackRendererMixin {

    @Inject(method = "renderByItem(Lnet/minecraft/item/ItemStack;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/tileentity/TileEntitySkullRenderer;renderSkull(FFFLnet/minecraft/util/EnumFacing;FILcom/mojang/authlib/GameProfile;IF)V"), require = 0)
    void setTargetItem(ItemStack stack, float tickDelta, CallbackInfo ci) {
        SkullBlockRendererAccessor.setItem(stack);
    }

    // Optifine moves the place where Figura injects into a different method
    /*
    @Inject(method = "renderRaw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/SkullBlockRenderer;renderSkull(Lnet/minecraft/core/Direction;FLnet/minecraft/world/level/block/SkullBlock$Type;Lcom/mojang/authlib/GameProfile;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"), remap = false, require = 0)
    void setTargetItem(ItemStack stack, PoseStack matrixStackIn, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn, CallbackInfo ci) {
        SkullBlockRendererAccessor.setItem(stack);
    }*/
    // TODO : Rework Optifine compatibility womp womp
}
