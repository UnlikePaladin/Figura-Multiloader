package org.figuramc.figura.mixin.gui.books;

import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.util.text.TextComponentString;
import org.figuramc.figura.font.Emojis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GuiScreenBook.class)
public class LineInfoMixin {

    @ModifyArg(method = "drawScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I"), index = 0)
    public String test(String string) {
        return Emojis.applyEmojis(new TextComponentString(string)).getUnformattedText();
    }

    @ModifyArg(method = "drawScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawSplitString(Ljava/lang/String;IIII)V"), index = 0)
    public String testM(String string) {
        return Emojis.applyEmojis(new TextComponentString(string)).getUnformattedText();
    }

}
