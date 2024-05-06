package org.figuramc.figura.mixin.render.renderers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.utils.RenderUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public class ScreenEffectRendererMixin {

    @Shadow @Final private Minecraft mc;
    @Unique
    private static Avatar avatar;

    @Inject(method = "renderFireInFirstPerson", at = @At("HEAD"), cancellable = true)
    private void renderFire(CallbackInfo ci) {
        Avatar a = AvatarManager.getAvatar(mc.getRenderViewEntity());
        if (RenderUtils.vanillaModelAndScript(a)) {
            if (!a.luaRuntime.renderer.renderFire) {
                ci.cancel();
            } else {
                avatar = a;
            }
        }
    }

    @ModifyVariable(method = "renderFireInFirstPerson", at = @At("LOAD"), ordinal = 0)
    private TextureAtlasSprite secondFireTexture(TextureAtlasSprite sprite) {
        TextureAtlasSprite s = RenderUtils.secondFireLayer(avatar);
        avatar = null;
        return s != null ? s : sprite;
    }
}
