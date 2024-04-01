package org.figuramc.figura.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import org.figuramc.figura.utils.FiguraIdentifier;
import org.figuramc.figura.utils.MathUtils;
import org.figuramc.figura.utils.ui.UIHelper;
import org.lwjgl.input.Keyboard;

public class ScrollBarWidget extends AbstractFiguraWidget implements FiguraWidget {

    // -- fields -- //

    public static final ResourceLocation SCROLLBAR_TEXTURE = new FiguraIdentifier("textures/gui/scrollbar.png");

    protected final int headHeight = 20;
    protected final int headWidth = 10;

    protected boolean isScrolling = false;
    protected boolean vertical = true;

    protected double scrollPos;
    protected double scrollPrecise;
    protected double scrollRatio = 1d;

    protected OnPress onPress;

    // -- constructors -- //

    public ScrollBarWidget(int x, int y, int width, int height, double initialValue) {
        super(x, y, width, height, new TextComponentString(""));
        scrollPrecise = initialValue;
        scrollPos = initialValue;
    }

    // -- methods -- //


    @Override
    public boolean mouseButtonClicked(int mouseX, int mouseY, int button) {
        if (!this.isActive() || !(this.isHovered()) || !this.mouseOver(mouseX, mouseY))
            return false;

        if (button == 0) {
            // jump to pos when not clicking on head
            double scrollPos = MathUtils.lerp(scrollPrecise, 0d, (vertical ? getHeight() - headHeight : getWidth() - headWidth) + 2d);

            if (vertical && mouseY < y + scrollPos || mouseY > y + scrollPos + headHeight)
                scroll(-(y + scrollPos + headHeight / 2d - mouseY));
            else if (!vertical && mouseX < x + scrollPos || mouseX > x + scrollPos + headWidth)
                scroll(-(x + scrollPos + headWidth / 2d - mouseX));

            isScrolling = true;
            playPressedSound(Minecraft.getMinecraft().getSoundHandler());
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseButtonReleased(int mouseX, int mouseY, int button) {
        if (button == 0 && isScrolling) {
            isScrolling = false;
            return true;
        }

        return false;
    }

    @Override
    public void mouseDragged(Minecraft minecraft, int mouseX, int mouseY, int button, double deltaX, double deltaY) {
        if (isScrolling) {
            // vertical drag
            if (vertical) {
                if (Math.signum(deltaY) == -1) {
                    if (mouseY <= this.getY() + this.getHeight()) {
                        scroll(deltaY);
                        return;
                    }

                } else if (mouseY >= this.y) {
                    scroll(deltaY);
                    return;
                }
            }
            // horizontal drag
            else if (Math.signum(deltaX) == -1) {
                if (mouseX <= this.getX() + this.getWidth()) {
                    scroll(deltaX);
                    return;
                }
            } else if (mouseX >= this.x) {
                scroll(deltaX);
                return;
            }
        }

        super.mouseDragged(minecraft, mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScroll(double mouseX, double mouseY, double amount) {
        if (!this.isActive()) return false;
        scroll(-amount * (vertical ? getHeight() : getWidth()) * 0.05d * scrollRatio);
        return true;
    }

    @Override
    public boolean pressedKey(char keyCode, int scanCode) {
        if (!this.isActive()) return false;
        switch (scanCode) {
            case Keyboard.KEY_DOWN:
            case Keyboard.KEY_RIGHT: {
                scroll(1 * (vertical ? getHeight() : getWidth()) * 0.05d * scrollRatio);
                return true;
            }
            case Keyboard.KEY_LEFT:
            case Keyboard.KEY_UP: {
                scroll(-1 * (vertical ? getHeight() : getWidth()) * 0.05d * scrollRatio);
                return true;
            }
        }
        return super.pressedKey(keyCode, scanCode);
    }

    @Override
    public boolean mouseOver(double mouseX, double mouseY) {
        return UIHelper.isMouseOver(getX(), getY(), getWidth(), getHeight(), mouseX, mouseY);
    }

    // apply scroll value
    protected void scroll(double amount) {
        scrollPrecise += amount / ((vertical ? getHeight() - headHeight : getWidth() - headWidth) + 2d);
        setScrollProgress(scrollPrecise);
    }

    // animate scroll head
    protected void lerpPos(float delta) {
        float lerpDelta = MathUtils.magicDelta(0.2f, delta);
        scrollPos = MathUtils.lerp(lerpDelta, scrollPos, getScrollProgress());
    }

    @Override
    public void draw(Minecraft minecraft, int mouseX, int mouseY, float delta) {
        if (!isVisible())
            return;

        isHovered = this.mouseOver(mouseX, mouseY);
        drawWidget(minecraft, mouseX, mouseY, delta);
    }

    // render the scroll
    @Override
    public void drawWidget(Minecraft mc, int mouseX, int mouseY, float delta) {
        UIHelper.setupTexture(SCROLLBAR_TEXTURE);
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();

        // render bar
        UIHelper.blit(x, y, width, 1, 10f, isScrolling ? 20f : 0f, 10, 1, 20, 40);
        UIHelper.blit(x, y + 1, width, height - 2, 10f, isScrolling ? 21f : 1f, 10, 18, 20, 40);
        UIHelper.blit(x, y + height - 1, width, 1, 10f, isScrolling ? 39f : 19f, 10, 1, 20, 40);

        // render head
        lerpPos(delta);
        UIHelper.blit(x, (int) (y + Math.round(MathUtils.lerp(scrollPos, 0, height - headHeight))), 0f, this.isHovered || isScrolling ? headHeight : 0f, headWidth, headHeight, 20, 40);
    }

    // -- getters and setters -- //

    @Override
    public boolean isVisible() {
        return this.visible;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public int getX() {
        return this.x;
    }

    @Override
    public void setX(int x) {
        this.x = x;
    }

    @Override
    public int getY() {
        return this.y;
    }

    @Override
    public void setY(int y) {
        this.y = y;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    // set scrollbar height
    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // get scroll value
    public double getScrollProgress() {
        return scrollPrecise;
    }

    // manually set scroll
    public void setScrollProgress(double amount) {
        setScrollProgress(amount, false);
    }

    public void setScrollProgressNoAnim(double amount) {
        setScrollProgress(amount, false);
        scrollPos = scrollPrecise;
    }

    // manually set scroll with optional clamping
    public void setScrollProgress(double amount, boolean force) {
        amount = Double.isNaN(amount) ? 0 : amount;
        scrollPrecise = force ? amount : MathHelper.clamp(amount, 0d, 1d);

        if (onPress != null)
            onPress.onPress(this);
    }

    // set button action
    public void setAction(OnPress onPress) {
        this.onPress = onPress;
    }

    // set scroll ratio
    public void setScrollRatio(double entryHeight, double heightDiff) {
        scrollRatio = (getHeight() + entryHeight) / (heightDiff / 2d);
    }

    // press action
    public interface OnPress {
        void onPress(ScrollBarWidget scrollbar);
    }
}
