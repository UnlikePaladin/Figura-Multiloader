package org.figuramc.figura.ducks.extensions;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.Style;

public interface StyleExtension {
    Style setFont(ResourceLocation location);
    ResourceLocation getFont();

    Style setRGBColor(int rgb);

    Integer getRGBColor();

    Style applyStyleToStyle(Style style);
}
