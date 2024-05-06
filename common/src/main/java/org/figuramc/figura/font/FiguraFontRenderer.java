package org.figuramc.figura.font;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import org.figuramc.figura.mixin.font.FontRendererAccessor;

public class FiguraFontRenderer extends FontRenderer {

    public FiguraFontRenderer(GameSettings gameSettings, ResourceLocation resourceLocation, TextureManager textureManager, boolean bl) {
        super(gameSettings, resourceLocation, textureManager, bl);
    }

    @Override
    public int drawString(String string, float f, float g, int i, boolean dropShadow) {
        ((FontRendererAccessor)this).setRed((i >> 16 & 0xFF) / 255.0F);
        ((FontRendererAccessor)this).setBlue((i >> 8 & 0xFF) / 255.0F);
        ((FontRendererAccessor)this).setGreen((i & 0xFF) / 255.0F);
        ((FontRendererAccessor)this).setAlpha((i >> 24 & 0xFF) / 255.0F);
        GlStateManager.color(((FontRendererAccessor)this).red(), ((FontRendererAccessor)this).blue(), ((FontRendererAccessor)this).green(), ((FontRendererAccessor)this).alpha());
        ((FontRendererAccessor)this).setPosX(f);
        ((FontRendererAccessor)this).setPosY(g);
        return super.drawString(string, f, g, i, dropShadow);
    }


    @Override
    public int drawEmoji(String string, float f, float g, int i, boolean dropShadow) {
        for(int z = 0; z < string.length(); ++z) {
            int codePoint = string.codePointAt(z);
            if ()
        }
        ((FontRendererAccessor)this).setRed((i >> 16 & 0xFF) / 255.0F);
        ((FontRendererAccessor)this).setBlue((i >> 8 & 0xFF) / 255.0F);
        ((FontRendererAccessor)this).setGreen((i & 0xFF) / 255.0F);
        ((FontRendererAccessor)this).setAlpha((i >> 24 & 0xFF) / 255.0F);
        GlStateManager.color(((FontRendererAccessor)this).red(), ((FontRendererAccessor)this).blue(), ((FontRendererAccessor)this).green(), ((FontRendererAccessor)this).alpha());
        ((FontRendererAccessor)this).setPosX(f);
        ((FontRendererAccessor)this).setPosY(g);
        return super.drawString(string, f, g, i, dropShadow);
    }
}
