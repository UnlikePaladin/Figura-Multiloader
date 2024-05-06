package org.figuramc.figura.mixin.font;

import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FontRenderer.class)

public interface FontRendererAccessor {
    @Accessor("colorCode")
    @Final
    int[] getColors();

    @Accessor("red")
    void setRed(float r);

    @Accessor("green")
    void setGreen(float g);

    @Accessor("blue")
    void setBlue(float b);

    @Accessor("alpha")
    void setAlpha(float r);

    @Accessor("red")
    float red();

    @Accessor("blue")
    float blue();

    @Accessor("green")
    float green();

    @Accessor("alpha")
    float alpha();

    @Accessor("posX")
    void setPosX(float x);

    @Accessor("posY")
    void setPosY(float y);
}
