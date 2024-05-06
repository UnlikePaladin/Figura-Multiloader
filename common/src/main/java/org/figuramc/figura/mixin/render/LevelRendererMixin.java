package org.figuramc.figura.mixin.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.math.vector.FiguraVec4;
import org.figuramc.figura.model.rendering.EntityRenderMode;
import org.figuramc.figura.model.rendering.texture.RenderTypes;
import org.figuramc.figura.utils.MathUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderGlobal.class)
public abstract class LevelRendererMixin {

    @Shadow @Final private Minecraft mc;

    @Shadow @Final private RenderManager renderManager;

    @ModifyArg(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderManager;renderEntityStatic(Lnet/minecraft/entity/Entity;FZ)V"))
    private Entity renderLevelRenderEntity(Entity entity) {
        Avatar avatar = AvatarManager.getAvatar(entity);
        if (avatar != null)
            avatar.renderMode = EntityRenderMode.RENDER;
        return entity;
    }

    @Inject(method = "renderEntities", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;", shift = At.Shift.BEFORE),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderHelper;enableStandardItemLighting()V"),
                    to = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/CompiledChunk;getTileEntities()Ljava/util/List;")
            ))
    private void renderLevelFirstPerson(Entity entity, ICamera camera, float tickDelta, CallbackInfo ci) {
        if (this.mc.gameSettings.thirdPersonView != 0)
            return;

        Entity e = mc.getRenderViewEntity();
        Avatar avatar = AvatarManager.getAvatar(e);

        if (avatar == null)
            return;

        // first person world parts
        avatar.firstPersonWorldRender(e, RenderTypes.FiguraBufferSource.INSTANCE, camera, tickDelta);

        // first person matrices
        if (!(e instanceof EntityLivingBase) || !Configs.FIRST_PERSON_MATRICES.value)
            return;
        EntityLivingBase livingEntity = (EntityLivingBase) e;

        Avatar.firstPerson = true;
        GlStateManager.pushMatrix();

        Render<? super EntityLivingBase> entityRenderer = this.renderManager.getEntityRenderObject(livingEntity);
        Vec3d offset = livingEntity.isSneaking() ? new Vec3d(0.0, -0.125, 0.0) : new Vec3d(0,0,0);
        Vec3d camPos = camera instanceof Frustum ? new Vec3d(((FrustumAccessor)camera).cameraPosX(), ((FrustumAccessor)camera).cameraPosY(), ((FrustumAccessor)camera).cameraPosZ()) : new Vec3d(0,0,0);

        double xPos = (MathUtils.lerp(tickDelta, livingEntity.prevPosX, livingEntity.posX) - camPos.x + offset.x);
        double yPos = (MathUtils.lerp(tickDelta, livingEntity.prevPosY, livingEntity.posY) - camPos.y + offset.y);
        double zPos = (MathUtils.lerp(tickDelta, livingEntity.prevPosZ, livingEntity.posZ) - camPos.z + offset.z);
        GlStateManager.translate(xPos, yPos, zPos); // TODO: Check if translation is necessary

        float yaw = (float) MathUtils.lerp(tickDelta, livingEntity.prevRotationPitch, livingEntity.rotationPitch);
        entityRenderer.doRender(livingEntity, xPos, yPos, zPos, yaw, tickDelta);

        GlStateManager.popMatrix();
        Avatar.firstPerson = false;
    }


    @ModifyArg(method = "drawSelectionBox", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;drawSelectionBoundingBox(Lnet/minecraft/util/math/AxisAlignedBB;FFFF)V"), index = 1)
    private float renderHitOutlineX(float x) {
        Avatar avatar = AvatarManager.getAvatar(this.mc.getRenderViewEntity());
        FiguraVec4 color;

        if (avatar == null || avatar.luaRuntime == null || (color = avatar.luaRuntime.renderer.blockOutlineColor) == null)
            return x;

        return (float) color.x;
    }

    @ModifyArg(method = "drawSelectionBox", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;drawSelectionBoundingBox(Lnet/minecraft/util/math/AxisAlignedBB;FFFF)V"), index = 2)
    private float renderHitOutlineY(float y) {
        Avatar avatar = AvatarManager.getAvatar(this.mc.getRenderViewEntity());
        FiguraVec4 color;

        if (avatar == null || avatar.luaRuntime == null || (color = avatar.luaRuntime.renderer.blockOutlineColor) == null)
            return y;

        return (float) color.y;
    }

    @ModifyArg(method = "drawSelectionBox", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;drawSelectionBoundingBox(Lnet/minecraft/util/math/AxisAlignedBB;FFFF)V"), index = 3)
    private float renderHitOutlineZ(float z) {
        Avatar avatar = AvatarManager.getAvatar(this.mc.getRenderViewEntity());
        FiguraVec4 color;

        if (avatar == null || avatar.luaRuntime == null || (color = avatar.luaRuntime.renderer.blockOutlineColor) == null)
            return z;

        return (float) color.z;
    }

    @ModifyArg(method = "drawSelectionBox", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;drawSelectionBoundingBox(Lnet/minecraft/util/math/AxisAlignedBB;FFFF)V"), index = 4)
    private float renderHitOutlineW(float w) {
        Avatar avatar = AvatarManager.getAvatar(this.mc.getRenderViewEntity());
        FiguraVec4 color;

        if (avatar == null || avatar.luaRuntime == null || (color = avatar.luaRuntime.renderer.blockOutlineColor) == null)
            return w;

        return (float) color.w;
    }
}