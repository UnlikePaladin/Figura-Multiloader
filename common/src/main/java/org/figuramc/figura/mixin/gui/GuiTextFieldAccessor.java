package org.figuramc.figura.mixin.gui;

import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiTextField.class)
public interface GuiTextFieldAccessor {
    @Intrinsic
    @Accessor("height")
    int getHeight();

    @Intrinsic
    @Accessor("width")
    void setWidth(int width);

    @Intrinsic
    @Accessor("isEnabled")
    boolean isEnabled();
}
