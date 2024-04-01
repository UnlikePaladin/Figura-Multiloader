package org.figuramc.figura.gui.widgets;

import net.minecraft.client.Minecraft;

public interface FiguraGuiEventListener {
    default boolean mouseButtonClicked(int mouseX, int mouseY, int mouseButton) {
        return false;
    }
    default boolean mouseOver(double mouseX, double mouseY) {
        return false;
    }
    default boolean mouseButtonReleased(int mouseX, int mouseY, int mouseButton) {
        return false;
    }
    default void mouseDragged(Minecraft minecraft, int mouseX, int mouseY, int button, double dragX, double dragY) {
    }
    default boolean pressedKey(char keyCode, int scanCode) {
        return false;
    }
    default boolean mouseScroll(double mouseX, double mouseY, double offset) {
        return false;
    }
    default boolean focusChange(boolean focused) {
        return false;
    }
}
