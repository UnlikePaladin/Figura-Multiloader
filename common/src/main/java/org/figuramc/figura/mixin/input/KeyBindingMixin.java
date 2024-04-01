package org.figuramc.figura.mixin.input;

import net.minecraft.client.settings.KeyBinding;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.lua.api.keybind.FiguraKeybind;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyBinding.class)
public class KeyBindingMixin {

    @Inject(method = "updateKeyBindState", at = @At("HEAD"))
    private static void setAll(CallbackInfo ci) {
        Avatar avatar = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());
        if (avatar != null && avatar.luaRuntime != null)
            FiguraKeybind.updateAll(avatar.luaRuntime.keybinds.keyBindings);
    }

    @Inject(method = "unPressAllKeys", at = @At("HEAD"))
    private static void releaseAll(CallbackInfo ci) {
        Avatar avatar = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());
        if (avatar != null && avatar.luaRuntime != null)
            FiguraKeybind.releaseAll(avatar.luaRuntime.keybinds.keyBindings);
    }

    @ModifyArg(method = "setKeyBindState", at = @At("HEAD"))
    private static boolean setDown(int keyCode, boolean pressed) {
        if (!pressed)
            return false;

        Avatar avatar = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());
        if (avatar == null || avatar.luaRuntime == null)
            return true;

        return !FiguraKeybind.overridesKey(avatar.luaRuntime.keybinds.keyBindings, keyCode);
    }
}
