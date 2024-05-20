package org.figuramc.figura.font;

import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import org.figuramc.figura.model.rendering.texture.RenderTypes;

import java.awt.image.BufferedImage;

public class FiguraGlyph {
    private final float scale;
    private final BufferedImage image;
    private final int offsetX;
    private final int offsetY;
    private final int width;
    private final int height;
    private final int advance;
    private final int ascent;

    public FiguraGlyph(float scale, BufferedImage image, int offsetX, int offsetY, int width, int height, int advance, int ascent) {
        this.scale = scale;
        this.image = image;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.width = width;
        this.height = height;
        this.advance = advance;
        this.ascent = ascent;
    }

    public float getOversample() {
        return 1.0f / this.scale;
    }

    public int getPixelWidth() {
        return this.width;
    }

    public int getPixelHeight() {
        return this.height;
    }

    public float getAdvance() {
        return this.advance;
    }

    public float getBearingY() {
        return 10.0f - (float)this.ascent;
    }

    public void upload(int xOffset, int yOffset) {
     //  TextureUtil.uploadTextureImageSub(0, image, xOffset, yOffset, false, false);
        this.image.upload(0, xOffset, yOffset, this.offsetX, this.offsetY, this.width, this.height, false, false);
    }

    public float getLeft() {
        return 0;
    }

    public float getRight() {
        return this.getLeft() + (float)this.getPixelWidth() / this.getOversample();
    }

    public float getUp() {
        return this.getBearingY();
    }

    public float getDown() {
        return this.getUp() + (float)this.getPixelHeight() / this.getOversample();
    }

    public float getAdvance(boolean bold) {
        return this.getAdvance() + (bold ? this.getBoldOffset() : 0.0f);
    }

    public float getBoldOffset() {
        return 1.0f;
    }

    public float getShadowOffset() {
        return 1.0f;
    }
}
