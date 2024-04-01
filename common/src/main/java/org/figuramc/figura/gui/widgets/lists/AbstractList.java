package org.figuramc.figura.gui.widgets.lists;

import net.minecraft.client.Minecraft;
import org.figuramc.figura.gui.widgets.*;
import org.figuramc.figura.utils.ui.UIHelper;

import java.util.Collections;
import java.util.List;

public abstract class AbstractList extends AbstractContainerElement {

    protected final ScrollBarWidget scrollBar;

    public int scissorsX, scissorsY;
    public int scissorsWidth, scissorsHeight;

    public AbstractList(int x, int y, int width, int height) {
        super(x, y, width, height);

        updateScissors(1, 1, -2, -2);

        children.add(scrollBar = new ScrollBarWidget(x + width - 14, y + 4, 10, height - 8, 0d));
        scrollBar.setVisible(false);
    }

    public void updateScissors(int xOffset, int yOffset, int endXOffset, int endYOffset) {
        this.scissorsX = xOffset;
        this.scissorsY = yOffset;
        this.scissorsWidth = endXOffset;
        this.scissorsHeight = endYOffset;
    }

    public boolean isInsideScissors(double mouseX, double mouseY) {
        return UIHelper.isMouseOver(getX() + scissorsX, getY() + scissorsY, getWidth() + scissorsWidth, getHeight() + scissorsHeight, mouseX, mouseY);
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY, float delta) {
        for (FiguraGuiEventListener child : children) {
            if (child instanceof AbstractFiguraWidget && !contents().contains(child)) {
                AbstractFiguraWidget widget = (AbstractFiguraWidget) child;
                widget.draw(mc, mouseX, mouseY, delta);
            } else if (child instanceof FiguraRenderable && !contents().contains(child)) {
                FiguraRenderable widget = (FiguraRenderable) child;
                widget.draw(mc, mouseX, mouseY, delta);
            }
        }
    }

    @Override
    public boolean mouseScroll(double mouseX, double mouseY, double amount) {
        return scrollBar.mouseScroll(mouseX, mouseY, amount) || super.mouseScroll(mouseX, mouseY, amount);
    }

    public List<? extends FiguraGuiEventListener> contents() {
        return Collections.emptyList();
    }
}