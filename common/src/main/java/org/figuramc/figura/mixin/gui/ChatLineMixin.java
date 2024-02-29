package org.figuramc.figura.mixin.gui;

import net.minecraft.client.gui.ChatLine;
import org.figuramc.figura.ducks.ChatLineAccessor;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ChatLine.class)
public class ChatLineMixin implements ChatLineAccessor {

    @Unique private int color = 0;

    @Override @Intrinsic
    public void figura$setColor(int color) {
        this.color = color;
    }

    @Override @Intrinsic
    public int figura$getColor() {
        return color;
    }
}
