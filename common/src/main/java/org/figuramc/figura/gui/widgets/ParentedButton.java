package org.figuramc.figura.gui.widgets;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class ParentedButton extends Button {

    private final AbstractContainerElement parent;

    public ParentedButton(int x, int y, int width, int height, ITextComponent text, AbstractContainerElement parent, ButtonAction pressAction) {
        super(x, y, width, height, text, null, pressAction);
        this.parent = parent;
    }

    public ParentedButton(int x, int y, int width, int height, int u, int v, int regionSize, ResourceLocation texture, int textureWidth, int textureHeight, ITextComponent tooltip, AbstractContainerElement parent, ButtonAction pressAction) {
        super(x, y, width, height, u, v, regionSize, texture, textureWidth, textureHeight, tooltip, pressAction);
        this.parent = parent;
    }

    @Override
    public boolean mouseOver(double mouseX, double mouseY) {
        return this.parent.hovered && super.mouseOver(mouseX, mouseY);
    }
}
