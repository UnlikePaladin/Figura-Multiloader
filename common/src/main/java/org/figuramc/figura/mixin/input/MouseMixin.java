package org.figuramc.figura.mixin.input;

import net.minecraft.client.Minecraft;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.gui.ActionWheel;
import org.figuramc.figura.gui.PopupMenu;
import org.figuramc.figura.lua.api.keybind.FiguraKeybind;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Shadow private static ByteBuffer buttons;
    @Shadow private static int dwheel;

    @Shadow
    public static boolean isGrabbed() {
        return false;
    }

    @Shadow private static IntBuffer coord_buffer;
    @Shadow private static int event_dwheel;
    @Unique
    static int GLFW_MOD_SHIFT = 0x0001;
    @Unique
    static int GLFW_MOD_CONTROL = 0x0002;
    @Unique
    static int GLFW_MOD_ALT = 0x0004;
    @Unique
    static int GLFW_MOD_CAPS_LOCK = 0x0010;
    @Unique
    static int GLFW_MOD_NUM_LOCK = 0x0020;

    @Inject(method = "poll", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/InputImplementation;pollMouse(Ljava/nio/IntBuffer;Ljava/nio/ByteBuffer;)V", shift = At.Shift.AFTER), cancellable = true)
    private static void modifyButton(CallbackInfo ci) {
        Avatar avatar = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());
        if (avatar == null || avatar.luaRuntime == null)
            return;

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

        for (int i = 0; i <= 1; i++) {
            if (avatar.mousePressEvent(i, buttons.get(i), modifiers) && (Mouse.isGrabbed() || Minecraft.getMinecraft().currentScreen == null)) {
                buttons.put(i, (byte) 0);
                continue;
            }

            boolean pressed = buttons.get(i) != 0;

            if (avatar.luaRuntime != null && FiguraKeybind.set(avatar.luaRuntime.keybinds.keyBindings, i, pressed, modifiers))
                buttons.put(i, (byte) 0);

            if (avatar.luaRuntime != null && pressed && avatar.luaRuntime.host.unlockCursor && Minecraft.getMinecraft().currentScreen == null)
                buttons.put(i, (byte) 0);

            if (avatar.luaRuntime != null && pressed && ActionWheel.isEnabled()) {
                if (i <= 1) ActionWheel.execute(ActionWheel.getSelected(), i == 0);
                buttons.put(i, (byte) 0);
            }
        }

        if (avatar != null && avatar.mouseScrollEvent(coord_buffer.get(2)) && (isGrabbed() || Minecraft.getMinecraft().currentScreen == null)) {
            ci.cancel();
            return;
        }

        if (ActionWheel.isEnabled()) {
            ActionWheel.scroll(coord_buffer.get(2));
            ci.cancel();
        } else if (PopupMenu.isEnabled() && PopupMenu.hasEntity()) {
            PopupMenu.scroll(Math.signum(coord_buffer.get(2)));
            ci.cancel();
        }
    }

    @Inject(method = "getEventDWheel", at = @At(value = "HEAD"), cancellable = true)
    private static void changeScroll(CallbackInfoReturnable<Integer> ci) {
        Avatar avatar = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());
        if (avatar != null && avatar.mouseScrollEvent(coord_buffer.get(2)) && (isGrabbed() || Minecraft.getMinecraft().currentScreen == null)) {
            ci.setReturnValue(0);
            return;
        }

        if (ActionWheel.isEnabled()) {
            ActionWheel.scroll(coord_buffer.get(2));
            ci.setReturnValue(0);
        } else if (PopupMenu.isEnabled() && PopupMenu.hasEntity()) {
            PopupMenu.scroll(Math.signum(coord_buffer.get(2)));
            ci.setReturnValue(0);
        }
    }
}
