package org.figuramc.figura.mixin.gui;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiChat.class)
public interface GuiChatAccessor {
    @Intrinsic
    @Accessor("inputField")
    GuiTextField getInput();
}
