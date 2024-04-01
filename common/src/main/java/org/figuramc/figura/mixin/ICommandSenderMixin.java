package org.figuramc.figura.mixin;

import com.mojang.text2speech.Narrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.figuramc.figura.utils.FiguraClientCommandSource;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ICommandSender.class)
abstract class ICommandSenderMixin implements FiguraClientCommandSource {


    @Override
    public void figura$sendFeedback(ITextComponent message) {
        Minecraft.getMinecraft().ingameGUI.getChatGUI().addToSentMessages(message.getFormattedText());
        Narrator.getNarrator().say(message.getUnformattedText());
    }

    @Override
    public void figura$sendError(ITextComponent message) {
        figura$sendFeedback(new TextComponentString("").appendSibling(message).setStyle(new Style().setColor(TextFormatting.RED)));
    }

    @Override
    public Minecraft figura$getClient() {
        return Minecraft.getMinecraft();
    }

    @Override
    public EntityPlayerSP figura$getPlayer() {
        return Minecraft.getMinecraft().player;
    }

    @Override
    public WorldClient figura$getWorld() {
        return Minecraft.getMinecraft().world;
    }
}