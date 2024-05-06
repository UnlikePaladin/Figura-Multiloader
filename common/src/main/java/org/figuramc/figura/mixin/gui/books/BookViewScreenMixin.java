package org.figuramc.figura.mixin.gui.books;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.util.text.TextComponentString;
import org.figuramc.figura.font.Emojis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiScreenBook.class)
public class BookViewScreenMixin {
    @Redirect(method = "drawScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I"))
    public int render(FontRenderer font, String string, int i, int j, int color) {
        return font.drawString(Emojis.applyEmojis(new TextComponentString(string)).getUnformattedText(), i, j, color);
    }

    @Redirect(method = "drawScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawSplitString(Ljava/lang/String;IIII)V"))
    public void render(FontRenderer font, String string, int i, int j, int k, int color) {
        font.drawSplitString(Emojis.applyEmojis(new TextComponentString(string)).getUnformattedText(), i, j, k, color);
    }
}
