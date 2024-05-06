package org.figuramc.figura.mixin.gui;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiChat.class)
public class ChatScreenMixin {

    @Shadow protected GuiTextField inputField;

    @ModifyArg(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiChat;sendChatMessage(Ljava/lang/String;)V"), method = "keyTyped")
    private String sendMessage(String text) {
        String s = text;
        Avatar avatar = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());
        if (avatar != null && !text.trim().isEmpty())
            s = avatar.chatSendMessageEvent(text);

        if (!text.equals(s))
            FiguraMod.LOGGER.info("Changed chat message from \"{}\" to \"{}\"", text, s);

        return s;
    }

    @Inject(at = @At("HEAD"), method = "drawScreen")
    private void render(int i, int j, float f, CallbackInfo ci) {
        Avatar avatar = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());
        if (avatar == null || avatar.luaRuntime == null)
            return;

        Integer color = avatar.luaRuntime.host.chatColor;
        if (color == null)
            return;

        this.inputField.setTextColor(color);
    }
}
