package org.figuramc.figura.utils;

import net.minecraft.util.text.TextComponentTranslation;
import org.figuramc.figura.FiguraMod;

public class FiguraText extends TextComponentTranslation {

    public FiguraText() {
        super(FiguraMod.MOD_ID);
    }

    public FiguraText(String string) {
        super(FiguraMod.MOD_ID + "." + string);
    }

    public FiguraText(String string, Object... args) {
        super(FiguraMod.MOD_ID + "." + string, args);
    }
}