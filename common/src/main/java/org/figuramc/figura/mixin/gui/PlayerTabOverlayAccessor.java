package org.figuramc.figura.mixin.gui;

import com.google.common.collect.Ordering;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiPlayerTabOverlay.class)
public interface PlayerTabOverlayAccessor {

    @Intrinsic
    @Accessor("header")
    ITextComponent getHeader();

    @Intrinsic
    @Accessor("footer")
    ITextComponent getFooter();

    @Intrinsic
    @Accessor("ENTRY_ORDERING")
    static Ordering<NetworkPlayerInfo> getPlayerOrdering() {
        throw new AssertionError();
    }
}
