package org.figuramc.figura.mixin.input;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MouseHelper;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.gui.ActionWheel;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHelper.class)
public class MouseHandlerMixin {

    @Inject(method = "mouseXYChange", at = @At("HEAD"), cancellable = true)
    private void onMove(CallbackInfo ci) {
        Avatar avatar = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());

        if (avatar != null && avatar.mouseMoveEvent(Mouse.getDX(), Mouse.getDY()) && (Mouse.isGrabbed() || Minecraft.getMinecraft().currentScreen == null)) {
            ci.cancel();
        }
    }

    @Inject(method = "grabMouseCursor", at = @At("HEAD"), cancellable = true)
    private void grabMouse(CallbackInfo ci) {
        Avatar avatar = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());
        if (ActionWheel.isEnabled() || (avatar != null && avatar.luaRuntime != null && avatar.luaRuntime.host.unlockCursor))
            ci.cancel();
    }
}
