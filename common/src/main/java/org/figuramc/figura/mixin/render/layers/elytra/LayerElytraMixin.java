package org.figuramc.figura.mixin.render.layers.elytra;

import net.minecraft.client.model.ModelElytra;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.layers.LayerElytra;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.extensions.Vector3fExtension;
import org.figuramc.figura.lua.api.vanilla_model.VanillaPart;
import org.figuramc.figura.model.ParentType;
import org.figuramc.figura.model.rendering.texture.RenderTypes;
import org.figuramc.figura.permissions.Permissions;
import org.figuramc.figura.utils.PlatformUtils;
import org.figuramc.figura.utils.ui.UIHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LayerElytra.class)
public abstract class LayerElytraMixin {
    @Shadow @Final private ModelElytra modelElytra;
    @Shadow @Final private static ResourceLocation TEXTURE_ELYTRA;
    @Unique
    private VanillaPart vanillaPart;
    @Unique
    private Avatar figura$avatar;

    @Unique
    private boolean renderedPivot;

    @Inject(at = @At(value = "HEAD"), method = "doRenderLayer")
    public void setAvatar(EntityLivingBase livingEntity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch, float scale, CallbackInfo ci) {
        figura$avatar = AvatarManager.getAvatar(livingEntity);
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelElytra;setRotationAngles(FFFFFFLnet/minecraft/entity/Entity;)V", shift = At.Shift.AFTER), method = "doRenderLayer")
    public void onRender(EntityLivingBase livingEntity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch, float scale, CallbackInfo ci) {
        vanillaPart = null;
        if (figura$avatar == null)
            return;

        if (figura$avatar.luaRuntime != null) {
            VanillaPart part = figura$avatar.luaRuntime.vanilla_model.ELYTRA;
            part.save(modelElytra);
            if (figura$avatar.permissions.get(Permissions.VANILLA_MODEL_EDIT) == 1) {
                vanillaPart = part;
                vanillaPart.preTransform(modelElytra);
            }
        }

        figura$avatar.elytraRender(livingEntity, RenderTypes.FiguraBufferSource.INSTANCE, livingEntity.getBrightnessForRender(), tickDelta, modelElytra);

        if (vanillaPart != null)
            vanillaPart.posTransform(modelElytra);

    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelElytra;render(Lnet/minecraft/entity/Entity;FFFFFF)V"), method = "doRenderLayer", cancellable = true)
    public void cancelVanillaPart(EntityLivingBase livingEntity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch, float scale, CallbackInfo ci) {
        if (vanillaPart != null)
            vanillaPart.restore(modelElytra);


        ItemStack itemStack = livingEntity.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
        if (itemStack.getItem() != Items.ELYTRA && !PlatformUtils.isModLoaded("origins")) {
            return;
        }
        if (figura$avatar != null && figura$avatar.luaRuntime != null && figura$avatar.permissions.get(Permissions.VANILLA_MODEL_EDIT) == 1 && figura$avatar.luaRuntime.vanilla_model.ELYTRA.checkVisible()) {
            // Try to render the pivot part
            renderedPivot = figura$avatar.pivotPartRender(ParentType.ElytraPivot, stack -> {
                GlStateManager.pushMatrix();
                GlStateManager.scale(16, 16, 16);

                GlStateManager.rotate(180f, 1f, 1f, 0);
                //GlStateManager.rotate(((Vector3fExtension)UIHelper.XP).figura$rotationDegrees(180f)); // TODO AAAAAAAAAAA, quaternion scary
                //GlStateManager.rotate(((Vector3fExtension)UIHelper.YP).figura$rotationDegrees(180f));
                GlStateManager.translate(0.0f, 0.0f, 0.125f);
                this.modelElytra.setRotationAngles(limbAngle, limbDistance, animationProgress, headYaw, headPitch, scale, livingEntity);
                this.modelElytra.render(livingEntity, limbAngle, limbDistance, animationProgress, headYaw, headPitch, scale);
                GlStateManager.popMatrix();
            });
        } else if (figura$avatar != null && figura$avatar.luaRuntime != null && figura$avatar.permissions.get(Permissions.VANILLA_MODEL_EDIT) == 1 && !figura$avatar.luaRuntime.vanilla_model.ELYTRA.checkVisible()){
            renderedPivot = true;
            GlStateManager.popMatrix();
            GlStateManager.disableBlend();
            ci.cancel();
            return;
        } else {
            renderedPivot = false;
        }

        if (renderedPivot) {
            GlStateManager.popMatrix();
            GlStateManager.disableBlend();
            ci.cancel();
        }
    }
}
