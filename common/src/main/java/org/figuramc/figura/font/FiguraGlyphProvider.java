package org.figuramc.figura.font;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import net.minecraft.client.renderer.texture.TextureUtil;

import java.awt.image.BufferedImage;

public class FiguraGlyphProvider {
    private BufferedImage image;
    private final Int2ObjectMap<FiguraGlyph> glyphs;

    public FiguraGlyphProvider(BufferedImage image, Int2ObjectMap<FiguraGlyph> glyphs) {
        this.image = image;
        this.glyphs = glyphs;
    }

    public void close() {
        this.image = null;
        this.glyphs.clear();
    }

    public FiguraGlyph getGlyph(int character) {
        return this.glyphs.get(character);
    }

    public IntSet getSupportedGlyphs() {
        return IntSets.unmodifiable(this.glyphs.keySet());
    }

    public static class FiguraGlyph {
        private int glTextureId;
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
            return 10.0f - (float) this.ascent;
        }

        public void upload(int xOffset, int yOffset) {
            deleteGlTexture();
            TextureUtil.uploadTextureImageSub(getGlTextureId(), image, xOffset + this.offsetX, yOffset + this.offsetY, false, false);
            //this.image.upload(0, xOffset, yOffset, this.offsetX, this.offsetY, this.width, this.height, false, false);
        }

        public float getLeft() {
            return 0;
        }

        public float getRight() {
            return this.getLeft() + (float) this.getPixelWidth() / this.getOversample();
        }

        public float getUp() {
            return this.getBearingY();
        }

        public float getDown() {
            return this.getUp() + (float) this.getPixelHeight() / this.getOversample();
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


        public int getGlTextureId() {
            if (this.glTextureId == -1) {
                this.glTextureId = TextureUtil.glGenTextures();
            }

            return this.glTextureId;
        }

        public void deleteGlTexture() {
            if (this.glTextureId != -1) {
                TextureUtil.deleteTexture(this.glTextureId);
                this.glTextureId = -1;
            }

        }
    }
}