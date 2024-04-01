package org.figuramc.figura.mixin.render.renderers;

import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemTransformVec3f;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.lua.api.world.ItemStackAPI;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.model.rendering.texture.RenderTypes;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderItem.class)
public abstract class RenderItemMixin {

    @Shadow public abstract IBakedModel getItemModelWithOverrides(ItemStack stack, @Nullable World world, @Nullable EntityLivingBase entity);

    @Inject(at = @At("HEAD"), method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;Z)V", cancellable = true)
    private void renderStatic(ItemStack item, EntityLivingBase entity, ItemCameraTransforms.TransformType transformType, boolean leftHanded, CallbackInfo ci) {
        if (entity == null || item.isEmpty())
            return;

        Avatar avatar = AvatarManager.getAvatar(entity);
        if (avatar == null)
            return;

        IBakedModel bakedModel = this.getItemModelWithOverrides(item, entity.world, entity);
        ItemTransformVec3f transform = bakedModel.getItemCameraTransforms().getTransform(transformType);
        int light = entity.world.getCombinedLight(entity.getPosition(), 0);

        if (avatar.itemRenderEvent(ItemStackAPI.verify(item), transformType.name(), FiguraVec3.fromVec3f(transform.translation), FiguraVec3.of(transform.rotation.z, transform.rotation.y, transform.rotation.x), FiguraVec3.fromVec3f(transform.scale), leftHanded, RenderTypes.FiguraBufferSource.INSTANCE, light, 10 << 16))
            ci.cancel();
    }
}
