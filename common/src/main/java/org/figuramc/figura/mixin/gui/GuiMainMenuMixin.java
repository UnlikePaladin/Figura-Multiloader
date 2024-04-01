package org.figuramc.figura.mixin.gui;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import org.figuramc.figura.FiguraMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GuiMainMenu.class)
public class GuiMainMenuMixin extends GuiScreen {
//TODO : check unformatted or formatted text, idk which is the right one
    @ModifyArg(method = "drawScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;getStringWidth(Ljava/lang/String;)I"))
    private String getSplashWidth(String text) {
        return FiguraMod.splashText == null ? text : FiguraMod.splashText.getUnformattedText();
    }

    @ModifyArg(method = "drawScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiMainMenu;drawCenteredString(Lnet/minecraft/client/gui/FontRenderer;Ljava/lang/String;III)V"), index = 1)
    private String drawSplashText(FontRenderer textRenderer, String text, int centerX, int y, int color) {
        if (FiguraMod.splashText == null)
            return text;

        drawCenteredString(textRenderer, FiguraMod.splashText.getUnformattedText(), centerX, y, color);
        return "";
    }
}
