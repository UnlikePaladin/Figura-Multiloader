package org.figuramc.figura.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.figuramc.figura.utils.ClickableTextHelper;
import org.figuramc.figura.utils.VertexFormatMode;
import org.figuramc.figura.utils.ui.UIHelper;

import java.util.Objects;

public class BackendMotdWidget extends AbstractFiguraWidget implements FiguraRenderable, FiguraGuiEventListener {
    private final FontRenderer font;
    private final ClickableTextHelper textHelper;
    private int maxWidth;
    private double scrollAmount;
    private boolean scrolling;

    public BackendMotdWidget(int x, int y, int width, int height, ITextComponent text, FontRenderer font) {
        super(x, y, width, height, text);
        this.font = font;
        this.textHelper = new ClickableTextHelper();
        this.maxWidth = this.getWidth() - this.totalInnerPadding();
    }

    @Override
    public void setMessage(ITextComponent message) {
        super.setMessage(message);
        textHelper.setMessage(message);
    }

    public void setWidth(int value) {
        super.setWidth(value);
        int prevWidth = this.maxWidth;
        this.maxWidth = this.getWidth() - this.totalInnerPadding();
        if (maxWidth != prevWidth)
            textHelper.markDirty();
    }

    protected int totalInnerPadding() {
        return innerPadding() * 2;
    }

    protected int getInnerHeight() {
        Objects.requireNonNull(font);
        return textHelper.lineCount() * font.FONT_HEIGHT;
    }

    protected double scrollRate() {
        Objects.requireNonNull(this.font);
        return font.FONT_HEIGHT;
    }

    protected int innerPadding() {
        return 4;
    }

    protected void renderBorder(Minecraft mc, int x, int y, int width, int height) {
        UIHelper.renderSliced(this.x - this.innerPadding(), this.y - this.innerPadding(), this.getWidth() + this.totalInnerPadding(), this.getHeight() + this.totalInnerPadding(), UIHelper.OUTLINE_FILL);
    }

    protected void renderBackground() {
        UIHelper.renderSliced(this.x - this.innerPadding(), this.y - this.innerPadding(), this.getWidth() + this.totalInnerPadding(), this.getHeight() + this.totalInnerPadding(), UIHelper.OUTLINE_FILL);
    }

    @Override
    public void drawWidget(Minecraft minecraft, int mouseX, int mouseY, float delta) {
        if (this.visible) {
            if (!scrollbarVisible()) {
                renderBackground();
                renderContents(minecraft, mouseX, mouseY, delta);
            } else {
                super.drawWidget(minecraft, mouseX, mouseY, delta);
            }
        }
    }

    protected void renderContents(Minecraft mc, int mouseX, int mouseY, float delta) {
        int xx = this.x + this.innerPadding();
        int yy = this.y + this.innerPadding();

        int scroll = (int)scrollAmount();
        textHelper.update(font, maxWidth);

        textHelper.visit((text, style, x, y, textWidth, textHeight) -> drawString(font, new TextComponentString(text).setStyle(style).getFormattedText(), xx + x, yy + y, 0xFFFFFFFF));

        //textHelper.renderDebug(graphics, xx, yy, mouseX, mouseY + scroll);

        if (withinContentAreaPoint(mouseX, mouseY)) {
            ITextComponent tooltip = textHelper.getHoverTooltip(xx, yy, mouseX, mouseY + scroll);
            if (tooltip != null)
                UIHelper.setTooltip(tooltip);

            if (mouseDown) {
                String link = textHelper.getClickLink(xx, yy, mouseX, mouseY + scroll);
                if (link != null)
                    UIHelper.openURL(link).run();

                mouseDown = false;
            }
        }
    }

    public void playPressedSound(SoundHandler handler) {
        // Don't play the button click sound
    }

    private boolean mouseDown = false;

    @Override
    public boolean mouseButtonClicked(int mouseX, int mouseY, int button) {
        mouseDown = mouseClickedScroll(mouseX, mouseY, button);
        return mouseDown;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    protected boolean scrollbarVisible() {
        return this.getInnerHeight() > this.getHeight();
    }

    public boolean shouldRender() {
        return getScrollBarHeight() > 0 && this.height >= 48;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    protected double scrollAmount() {
        return this.scrollAmount;
    }

    protected void setScrollAmount(double scrollAmount) {
        this.scrollAmount = MathHelper.clamp(scrollAmount, 0.0, (double)this.getMaxScrollAmount());
    }

    protected int getMaxScrollAmount() {
        return Math.max(0, this.getContentHeight() - (this.height - 4));
    }

    private int getContentHeight() {
        return 4;
    }

    private void renderScrollBar() {
        int i = this.getScrollBarHeight();
        int j = this.x + this.width;
        int k = this.x + this.width + 8;
        int l = Math.max(this.y, (int)this.scrollAmount * (this.height - i) / this.getMaxScrollAmount() + this.y);
        int m = l + i;
        Tessellator tesselator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuffer();
        bufferBuilder.begin(VertexFormatMode.QUADS.asGLMode, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(j, m, 0.0).color(128, 128, 128, 255).endVertex();
        bufferBuilder.pos(k, m, 0.0).color(128, 128, 128, 255).endVertex();
        bufferBuilder.pos(k, l, 0.0).color(128, 128, 128, 255).endVertex();
        bufferBuilder.pos(j, l, 0.0).color(128, 128, 128, 255).endVertex();
        bufferBuilder.pos(j, (m - 1), 0.0).color(192, 192, 192, 255).endVertex();
        bufferBuilder.pos((k - 1), (m - 1), 0.0).color(192, 192, 192, 255).endVertex();
        bufferBuilder.pos((k - 1), l, 0.0).color(192, 192, 192, 255).endVertex();
        bufferBuilder.pos(j, l, 0.0).color(192, 192, 192, 255).endVertex();
        tesselator.draw();
    }
    private int getScrollBarHeight() {
        return MathHelper.clamp((int)((float)(this.height * this.height) / (float)this.getContentHeight()), 32, this.height);
    }

    protected void renderDecorations(Minecraft mc) {
        if (this.scrollbarVisible()) {
            this.renderScrollBar();
        }
    }

    protected boolean withinContentAreaTopBottom(int top, int bottom) {
        return (double)bottom - this.scrollAmount >= (double)this.y && (double)top - this.scrollAmount <= (double)(this.y + this.height);
    }

    protected boolean withinContentAreaPoint(double x, double y) {
        return x >= (double)this.x && x < (double)(this.x + this.width) && y >= (double)this.y && y < (double)(this.y + this.height);
    }

    public boolean mouseClickedScroll(double mouseX, double mouseY, int button) {
        if (!this.visible) {
            return false;
        } else {
            boolean bl = this.withinContentAreaPoint(mouseX, mouseY);
            boolean bl2 = this.scrollbarVisible() && mouseX >= (double)(this.x + this.width) && mouseX <= (double)(this.x + this.width + 8) && mouseY >= (double)this.y && mouseY < (double)(this.y + this.height);
            this.setFocused(bl || bl2);
            if (bl2 && button == 0) {
                this.scrolling = true;
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean mouseButtonReleased(int mouseX, int mouseY, int button) {
        if (button == 0) {
            this.scrolling = false;
        }
        return super.mouseButtonReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseDragged(Minecraft mc, int mouseX, int mouseY, int button, double deltaX, double deltaY) {
        if (this.visible && this.isFocused() && this.scrolling) {
            if (mouseY < (double)this.y) {
                this.setScrollAmount(0.0);
            } else if (mouseY > (double)(this.y + this.height)) {
                this.setScrollAmount((double)this.getMaxScrollAmount());
            } else {
                int i = this.getScrollBarHeight();
                double d = (double)Math.max(1, this.getMaxScrollAmount() / (this.height - i));
                this.setScrollAmount(this.scrollAmount + deltaY * d);
            }

        }
    }

    @Override
    public boolean mouseScroll(double mouseX, double mouseY, double amount) {
        if (this.visible && this.isFocused()) {
            this.setScrollAmount(this.scrollAmount - amount * this.scrollRate());
            return true;
        } else {
            return false;
        }
    }
}
