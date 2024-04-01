package org.figuramc.figura.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.figuramc.figura.utils.ui.UIHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public abstract class AbstractContainerElement extends Gui implements FiguraTickable, FiguraWidget, FiguraRenderable, FiguraGuiEventListener {

    public static final ITextComponent HOVERED_ARROW = new TextComponentString("•");

    protected final List<FiguraGuiEventListener> children = new ArrayList<>();
    private FiguraGuiEventListener focusedElement = null;
    private ITextComponent message;
    private int x, y;
    private int width, height;
    boolean hovered = false;
    private boolean visible = true;

    public AbstractContainerElement(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.message = new TextComponentString("");
    }

    @Override
    public void tick() {
        for (FiguraGuiEventListener listener : this.children) {
            if (listener instanceof FiguraTickable) {
                FiguraTickable tickable = (FiguraTickable) listener;
                tickable.tick();
            }
        }
    }

    @Override
    public void draw(Minecraft minecraft, int mouseX, int mouseY, float delta) {
        for (FiguraGuiEventListener listener : this.children) {
            if (listener instanceof FiguraRenderable) {
                ((FiguraRenderable) listener).draw(minecraft, mouseX, mouseY, delta);
            }
        }
    }

    @Override
    public boolean mouseButtonClicked(int mouseX, int mouseY, int mouseButton) {
        //fix mojang focusing for text fields
        for (FiguraGuiEventListener listener : this.children) {
            if (listener instanceof TextField) {
                TextField field = (TextField) listener;
                field.getField().setFocused(field.isEnabled() && field.mouseOver(mouseX, mouseY));
            }
        }
        for (FiguraGuiEventListener guiEventListener : this.children()) {
            if (!guiEventListener.mouseButtonClicked(mouseX, mouseY, mouseButton)) continue;
            this.setFocused(guiEventListener);
            return true;
        }
        return false;
    }

    void setFocused(FiguraGuiEventListener guiEventListener) {
        this.focusedElement = guiEventListener;
    }

    @Override
    public boolean mouseOver(double mouseX, double mouseY) {
        return UIHelper.isMouseOver(getX(), getY(), getWidth(), getHeight(), mouseX, mouseY);
    }

    @Override
    public void mouseDragged(Minecraft minecraft, int mouseX, int mouseY, int button, double dragX, double dragY) {
        // yeet mouse 0 and isDragging check
        if (this.getFocused() != null) {
            this.getFocused().mouseDragged(Minecraft.getMinecraft(), mouseX, mouseY, button, dragX, dragY);
        }
    }

    private FiguraGuiEventListener getFocused() {
        return focusedElement;
    }

    @Override
    public boolean mouseButtonReleased(int mouseX, int mouseY, int button) {
        // better check for mouse released when outside node's boundaries
        return this.getFocused() != null && this.getFocused().mouseButtonReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScroll(double mouseX, double mouseY, double offset) {
        // fix scrolling targeting only one child
        boolean ret = false;
        for (FiguraGuiEventListener child : this.children()) {
            if (child.mouseOver(mouseX, mouseY))
                ret = ret || child.mouseScroll(mouseX, mouseY, offset);
        }
        return ret;
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    public boolean isHovered() {
        return hovered;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;

        for (FiguraGuiEventListener listener : this.children) {
            if (listener instanceof FiguraWidget) {
                FiguraWidget drawable = (FiguraWidget) listener;
                drawable.setVisible(visible);
            } else if (listener instanceof AbstractFiguraWidget) {
                AbstractFiguraWidget widget = (AbstractFiguraWidget) listener;
                widget.visible = visible;
            }
        }
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setX(int x) {
        this.x = x;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public void setY(int y) {
        this.y = y;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public int getWidth() {
        return width;
    }

    public List<? extends FiguraGuiEventListener> children() {
        return children;
    }

    @Override
    public boolean pressedKey(char keyCode, int scanCode) {
        for (FiguraGuiEventListener widget : children) {
            if (widget.pressedKey(keyCode, scanCode))
                return true;
        }
        return false;
    }

    public Optional<FiguraGuiEventListener> findChildAt(double mouseX, double mouseY) {
        for (FiguraGuiEventListener guiEventListener : this.children()) {
            if (!guiEventListener.mouseOver(mouseX, mouseY)) continue;
            return Optional.of(guiEventListener);
        }
        return Optional.empty();
    }

    @Override
    public boolean focusChange(boolean focused) {
        FiguraGuiEventListener guiEventListener = this.getFocused();

        if (guiEventListener != null && guiEventListener.focusChange(focused)) {
            return true;
        }

        List<? extends FiguraGuiEventListener> children = this.children();
        int focusedIndx = children.indexOf(guiEventListener);
        int iteratorPos = guiEventListener != null && focusedIndx >= 0 ? focusedIndx + (focused ? 1 : 0) : (focused ? 0 : children.size());

        ListIterator<? extends FiguraGuiEventListener> listIterator = children.listIterator(iteratorPos);
        BooleanSupplier shouldContinue = focused ? listIterator::hasNext : listIterator::hasPrevious;
        Supplier<FiguraGuiEventListener> supplier = focused ? listIterator::next : listIterator::previous;

        while (shouldContinue.getAsBoolean()) {
            FiguraGuiEventListener listener = supplier.get();
            if (!listener.focusChange(focused)) continue;
            this.setFocused(listener);
            return true;
        }
        this.setFocused(null);
        return false;
    }
}