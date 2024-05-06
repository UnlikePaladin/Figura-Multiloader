package org.figuramc.figura.mixin.render.layers.items;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.model.ParentType;
import org.figuramc.figura.utils.RenderUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LayerHeldItem.class)
public abstract class ItemInHandLayerMixin implements LayerRenderer<EntityLivingBase> {

    @Inject(method = "renderHeldItem", at = @At("HEAD"), cancellable = true)
    protected void renderArmWithItem(EntityLivingBase entity, ItemStack itemStack, ItemCameraTransforms.TransformType transformationMode, EnumHandSide arm, CallbackInfo ci) {
        if (itemStack.isEmpty())
            return;

        boolean left = arm == EnumHandSide.LEFT;

        Avatar avatar = AvatarManager.getAvatar(entity);
        if (!RenderUtils.renderArmItem(avatar, left, ci))
            return;

        // pivot part
        if (avatar.pivotPartRender(left ? ParentType.LeftItemPivot : ParentType.RightItemPivot, stack -> {
            final float s = 16f;
            GlStateManager.scale(s, s, s);
            GlStateManager.rotate(-90.0f, 1.0f, 0.0f, 0.0f); // Vector3f.XP.rotationDegrees(-90f));
            Minecraft.getMinecraft().getItemRenderer().renderItemSide(entity, itemStack, transformationMode, left);
        })) {
            ci.cancel();
        }
    }
}
