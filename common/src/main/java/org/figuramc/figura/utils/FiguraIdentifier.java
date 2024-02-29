package org.figuramc.figura.utils;

import net.minecraft.util.ResourceLocation;
import org.figuramc.figura.FiguraMod;

public class FiguraIdentifier extends ResourceLocation {

    public FiguraIdentifier(String string) {
        super(FiguraMod.MOD_ID, string);
    }

    public static String formatPath(String path) {
        return ResourceUtils.sanitizeName(path, charValue -> charValue == '_' || charValue == '-' || charValue >= 'a' && charValue <= 'z' || charValue >= '0' && charValue <= '9' || charValue == '/' || charValue == '.');
    }
}
