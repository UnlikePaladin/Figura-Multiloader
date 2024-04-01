package org.figuramc.figura.mixin.render.layers;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerEntityOnShoulder;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.extensions.Vector3fExtension;
import org.figuramc.figura.model.ParentType;
import org.figuramc.figura.utils.RenderUtils;
import org.figuramc.figura.utils.ui.UIHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(LayerEntityOnShoulder.class)
public abstract class LayerEntityOnShoulderMixin {

//TODO: convert my access widener for fabric to a access transformer on forge
    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelBase;render(Lnet/minecraft/entity/Entity;FFFFFF)V"), method = "renderEntityOnShoulder", cancellable = true)
    private void render(EntityPlayer player, @Nullable UUID shoulderEntityUuid, NBTTagCompound shoulderEntityNbt, RenderLivingBase<? extends EntityLivingBase> shoulderEntityRenderer, ModelBase shoulderEntityModel, ResourceLocation shoulderEntityTexture, Class<?> shoulderEntityClass, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch, float scale, boolean leftShoulder, CallbackInfoReturnable<LayerEntityOnShoulder.DataHolder> ci) {
        Avatar avatar = AvatarManager.getAvatar(player);
        if (!RenderUtils.vanillaModel(avatar))
            return;

        // script hide
        if (avatar.luaRuntime != null &&
                (leftShoulder && !avatar.luaRuntime.vanilla_model.LEFT_PARROT.checkVisible() ||
                !leftShoulder && !avatar.luaRuntime.vanilla_model.RIGHT_PARROT.checkVisible())
        ) {
            GlStateManager.popMatrix();
            ci.setReturnValue(((LayerEntityOnShoulder)(Object)this).new DataHolder(shoulderEntityUuid, shoulderEntityRenderer, shoulderEntityModel, shoulderEntityTexture, shoulderEntityClass));
            ci.cancel();
            return;
        }

        // pivot part
        if (avatar.pivotPartRender(leftShoulder ? ParentType.LeftParrotPivot : ParentType.RightParrotPivot, stack -> {
            GlStateManager.translate(0d, 24d, 0d);
            float s = 16f;
            GlStateManager.scale(s, s, s);
            GlStateManager.rotate(((Vector3fExtension) UIHelper.XP).figura$rotationDegrees(180f)); // TODO AAAAAAAAAAA, quaternion scary
            GlStateManager.rotate(((Vector3fExtension)UIHelper.YP).figura$rotationDegrees(180f));
            shoulderEntityModel.render(player, limbAngle, limbDistance, animationProgress, headYaw, headPitch, scale);
        })) {
            GlStateManager.popMatrix();
            ci.setReturnValue(((LayerEntityOnShoulder)(Object)this).new DataHolder(shoulderEntityUuid, shoulderEntityRenderer, shoulderEntityModel, shoulderEntityTexture, shoulderEntityClass));
            ci.cancel();
        }
    }
}
