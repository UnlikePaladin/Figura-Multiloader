package org.figuramc.figura.mixin.render;

import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.utils.RenderUtils;
import org.figuramc.figura.utils.ui.UIHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ActiveRenderInfo.class)
public abstract class CameraMixin {


    @Unique private static Avatar avatar;

    @Shadow private static float rotationX;

    @Shadow private static float rotationZ;

    @Shadow private static float rotationYZ;

    @Shadow private static float rotationXY;

    @Shadow private static float rotationXZ;

    @Shadow private static Vec3d position;

    @Inject(method = "updateRenderInfo", at = @At(value = "RETURN"))
    private static void setupRot(EntityPlayer focusedEntity, boolean bl, CallbackInfo ci) {
        avatar = AvatarManager.getAvatar(focusedEntity);
        if (!RenderUtils.vanillaModelAndScript(avatar)) {
            avatar = null;
            return;
        }

        float x = focusedEntity.rotationPitch;
        float y  = focusedEntity.rotationYaw;

        FiguraVec3 rot = avatar.luaRuntime.renderer.cameraRot;
        if (rot != null && rot.notNaN()) {
            x = (float) rot.x;
            y = (float) rot.y;
        }

        FiguraVec3 offset = avatar.luaRuntime.renderer.cameraOffsetRot;
        if (offset != null && offset.notNaN()) {
            x += (float) offset.x;
            y += (float) offset.y;
        }
        int i = bl ? 1 : 0;
        rotationX = MathHelper.cos(y * (float) (Math.PI / 180.0)) * (float)(1 - i * 2);
        rotationZ = MathHelper.sin(y * (float) (Math.PI / 180.0)) * (float)(1 - i * 2);
        rotationYZ = -rotationZ * MathHelper.sin(x * (float) (Math.PI / 180.0)) * (float)(1 - i * 2);
        rotationXY = rotationX * MathHelper.sin(x * (float) (Math.PI / 180.0)) * (float)(1 - i * 2);
        rotationXZ = MathHelper.cos(x * (float) (Math.PI / 180.0));
    }

    @ModifyVariable(method = "projectViewFromEntity", at = @At(value = "STORE"), ordinal = 3)
    private static double setupPivotX(double originalX) {
        if (avatar != null) {
            double x = originalX;

            FiguraVec3 piv = avatar.luaRuntime.renderer.cameraPivot;
            if (piv != null && piv.notNaN()) {
                x = piv.x;
            }

            FiguraVec3 offset = avatar.luaRuntime.renderer.cameraOffsetPivot;
            if (offset != null && offset.notNaN()) {
                x += offset.x;
            }
            return x;
        }
        return originalX;
    }

    @ModifyVariable(method = "projectViewFromEntity", at = @At(value = "STORE"), ordinal = 4)
    private static double setupPivotY(double originalY) {
        if (avatar != null) {
            double y = originalY;

            FiguraVec3 piv = avatar.luaRuntime.renderer.cameraPivot;
            if (piv != null && piv.notNaN()) {
                y = piv.y;
            }

            FiguraVec3 offset = avatar.luaRuntime.renderer.cameraOffsetPivot;
            if (offset != null && offset.notNaN()) {
                y += offset.y;
            }
            return y;
        }
        return originalY;
    }

    @ModifyVariable(method = "projectViewFromEntity", at = @At(value = "STORE"), ordinal = 5)
    private static double setupPivotZ(double originalZ) {
        if (avatar != null) {
            double z = originalZ;

            FiguraVec3 piv = avatar.luaRuntime.renderer.cameraPivot;
            if (piv != null && piv.notNaN()) {
                z = piv.z;
            }

            FiguraVec3 offset = avatar.luaRuntime.renderer.cameraOffsetPivot;
            if (offset != null && offset.notNaN()) {
                z += offset.z;
            }
            return z;
        }
        return originalZ;
    }


    @Inject(method = "updateRenderInfo", at = @At(value = "RETURN"))
    private static void setupPos(EntityPlayer entityplayerIn, boolean bl, CallbackInfo ci) {
        if (avatar != null) {
            FiguraVec3 pos = avatar.luaRuntime.renderer.cameraPos;
            if (pos != null && pos.notNaN()) {
                double x = position.x - pos.x;
                double y = position.y + pos.y;
                double z = position.z - pos.z;
                position = new Vec3d(x, y, z);
            }
            avatar = null;
        }
    }

    @Inject(method = "getRotationX", at = @At("HEAD"), cancellable = true)
    private static void getXRot(CallbackInfoReturnable<Float> cir) {
        if (UIHelper.paperdoll)
            cir.setReturnValue(0f);
    }

    @Inject(method = "getRotationZ", at = @At("HEAD"), cancellable = true)
    private static void getZRot(CallbackInfoReturnable<Float> cir) {
        if (UIHelper.paperdoll)
            cir.setReturnValue(0f);
    }

    @Inject(method = "getRotationYZ", at = @At("HEAD"), cancellable = true)
    private static void getYZRot(CallbackInfoReturnable<Float> cir) {
        if (UIHelper.paperdoll)
            cir.setReturnValue(0f);
    }

    @Inject(method = "getRotationXY", at = @At("HEAD"), cancellable = true)
    private static void getXYRot(CallbackInfoReturnable<Float> cir) {
        if (UIHelper.paperdoll)
            cir.setReturnValue(0f);
    }

    @Inject(method = "getRotationXZ", at = @At("HEAD"), cancellable = true)
    private static void getXZRot(CallbackInfoReturnable<Float> cir) {
        if (UIHelper.paperdoll)
            cir.setReturnValue(0f);
    }
}
