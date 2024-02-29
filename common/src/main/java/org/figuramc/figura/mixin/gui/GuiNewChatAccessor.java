package org.figuramc.figura.mixin.gui;

import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(GuiNewChat.class)
public interface GuiNewChatAccessor {

    @Intrinsic
    @Accessor("chatLines")
    List<ChatLine> getAllMessages();
}
