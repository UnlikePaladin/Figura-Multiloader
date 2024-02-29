package org.figuramc.figura.mixin.font;

import net.minecraft.util.text.TextFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TextFormatting.class)
public interface TextFormattingAccessor {
    @Accessor("formattingCode")
    char getFormattingCode();
}
