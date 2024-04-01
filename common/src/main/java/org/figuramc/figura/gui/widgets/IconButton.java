package org.figuramc.figura.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.figuramc.figura.utils.ui.UIHelper;

public class IconButton extends Button {

    public IconButton(int x, int y, int width, int height, int u, int v, int regionSize, ResourceLocation texture, int textureWidth, int textureHeight, ITextComponent text, ITextComponent tooltip, ButtonAction pressAction) {
        super(x, y, width, height, u, v, regionSize, texture, textureWidth, textureHeight, text, tooltip, pressAction);
    }

    @Override
    protected void renderTexture(Minecraft mc, float delta) {
        this.renderDefaultTexture(mc, delta);

        UIHelper.setupTexture(texture);
        int size = getTextureSize();
        UIHelper.blit(getX() + 2, getY() + (getHeight() - size) / 2, size, size, u, v, regionSize, regionSize, textureWidth, textureHeight);
    }

    @Override
    protected void renderText(Minecraft mc, float delta) {
        int size = getTextureSize();
        UIHelper.renderCenteredScrollingText(getMessage(), getX() + 4 + size, getY(), getWidth() - 6 - size, getHeight(), getTextColor());
    }

    protected int getTextureSize() {
        return Math.min(getWidth(), getHeight()) - 4;
    }
}
