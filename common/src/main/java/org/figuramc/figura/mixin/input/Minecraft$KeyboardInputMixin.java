package org.figuramc.figura.mixin.input;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.lua.api.keybind.FiguraKeybind;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class Minecraft$KeyboardInputMixin {
    int GLFW_MOD_SHIFT = 0x0001;
    int GLFW_MOD_CONTROL = 0x0002;
    int GLFW_MOD_ALT = 0x0004;
    int GLFW_MOD_CAPS_LOCK = 0x0010;
 	int GLFW_MOD_NUM_LOCK = 0x0020;
    @Shadow public boolean inGameHasFocus;

    @Shadow @Nullable public GuiScreen currentScreen;

    @Inject(method = "runTickKeyboard", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;getEventCharacter()C"), slice = @Slice(from = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;next()Z"), to = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getSystemTime()J")), cancellable = true)
    private void keyPress(CallbackInfo ci) {
        int modifiers = 0;
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
            modifiers = GLFW_MOD_SHIFT | modifiers;
        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))
            modifiers = GLFW_MOD_CONTROL | modifiers;
        if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU))
            modifiers = GLFW_MOD_ALT | modifiers;
        if (Keyboard.isKeyDown(Keyboard.KEY_NUMLOCK))
            modifiers = GLFW_MOD_NUM_LOCK | modifiers;
        if (Keyboard.isKeyDown(Keyboard.KEY_CAPITAL))
            modifiers = GLFW_MOD_CAPS_LOCK | modifiers;

        if (!this.inGameHasFocus)
            return;

        if (Keyboard.getEventKeyState() && Configs.PANIC_BUTTON.keyBind.getKeyCode() == Keyboard.getEventKey()) {
            AvatarManager.togglePanic();
            ci.cancel();
        }

        Avatar avatar = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());
        if (avatar == null || avatar.luaRuntime == null)
            return;

        if (avatar.keyPressEvent(Keyboard.getEventKey(), Keyboard.getEventKeyState() ? 1 : 0, modifiers) && (Mouse.isGrabbed() || this.currentScreen == null)) {
            ci.cancel();
            return;
        }
        int key;
        if (Keyboard.getEventKey() != 0) {
            key =  Keyboard.getEventKey();
        } else {
            key = Keyboard.getEventCharacter() +256;
        }
        if (avatar.luaRuntime != null && FiguraKeybind.set(avatar.luaRuntime.keybinds.keyBindings, key, Keyboard.getEventKeyState(), modifiers)) {
            KeyBinding.updateKeyBindState();
            ci.cancel();
        }

        avatar.charTypedEvent(String.valueOf(Keyboard.getEventCharacter()), modifiers, Keyboard.getEventCharacter());
    }
}
