package org.figuramc.figura.mixin.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.extensions.Vector3fExtension;
import org.figuramc.figura.utils.ColorUtils;
import org.figuramc.figura.utils.RenderUtils;
import org.figuramc.figura.utils.ui.UIHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Render.class)
public class RenderMixin<T extends Entity> {
    @Shadow protected boolean renderOutlines;
    @Unique private Avatar avatar;

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    public void modifyTeamColor(T entityIn, CallbackInfoReturnable<Integer> cir) {
        Avatar avatar = AvatarManager.getAvatar(entityIn);
        if (avatar == null)
            return;

        if (renderOutlines && RenderUtils.vanillaModelAndScript(avatar) && avatar.luaRuntime.renderer.outlineColor != null) {
            int i = ColorUtils.rgbToInt(avatar.luaRuntime.renderer.outlineColor);
            cir.setReturnValue(i);
        }
    }

    @Inject(method = "renderEntityOnFire", at = @At("HEAD"), cancellable = true)
    private void renderFlame(T entity, double d, double e, double f, float partialTicks, CallbackInfo ci) {
        Avatar a = AvatarManager.getAvatar(entity);
        if (RenderUtils.vanillaModelAndScript(a)) {
            if (!a.luaRuntime.renderer.renderFire) {
                ci.cancel();
            } else {
                avatar = a;
            }
        }
    }

    @Redirect(method = "renderEntityOnFire", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;rotate(FFFF)V"))
    private void renderFlameRot(float f, float g, float h, float z) {
        if (UIHelper.paperdoll) {
            GlStateManager.rotate(((Vector3fExtension)UIHelper.YP).figura$rotationDegrees(UIHelper.fireRot));
        } else {
            GlStateManager.rotate(f, g, h, z);
        }
    }

    @ModifyVariable(method = "renderEntityOnFire", at = @At("STORE"), ordinal = 0)
    private TextureAtlasSprite firstFireTexture(TextureAtlasSprite sprite) {
        TextureAtlasSprite s = RenderUtils.firstFireLayer(avatar);
        return s != null ? s : sprite;
    }

    @ModifyVariable(method = "renderEntityOnFire", at = @At("STORE"), ordinal = 1)
    private TextureAtlasSprite secondFireTexture(TextureAtlasSprite sprite) {
        TextureAtlasSprite s = RenderUtils.secondFireLayer(avatar);
        avatar = null;
        return s != null ? s : sprite;
    }

    @ModifyVariable(method = "renderShadow", at = @At("STORE"), ordinal = 2)
    private static float modifyShadowSize(float h, Entity entity) {
        Avatar avatar = AvatarManager.getAvatar(entity);
        if (RenderUtils.vanillaModelAndScript(avatar) && avatar.luaRuntime.renderer.shadowRadius != null)
            return avatar.luaRuntime.renderer.shadowRadius;
        return h;
    }

    @Inject(method = "doRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void render(T entity, double d, double e, double f, float g, float partialTicks, CallbackInfo ci) {
        List<Entity> passengers = entity.getPassengers();
        if (passengers == null || passengers.isEmpty())
            return;

        Avatar avatar = AvatarManager.getAvatar(passengers.get(0));
        if (RenderUtils.vanillaModelAndScript(avatar) && !avatar.luaRuntime.renderer.renderVehicle)
            ci.cancel();
    }
}
