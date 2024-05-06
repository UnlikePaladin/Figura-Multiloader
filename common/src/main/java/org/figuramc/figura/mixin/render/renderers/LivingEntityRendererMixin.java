package org.figuramc.figura.mixin.render.renderers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.gui.PopupMenu;
import org.figuramc.figura.lua.api.vanilla_model.VanillaPart;
import org.figuramc.figura.math.matrix.FiguraMat4;
import org.figuramc.figura.model.rendering.PartFilterScheme;
import org.figuramc.figura.model.rendering.texture.RenderTypes;
import org.figuramc.figura.permissions.Permissions;
import org.figuramc.figura.utils.RenderUtils;
import org.figuramc.figura.utils.ui.UIHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.FloatBuffer;

@Mixin(RenderLivingBase.class)
public abstract class LivingEntityRendererMixin<T extends EntityLivingBase> extends Render<T> {

    protected LivingEntityRendererMixin(RenderManager renderManager) {
        super(renderManager);
    }



    @Shadow public abstract ModelBase getMainModel();

    @Shadow protected abstract boolean isVisible(T entityLivingBase);

    @Shadow protected abstract int getColorMultiplier(T entityLivingBase, float f, float partialTickTime);

    @Unique
    private Avatar currentAvatar;

    @Unique
    private Matrix4f lastPose;

    @Unique
    private final FloatBuffer modelBuff = BufferUtils.createFloatBuffer(16);


    @Inject(at = @At("HEAD"), method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V")
    private void onRender(T entityLivingBase, double d, double e, double f, float g, float partialTicks, CallbackInfo ci) {
        currentAvatar = AvatarManager.getAvatar(entityLivingBase);
        if (currentAvatar == null)
            return;

        GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, modelBuff);
        lastPose = new Matrix4f();
        lastPose.load(modelBuff);
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelBase;setRotationAngles(FFFFFFLnet/minecraft/entity/Entity;)V", shift = At.Shift.AFTER), method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", cancellable = true)
    private void preRender(T entity, double d, double e, double f, float yaw, float delta, CallbackInfo ci) {
        if (currentAvatar == null)
            return;

        if (Avatar.firstPerson) {
            // TODO : check how broken this is
            currentAvatar.updateMatrices((RenderLivingBase<?>) (Object) this);
            currentAvatar = null;
            GlStateManager.enableCull();
            GlStateManager.popMatrix();
            ci.cancel();
            return;
        }

        if (currentAvatar.luaRuntime != null) {
            VanillaPart part = currentAvatar.luaRuntime.vanilla_model.PLAYER;
            ModelBase model = getMainModel();
            part.save(model);
            if (currentAvatar.permissions.get(Permissions.VANILLA_MODEL_EDIT) == 1)
                part.preTransform(model);
        }

        boolean showBody = this.isVisible(entity);
        boolean translucent = !showBody && Minecraft.getMinecraft().player != null && !entity.isInvisibleToPlayer(Minecraft.getMinecraft().player);
        boolean glowing = !showBody && this.renderOutlines;
        boolean invisible = !translucent && !showBody && !glowing;

        // When viewed 3rd person, render all non-world parts.
        PartFilterScheme filter = invisible ? PartFilterScheme.PIVOTS : PartFilterScheme.MODEL;
        int overlay = getColorMultiplier(entity, entity.getBrightness(), delta);

        FiguraMod.pushProfiler(FiguraMod.MOD_ID);
        FiguraMod.pushProfiler(currentAvatar);

        FiguraMod.pushProfiler("calculateMatrix");
        Matrix4f diff = new Matrix4f();
        diff.load(lastPose);
        diff.invert();
        GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, modelBuff);
        Matrix4f temp = new Matrix4f();
        temp.load(modelBuff);
        Matrix4f.mul(diff, temp, diff);
        FiguraMat4 poseMatrix = new FiguraMat4().set(diff);

        FiguraMod.popPushProfiler("renderEvent");
        currentAvatar.renderEvent(delta, poseMatrix);

        FiguraMod.popPushProfiler("render");
        currentAvatar.render(entity, yaw, delta, translucent ? 0.15f : 1f, RenderTypes.FiguraBufferSource.INSTANCE, entity.getBrightnessForRender(), overlay, (RenderLivingBase<?>) (Object) this, filter, translucent, glowing);

        FiguraMod.popPushProfiler("postRenderEvent");
        currentAvatar.postRenderEvent(delta, poseMatrix);

        FiguraMod.popProfiler(3);

        if (currentAvatar.luaRuntime != null && currentAvatar.permissions.get(Permissions.VANILLA_MODEL_EDIT) == 1)
            currentAvatar.luaRuntime.vanilla_model.PLAYER.posTransform(getMainModel());
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;popMatrix()V"), method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V")
    private void endRender(T entity, double x, double y, double z, float yaw, float partialTicks, CallbackInfo ci) {
        if (currentAvatar == null)
            return;

        // Render avatar with params
        if (currentAvatar.luaRuntime != null && currentAvatar.permissions.get(Permissions.VANILLA_MODEL_EDIT) == 1)
            currentAvatar.luaRuntime.vanilla_model.PLAYER.restore(getMainModel());

        currentAvatar = null;
        lastPose = null;
    }

    @Inject(method = "canRenderName(Lnet/minecraft/entity/EntityLivingBase;)Z", at = @At("HEAD"), cancellable = true)
    private void shouldShowName(T livingEntity, CallbackInfoReturnable<Boolean> cir) {
        if (UIHelper.paperdoll)
            cir.setReturnValue(Configs.PREVIEW_NAMEPLATE.value);
        else if (Minecraft.getMinecraft().gameSettings.hideGUI || livingEntity.getUniqueID().equals(PopupMenu.getEntityId()))
            cir.setReturnValue(false);
        else if (!AvatarManager.panic) {
            if (Configs.SELF_NAMEPLATE.value && livingEntity == Minecraft.getMinecraft().player)
                cir.setReturnValue(true);
            else if (Configs.NAMEPLATE_RENDER.value == 2 || (Configs.NAMEPLATE_RENDER.value == 1 && livingEntity != FiguraMod.extendedPickEntity))
                cir.setReturnValue(false);
        }
    }

    @Inject(method = "applyRotations", at = @At("TAIL"))
    private void figura$isEntityUpsideDown(T entity, float ageInTicks, float rotationYaw, float partialTicks, CallbackInfo ci) {
        Avatar avatar = AvatarManager.getAvatar(entity);
        if (RenderUtils.vanillaModelAndScript(avatar)) {
            Boolean upsideDown = avatar.luaRuntime.renderer.upsideDown;
            if (upsideDown != null && upsideDown) {
                GlStateManager.translate(0.0F, entity.height + 0.1F, 0.0F);
                GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
            }
        }
    }
}
