package org.figuramc.figura.mixin.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.extensions.Vector3fExtension;
import org.figuramc.figura.math.matrix.FiguraMat4;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.utils.EntityUtils;
import org.figuramc.figura.utils.RenderUtils;
import org.figuramc.figura.utils.ui.UIHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.FloatBuffer;

@Mixin(EntityRenderer.class)
public abstract class GameRendererMixin {

    @Shadow @Final
    private Minecraft mc;
    @Shadow
    private ShaderGroup shaderGroup;
    @Shadow private boolean useShader;
    @Shadow private float fovModifierHand;

    @Shadow
    protected abstract void loadShader(ResourceLocation id);
    @Shadow public abstract void loadEntityShader(Entity entity);

    @Unique
    private boolean avatarPostShader = false;
    @Unique
    private static FloatBuffer figura$matrixBuf = BufferUtils.createFloatBuffer(16);
    @Inject(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ActiveRenderInfo;updateRenderInfo(Lnet/minecraft/entity/player/EntityPlayer;Z)V", shift = At.Shift.BEFORE))
    private void onCameraRotation(int pass, float tickDelta, long limitTime, CallbackInfo ci) {
        Avatar avatar = AvatarManager.getAvatar(this.mc.getRenderViewEntity() == null ? this.mc.player : this.mc.getRenderViewEntity());
        if (!RenderUtils.vanillaModelAndScript(avatar))
            return;

        float z = 0f;

        FiguraVec3 rot = avatar.luaRuntime.renderer.cameraRot;
        if (rot != null)
            z = (float) rot.z;

        FiguraVec3 offset = avatar.luaRuntime.renderer.cameraOffsetRot;
        if (offset != null)
            z += (float) offset.z;

        GlStateManager.rotate(((Vector3fExtension)UIHelper.ZP).figura$rotationDegrees(z));

        FiguraMat4 mat = avatar.luaRuntime.renderer.cameraMat;
        if (mat != null) {
            figura$matrixBuf.clear();
            mat.toMatrix4f().store(figura$matrixBuf);
            GL11.glLoadMatrix(figura$matrixBuf);
        }
        //FiguraMat3 normal = avatar.luaRuntime.renderer.cameraNormal;
        //if (normal != null) {
       //     stack.last().normal = normal.toMatrix3f();
       // }
    }

    @Inject(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderEntityOutlineFramebuffer()V", shift = At.Shift.AFTER))
    private void render(float tick, long startTime, CallbackInfo ci) {
        Entity entity = this.mc.getRenderViewEntity();
        Avatar avatar = AvatarManager.getAvatar(entity);
        if (!RenderUtils.vanillaModelAndScript(avatar)) {
            if (avatarPostShader) {
                avatarPostShader = false;
                this.loadEntityShader(entity);
            }
            return;
        }

        ResourceLocation resource = avatar.luaRuntime.renderer.postShader;
        if (resource == null) {
            if (avatarPostShader) {
                avatarPostShader = false;
                this.loadEntityShader(entity);
            }
            return;
        }

        try {
            avatarPostShader = true;
            this.useShader = true;
            if (this.shaderGroup == null || !this.shaderGroup.getShaderGroupName().equals(resource.toString()))
                this.loadShader(resource);
        } catch (Exception ignored) {
            this.useShader = false;
            avatar.luaRuntime.renderer.postShader = null;
        }
    }

    @Inject(method = "loadEntityShader", at = @At("HEAD"), cancellable = true)
    private void loadEntityShader(Entity entity, CallbackInfo ci) {
        if (avatarPostShader)
            ci.cancel();
    }

    @Inject(method = "updateFovModifierHand", at = @At("RETURN"))
    private void tickFov(CallbackInfo ci) {
        Avatar avatar = AvatarManager.getAvatar(this.mc.getRenderViewEntity());
        if (RenderUtils.vanillaModelAndScript(avatar)) {
            Float fovModifierHand = avatar.luaRuntime.renderer.fov;
            if (fovModifierHand != null) this.fovModifierHand = fovModifierHand;
        }
    }

    @Inject(method = "getMouseOver", at = @At("RETURN"))
    private void pick(float tickDelta, CallbackInfo ci) {
        FiguraMod.pushProfiler(FiguraMod.MOD_ID);
        FiguraMod.pushProfiler("extendedPick");
        FiguraMod.extendedPickEntity = EntityUtils.getViewedEntity(32);
        FiguraMod.popProfiler(2);
    }

    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void onRenderLevel(float tickDelta, long limitTime, CallbackInfo ci) {
        AvatarManager.executeAll("worldRender", avatar -> avatar.render(tickDelta));
    }

    @Inject(method = "renderWorld", at = @At("RETURN"))
    private void afterRenderLevel(float tickDelta, long limitTime, CallbackInfo ci) {
        AvatarManager.executeAll("postWorldRender", avatar -> avatar.postWorldRenderEvent(tickDelta));
    }

}
