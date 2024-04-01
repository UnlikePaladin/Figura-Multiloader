package org.figuramc.figura.mixin.font;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.text.ITextComponent;
import org.figuramc.figura.ducks.extensions.FontExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Locale;
import java.util.Random;

@Mixin(FontRenderer.class)
public abstract class FontMixin implements FontExtension {
    @Shadow private boolean randomStyle;

    @Shadow private boolean boldStyle;

    @Shadow private boolean strikethroughStyle;

    @Shadow private boolean underlineStyle;

    @Shadow private boolean italicStyle;

    @Shadow private int textColor;

    @Shadow private float alpha;

    @Shadow private float red;

    @Shadow private float blue;

    @Shadow private float green;

    @Shadow public abstract int getCharWidth(char character);

    @Shadow public Random fontRandom;

    @Shadow private boolean unicodeFlag;

    @Shadow private float posX;

    @Shadow private float posY;

    @Shadow public int FONT_HEIGHT;

    @Shadow protected abstract float renderChar(char c, boolean italic);

    @Shadow protected abstract void renderStringAtPos(String string, boolean shadow);

    private static int figura$adjustColor(int color) {
        if ((color & 0xFC000000) == 0) {
            return color | 0xFF000000;
        }
        return color;
    }


    public void figura$drawInBatch8xOutline(ITextComponent text, float x, float y, int color, int outlineColor) {
        int i = figura$adjustColor(outlineColor);
        GlStateManager.enablePolygonOffset();
        GlStateManager.doPolygonOffset(-1.0f, -10.0f);
        figura$renderStringAtPos(text.getFormattedText(), false, i);
        GlStateManager.doPolygonOffset(0.0f, 0.0f);
        GlStateManager.disablePolygonOffset();
        renderStringAtPos(text.getFormattedText(), false);
    }



    private void figura$renderStringAtPos(String text, boolean shadow, int color) {
        for(int i = 0; i < text.length(); ++i) {
            char c0 = text.charAt(i);
            if (c0 == 167 && i + 1 < text.length()) {
                int i1 = "0123456789abcdefklmnor".indexOf(String.valueOf(text.charAt(i + 1)).toLowerCase(Locale.ROOT).charAt(0));
                if (i1 < 16) {
                    this.randomStyle = false;
                    this.boldStyle = false;
                    this.strikethroughStyle = false;
                    this.underlineStyle = false;
                    this.italicStyle = false;

                    this.textColor = color;
                    GlStateManager.color((float)(color >> 16) / 255.0F, (float)(color >> 8 & 0xFF) / 255.0F, (float)(color & 0xFF) / 255.0F, this.alpha);
                } else if (i1 == 16) {
                    this.randomStyle = true;
                } else if (i1 == 17) {
                    this.boldStyle = true;
                } else if (i1 == 18) {
                    this.strikethroughStyle = true;
                } else if (i1 == 19) {
                    this.underlineStyle = true;
                } else if (i1 == 20) {
                    this.italicStyle = true;
                } else if (i1 == 21) {
                    this.randomStyle = false;
                    this.boldStyle = false;
                    this.strikethroughStyle = false;
                    this.underlineStyle = false;
                    this.italicStyle = false;
                    GlStateManager.color(this.red, this.blue, this.green, this.alpha);
                }

                ++i;
            } else {
                int j = "ÀÁÂÈÊËÍÓÔÕÚßãõğİıŒœŞşŴŵžȇ\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜø£Ø×ƒáíóúñÑªº¿®¬½¼¡«»░▒▓│┤╡╢╖╕╣║╗╝╜╛┐└┴┬├─┼╞╟╚╔╩╦╠═╬╧╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀αβΓπΣσμτΦΘΩδ∞∅∈∩≡±≥≤⌠⌡÷≈°∙·√ⁿ²■\u0000"
                        .indexOf(c0);
                if (this.randomStyle && j != -1) {
                    int k = this.getCharWidth(c0);

                    char c1;
                    do {
                        j = this.fontRandom
                                .nextInt(
                                        "ÀÁÂÈÊËÍÓÔÕÚßãõğİıŒœŞşŴŵžȇ\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜø£Ø×ƒáíóúñÑªº¿®¬½¼¡«»░▒▓│┤╡╢╖╕╣║╗╝╜╛┐└┴┬├─┼╞╟╚╔╩╦╠═╬╧╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀αβΓπΣσμτΦΘΩδ∞∅∈∩≡±≥≤⌠⌡÷≈°∙·√ⁿ²■\u0000"
                                                .length()
                                );
                        c1 = "ÀÁÂÈÊËÍÓÔÕÚßãõğİıŒœŞşŴŵžȇ\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜø£Ø×ƒáíóúñÑªº¿®¬½¼¡«»░▒▓│┤╡╢╖╕╣║╗╝╜╛┐└┴┬├─┼╞╟╚╔╩╦╠═╬╧╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀αβΓπΣσμτΦΘΩδ∞∅∈∩≡±≥≤⌠⌡÷≈°∙·√ⁿ²■\u0000"
                                .charAt(j);
                    } while(k != this.getCharWidth(c1));

                    c0 = c1;
                }

                float f1 = j != -1 && !this.unicodeFlag ? 1.0F : 0.5F;
                boolean flag = (c0 == 0 || j == -1 || this.unicodeFlag) && shadow;
                if (flag) {
                    this.posX -= f1;
                    this.posY -= f1;
                }

                float xOffset = this.renderChar(c0, this.italicStyle);
                if (flag) {
                    this.posX += f1;
                    this.posY += f1;
                }

                if (this.boldStyle) {
                    this.posX += f1;
                    if (flag) {
                        this.posX -= f1;
                        this.posY -= f1;
                    }

                    this.renderChar(c0, this.italicStyle);
                    this.posX -= f1;
                    if (flag) {
                        this.posX += f1;
                        this.posY += f1;
                    }

                    ++xOffset;
                }


                Tessellator tessellator;
                BufferBuilder bufferBuilder;
                if (this.strikethroughStyle) {
                    tessellator = Tessellator.getInstance();
                    bufferBuilder = tessellator.getBuffer();
                    GlStateManager.disableTexture2D();
                    bufferBuilder.begin(7, DefaultVertexFormats.POSITION);
                    bufferBuilder.pos(this.posX, this.posY + (float)(this.FONT_HEIGHT / 2), 0.0).endVertex();
                    bufferBuilder.pos(this.posX + xOffset, this.posY + (float)(this.FONT_HEIGHT / 2), 0.0).endVertex();
                    bufferBuilder.pos(this.posX + xOffset, this.posY + (float)(this.FONT_HEIGHT / 2) - 1.0F, 0.0).endVertex();
                    bufferBuilder.pos(this.posX, this.posY + (float)(this.FONT_HEIGHT / 2) - 1.0F, 0.0).endVertex();
                    tessellator.draw();
                    GlStateManager.enableTexture2D();
                }

                if (this.underlineStyle) {
                    tessellator = Tessellator.getInstance();
                    bufferBuilder = tessellator.getBuffer();
                    GlStateManager.disableTexture2D();
                    bufferBuilder.begin(7, DefaultVertexFormats.POSITION);
                    int l = this.underlineStyle ? -1 : 0;
                    bufferBuilder.pos(this.posX + (float)l, this.posY + (float)this.FONT_HEIGHT, 0.0).endVertex();
                    bufferBuilder.pos(this.posX + xOffset, this.posY + (float)this.FONT_HEIGHT, 0.0).endVertex();
                    bufferBuilder.pos(this.posX + xOffset, this.posY + (float)this.FONT_HEIGHT - 1.0F, 0.0).endVertex();
                    bufferBuilder.pos(this.posX + (float)l, this.posY + (float)this.FONT_HEIGHT - 1.0F, 0.0).endVertex();
                    tessellator.draw();
                    GlStateManager.enableTexture2D();
                }
            }
        }
    }
}
