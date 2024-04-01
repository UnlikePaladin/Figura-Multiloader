package org.figuramc.figura.ducks.extensions;

import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.util.text.ITextComponent;

public interface FontExtension {
    public void figura$drawInBatch8xOutline(ITextComponent text, float x, float y, int color, int outlineColor);
}
