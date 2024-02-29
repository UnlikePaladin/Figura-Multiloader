package org.figuramc.figura.mixin.render;

import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.text.TextFormatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EnumDyeColor.class)
public interface EnumDyeColorAccessor {
    @Final
    @Accessor("chatColor")
    @Intrinsic
    TextFormatting getChatColor();
}
