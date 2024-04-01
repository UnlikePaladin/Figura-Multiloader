package org.figuramc.figura.gui.widgets;

import net.minecraft.client.Minecraft;

public interface FiguraRenderable {
    void draw(Minecraft minecraft, int mouseX, int mouseY, float delta);
}
