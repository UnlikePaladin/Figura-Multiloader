package org.figuramc.figura.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.figuramc.figura.utils.FiguraIdentifier;
import org.figuramc.figura.utils.MathUtils;
import org.figuramc.figura.utils.ui.UIHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class SliderWidget extends ScrollBarWidget {

    // -- fields -- //

    public static final ResourceLocation SLIDER_TEXTURE = new FiguraIdentifier("textures/gui/slider.png");

    protected final int headHeight = 11;
    protected final int headWidth = 11;
    protected final boolean showSteps;

    private int max;
    private double stepSize;
    private double steppedPos;

    // -- constructors -- //

    public SliderWidget(int x, int y, int width, int height, double initialValue, int maxValue, boolean showSteps) {
        super(x, y, width, height, initialValue);
        this.vertical = false;
        this.showSteps = showSteps;
        this.steppedPos = initialValue;
        setMax(maxValue);
    }

    // -- methods -- //


    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!this.isActive()) return false;
        scroll(stepSize * Math.signum(-amount) * (getWidth() - headWidth + 2d));
        return true;
    }

    @Override
    public boolean pressedKey(char keyCode, int scanCode) {
        if (!this.isActive()) return false;

        int modifiers = 0;
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            modifiers = modifiers | 0x0001;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
            modifiers = modifiers | 0x0002;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)) {
            modifiers = modifiers | 0x0004;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)) {
            modifiers = modifiers | 0x0004;
        }

        switch (scanCode) {
            case Keyboard.KEY_DOWN:
            case Keyboard.KEY_RIGHT: {
                scroll(stepSize * 1 * Math.max(modifiers * 10, 1) * (getWidth() - headWidth + 2d));
                return true;
            }
            case Keyboard.KEY_LEFT:
            case Keyboard.KEY_UP: {
                scroll(stepSize * -1 * Math.max(modifiers * 10, 1) * (getWidth() - headWidth + 2d));
                return true;
            }
        }

        return super.pressedKey(keyCode, scanCode);
    }

    @Override
    protected void scroll(double amount) {
        // normal scroll
        super.scroll(amount);

        // get the closest step
        steppedPos = getClosestStep();
    }

    private double getClosestStep() {
        // get closer steps
        double lowest = scrollPrecise - scrollPrecise % stepSize;
        double highest = lowest + stepSize;

        // get distance
        double distanceLow = Math.abs(lowest - scrollPrecise);
        double distanceHigh = Math.abs(highest - scrollPrecise);

        // return closest
        return distanceLow < distanceHigh ? lowest : highest;
    }

    @Override
    public void draw(Minecraft minecraft, int mouseX, int mouseY, float delta) {
        if (this.isVisible()) {
            // set hovered
            this.isHovered = this.mouseOver(mouseX, mouseY);

            // render button
            this.drawWidget(minecraft, mouseX, mouseY, delta);
        }
    }

    @Override
    public void drawWidget(Minecraft mc, int mouseX, int mouseY, float delta) {
        UIHelper.setupTexture(SLIDER_TEXTURE);
        int x = getX();
        int y = getY();
        int width = getWidth();

        // draw bar
        UIHelper.blit(x, y + 3, width, 5, isScrolling ? 10f : 0f, 0f, 5, 5, 33, 16);

        // draw steps
        if (showSteps) {
            for (int i = 0; i < max; i++) {
                UIHelper.blit((int) Math.floor(x + 3 + stepSize * i * (width - 11)), y + 3, 5, 5, isScrolling ? 15f : 5f, 0f, 5, 5, 33, 16);
            }
        }

        // draw header
        lerpPos(delta);
        UIHelper.blit((int) (x + Math.round(MathUtils.lerp(scrollPos, 0, width - headWidth))), y, isActive() ? ((this.isHovered() || this.isFocused()) || isScrolling ? headWidth * 2 : headWidth) : 0f, 5f, headWidth, headHeight, 33, 16);
    }

    // -- getters and setters -- //

    @Override
    public double getScrollProgress() {
        return steppedPos;
    }

    @Override
    public void setScrollProgress(double amount, boolean force) {
        steppedPos = force ? amount : MathHelper.clamp(amount, 0d, 1d);
        super.setScrollProgress(amount, force);
    }

    public int getMax() {
        return max;
    }

    public void setMax(int maxValue) {
        // set steps data
        this.max = maxValue;
        this.stepSize = 1d / (maxValue - 1);
    }

    public int getIntValue() {
        return (int) Math.round(getScrollProgress() * (getMax() - 1));
    }
}
