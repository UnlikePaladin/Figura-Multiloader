package org.figuramc.figura.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.ClickEvent;
import org.figuramc.figura.utils.MathUtils;
import org.figuramc.figura.utils.TextUtils;
import org.figuramc.figura.utils.ui.UIHelper;

import java.util.List;

public class Label implements FiguraWidget, FiguraGuiEventListener {

    // text
    private final FontRenderer font;
    private ITextComponent rawText;
    private List<ITextComponent> formattedText;
    public TextUtils.Alignment alignment;
    public Integer outlineColor;
    public Integer backgroundColor;
    private Integer alpha;
    private int alphaPrecise = 0xFF;
    public int maxWidth;
    public boolean wrap;

    private Style hovered;

    // widget
    private int x, y;
    private int width, height;
    private float scale;
    private boolean visible = true;
    public boolean centerVertically;

    public Label(Object text, int x, int y, float scale, int maxWidth, boolean wrap, TextUtils.Alignment alignment, Integer outlineColor) {
        this.font = Minecraft.getMinecraft().fontRenderer;
        this.rawText = text instanceof ITextComponent ? (ITextComponent) text : new TextComponentString(String.valueOf(text));
        this.x = x;
        this.y = y;
        this.scale = scale;
        this.maxWidth = maxWidth;
        this.wrap = wrap;
        this.alignment = alignment;
        this.outlineColor = outlineColor;
        updateText();
    }

    public Label(Object text, int x, int y, int outlineColor) {
        this(text, x, y, 1f, -1, false, TextUtils.Alignment.LEFT, outlineColor);
    }

    public Label(Object text, int x, int y, TextUtils.Alignment alignment) {
        this(text, x, y, 1f, -1, false, alignment, null);
    }

    public Label(Object text, int x, int y, TextUtils.Alignment alignment, int outlineColor) {
        this(text, x, y, 1f, -1, false, alignment, outlineColor);
    }

    public Label(Object text, int x, int y, int maxWidth, boolean wrap, TextUtils.Alignment alignment) {
        this(text, x, y, 1f, maxWidth, wrap, alignment, null);
    }

    @Override
    public void draw(Minecraft minecraft, int mouseX, int mouseY, float delta) {
        hovered = null;

        if (!isVisible())
            return;

        renderBackground(minecraft);
        renderText(minecraft, mouseX, mouseY, delta);
    }

    private void renderBackground(Minecraft mc) {
        if (backgroundColor == null)
            return;

        int x = getX();
        int y = getY();

        UIHelper.fill(x, y, x + width, y + height, backgroundColor);
    }

    private void renderText(Minecraft mc, int mouseX, int mouseY, float delta) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(this.x, getY(), 0);
        GlStateManager.scale(scale, scale, scale);

        // alpha
        if (alpha != null) {
            float lerpDelta = MathUtils.magicDelta(0.6f, delta);
            alphaPrecise = (int) MathUtils.lerp(lerpDelta, alphaPrecise, mouseOver(mouseX, mouseY) ? 0xFF : alpha);
        }

        // prepare pos
        int y = 0;
        int height = font.FONT_HEIGHT;

        for (ITextComponent text : formattedText) {
            // dimensions
            int x = -alignment.apply(font, text);
            int width = font.getStringWidth(text.getFormattedText());

            // hovered
            if (mouseX >= this.x + x * scale && mouseX < this.x + (x + width) * scale && mouseY >= this.y + y * scale && mouseY < this.y + (y + height) * scale) {
                // get style at the mouse pos
                int pos = (int) ((mouseX - this.x - x * scale) / scale);
                hovered = UIHelper.getClickedComponentAt(text, this.width, pos).getStyle();

                // add underline for the text with the click event
                ClickEvent event = hovered != null ? hovered.getClickEvent() : null;
                if (event != null)
                    text = TextUtils.replaceStyle(text, new Style().setUnderlined(true), style -> event.equals(style.getClickEvent()));
                    // text = TextUtils.setStyleAtWidth(text, pos, font, Style.EMPTY.withUnderlined(true));

                // set tooltip for hovered text, if any
                UIHelper.setTooltip(hovered);
            }

            // render text
            if (outlineColor != null) {
                UIHelper.renderOutlineText(font, text, x, y, 0xFFFFFF, outlineColor);
            } else {
                font.drawStringWithShadow(text.getFormattedText(), x, y, 0xFFFFFF + (alphaPrecise << 24));
            }

            y += height;
        }

        GlStateManager.popMatrix();
    }

    @Override
    public boolean mouseButtonClicked(int mouseX, int mouseY, int button) {
        if (hovered != null && Minecraft.getMinecraft().currentScreen != null) {
            Minecraft.getMinecraft().currentScreen.handleComponentClick(new TextComponentString("").setStyle(hovered));
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseOver(double mouseX, double mouseY) {
        if (!isVisible())
            return false;

        int x = getX();
        int y = getY();

        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height)
            return true;
        return FiguraGuiEventListener.super.mouseOver(mouseX, mouseY);
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public void setScale(float scale) {
        this.scale = scale;
        updateText();
    }

    public void setText(ITextComponent text) {
        this.rawText = text;
        updateText();
    }

    private void updateText() {
        this.formattedText = TextUtils.formatInBounds(rawText, font, (int) (maxWidth / scale), wrap);
        this.width = (int) (TextUtils.getWidth(formattedText, font) * scale);
        this.height = (int) (font.FONT_HEIGHT * formattedText.size() * scale);
    }

    @Override
    public int getX() {
        int x = this.x;

        if (alignment == TextUtils.Alignment.RIGHT)
            x -= width;
        else if (alignment == TextUtils.Alignment.CENTER)
            x -= width / 2;

        return x;
    }

    public int getRawX() {
        return x;
    }

    @Override
    public void setX(int x) {
        this.x = x;
    }

    @Override
    public int getY() {
        int y = this.y;

        if (centerVertically)
            y -= height / 2;

        return y;
    }

    public int getRawY() {
        return y;
    }

    @Override
    public void setY(int y) {
        this.y = y;
    }

    @Override
    public void setWidth(int width) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setHeight(int height) {
        throw new UnsupportedOperationException();
    }

    public void setAlpha(int alpha) {
        this.alpha = this.alphaPrecise = alpha;
    }
}
