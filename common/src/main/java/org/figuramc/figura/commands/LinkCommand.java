package org.figuramc.figura.commands;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.utils.ColorUtils;
import org.figuramc.figura.utils.FiguraText;

import java.util.ArrayList;
import java.util.List;

class LinkCommand {

    private static final List<FiguraMod.Links> LINKS = new ArrayList<FiguraMod.Links>() {{
            add(FiguraMod.Links.Wiki);
            add(FiguraMod.Links.Kofi);
            add(FiguraMod.Links.OpenCollective);
            add(null);
            add(FiguraMod.Links.Discord);
            add(FiguraMod.Links.Github);
            add(null);
            add(FiguraMod.Links.Modrinth);
            add(FiguraMod.Links.Curseforge);
    }};

    public static class LinkSubCommand extends FiguraCommands.FiguraSubCommand {
        // get links
        public LinkSubCommand() {
            super("links");
        }

        @Override
        public void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] args) throws CommandException {
            // header
            ITextComponent message = new TextComponentString("").setStyle(ColorUtils.Colors.AWESOME_BLUE.style)
                    .appendSibling(new TextComponentString("•*+•* ")
                            .appendSibling(new FiguraText())
                            .appendText(" Links *•+*•").setStyle(new Style().setUnderlined(true)))
                    .appendText("\n");

            // add links
            for (FiguraMod.Links link : LINKS) {
                message.appendText("\n");

                if (link == null)
                    continue;

                message.appendSibling(new TextComponentString("• [" + link.name() + "]")
                        .setStyle(link.style)
                        .setStyle(new Style().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link.url)))
                        .setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(link.url)))));
            }

            FiguraMod.sendChatMessage(message);
        }
    }
}
