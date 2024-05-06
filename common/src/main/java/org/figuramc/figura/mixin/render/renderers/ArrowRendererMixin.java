package org.figuramc.figura.mixin.render.renderers;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderArrow;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityArrow;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.lua.api.entity.EntityAPI;
import org.figuramc.figura.model.rendering.texture.RenderTypes;
import org.figuramc.figura.permissions.Permissions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderArrow.class)
public abstract class ArrowRendererMixin<T extends EntityArrow> extends Render<T> {

    protected ArrowRendererMixin(RenderManager dispatcher) {
        super(dispatcher);
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;scale(FFF)V"), method = "doRender(Lnet/minecraft/entity/projectile/EntityArrow;DDDFF)V", cancellable = true)
    private void render(T abstractArrow, double xPos, double yPos, double zPos, float yaw, float tickDelta, CallbackInfo ci) {
        Entity owner = abstractArrow.shootingEntity;
        if (owner == null)
            return;

        Avatar avatar = AvatarManager.getAvatar(owner);
        if (avatar == null || avatar.permissions.get(Permissions.VANILLA_MODEL_EDIT) == 0)
            return;

        FiguraMod.pushProfiler(FiguraMod.MOD_ID);
        FiguraMod.pushProfiler(avatar);
        FiguraMod.pushProfiler("arrowRender");

        FiguraMod.pushProfiler("event");
        boolean bool = avatar.arrowRenderEvent(tickDelta, EntityAPI.wrap(abstractArrow));

        FiguraMod.popPushProfiler("render");
        if (bool || avatar.renderArrow(RenderTypes.FiguraBufferSource.INSTANCE, tickDelta, abstractArrow.getBrightnessForRender())) {
            GlStateManager.popMatrix();
            ci.cancel();
        }

        FiguraMod.popProfiler(4);
    }
}
