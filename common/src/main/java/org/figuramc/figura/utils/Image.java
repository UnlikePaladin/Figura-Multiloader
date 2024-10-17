package org.figuramc.figura.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.lwjgl.opengl.GL11;

import java.awt.image.BufferedImage;
import java.nio.Buffer;
import java.nio.IntBuffer;

public class Image {
    int width, height;
    Format format;
    BufferedImage image;
    private static final IntBuffer DATA_BUFFER = GLAllocation.createDirectIntBuffer(4194304);

    public Image(Format format, BufferedImage image) {
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.format = format;
        this.image = image;
    }

    public void setPackPixelStoreState() {
        GlStateManager.glPixelStorei(GL11.GL_PACK_ALIGNMENT, format.componentCount);
    }

    public void setUnpackPixelStoreState() {
        GlStateManager.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, format.componentCount);
    }

    private void _upload(int level, int xOffset, int yOffset, int unpackSkipPixels, int unpackSkipRows, int width, int height, boolean blur, boolean clamp, boolean mipmap, boolean autoClose) {

        setTexFilter(blur, mipmap);
        setTexClamp(clamp);
        if (width == this.width) {
            GlStateManager.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, GL11.GL_FALSE);
        } else {
            GlStateManager.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, this.width);
        }
        GlStateManager.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, unpackSkipPixels);
        GlStateManager.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, unpackSkipRows);

        setUnpackPixelStoreState();
        int offset = 4194304 / this.width;
        int[] is = new int[this.width * offset];

        for(int n = 0; n < this.width * this.height; n += this.width * offset) {
          //  int o = n / k;
          //  int p = Math.min(offset, l - o);
       //    int q = k * p;
          //  image.getRGB(0, o, k, p, is, 0, k);
        //    copyToBuffer(is, q);
        //    GlStateManager.glTexSubImage2D(GL11.GL_TEXTURE_2D, level, xOffset, j + o, k, p, 32993, 33639, DATA_BUFFER);
        }

        GlStateManager.glTexSubImage2D(GL11.GL_TEXTURE_2D, level, xOffset, yOffset, width, height, this.format.glFormat, GL11.GL_UNSIGNED_BYTE, DATA_BUFFER);
        if (autoClose) {
       //     this.close();
        }
    }

    private static void copyToBuffer(int[] is, int i) {
        copyToBufferPos(is, 0, i);
    }

    private static void copyToBufferPos(int[] is, int i, int j) {
        int[] js = is;
        if (Minecraft.getMinecraft().gameSettings.anaglyph) {
            js = TextureUtil.updateAnaglyph(is);
        }

        DATA_BUFFER.clear();
        DATA_BUFFER.put(js, i, j);
        DATA_BUFFER.position(0).limit(j);
    }

    public void setTexFilter(boolean linear, boolean mipmap) {
        if (linear) {
            GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, mipmap ? GL11.GL_LINEAR_MIPMAP_LINEAR : GL11.GL_LINEAR);
            GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        } else {
            GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, mipmap ? GL11.GL_NEAREST_MIPMAP_LINEAR : GL11.GL_NEAREST);
            GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        }
    }

    public void setTexClamp(boolean bl) {
        if (bl) {
            GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
            GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        } else {
            GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        }
    }

    enum Format {
        RGBA(4, GL11.GL_RGBA, (1 << 4) | (1 << 3) | (1 << 2) | (1), 0, 8, 16, 255, 24),
        RGB(3, GL11.GL_RGB, (1 << 4) | (1 << 3) | (1 << 2), 0, 8, 16, 255, 255),
        LUMINANCE_ALPHA(2, GL11.GL_LUMINANCE_ALPHA, (1 << 1) | (1), 255, 255, 255, 0, 8),
        LUMINANCE(1, GL11.GL_LUMINANCE, (1 << 1), 0, 0, 0, 0, 255);

        public final int componentCount, glFormat, hasComponent, redOffset, greenOffset, blueOffset, alphaOffset, luminanceOffset;
        Format(int componentCount, int glFormat, int hasComponent, int redOffset, int greenOffset, int blueOffset, int alphaOffset, int luminanceOffset) {
            this.componentCount = componentCount;
            this.glFormat = glFormat;
            this.hasComponent = hasComponent;
            this.redOffset = redOffset;
            this.greenOffset = greenOffset;
            this.blueOffset = blueOffset;
            this.alphaOffset = alphaOffset;
            this.luminanceOffset = luminanceOffset;
        }

        public boolean hasRed() {
            return (hasComponent & (1 << 4)) != 0;
        }
        public boolean hasGreen() {
            return (hasComponent & (1 << 3)) != 0;
        }
        public boolean hasBlue(){
            return (hasComponent & (1 << 2)) != 0;
        }
        public boolean hasLuminance(){
            return (hasComponent & (1 << 1)) != 0;
        }
        public boolean hasAlpha() {
            return (hasComponent & 1) != 0;
        }
    }
}
