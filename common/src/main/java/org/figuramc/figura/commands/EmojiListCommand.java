package org.figuramc.figura.commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.figuramc.figura.font.EmojiContainer;
import org.figuramc.figura.font.EmojiUnicodeLookup;
import org.figuramc.figura.font.Emojis;
import org.figuramc.figura.utils.ColorUtils;
import org.figuramc.figura.utils.FiguraClientCommandSource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

class EmojiListCommand {
    private static final ITextComponent COMMA_SPACE = new TextComponentString(", ").setStyle(new Style().setColor(TextFormatting.GRAY));

    public static class EmojiListSubCommand extends FiguraCommands.FiguraSubCommand {

        public EmojiListSubCommand() {
            super("emojis");
        }

        @Override
        public void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] args) throws CommandException {
            if (args.length > 0)
                listCategory(iCommandSender, args[0]);
        }

        @Override
        public List<String> getTabCompletions(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] strings, @Nullable BlockPos targetPos) {
            List<String> suggestions = new ArrayList<>(Emojis.getCategoryNames());
            suggestions.add("all");
            return CommandBase.getListOfStringsMatchingLastWord(strings, suggestions);
        }
    }

    private static int listCategory(ICommandSender context, String category) {
        FiguraClientCommandSource src = (FiguraClientCommandSource) context;
        if (Objects.equals(category, "all")) {
            for (String curCategory : Emojis.getCategoryNames()) {
                if (!printEmojis(curCategory, src::figura$sendFeedback, src::figura$sendError)) {
                    return 0;
                }
                src.figura$sendFeedback(new TextComponentString(""));
            }

            return 1;
        }
        return printEmojis(category, src::figura$sendFeedback, src::figura$sendError) ? 1 : 0;
    }


    private static boolean printEmojis(String category, Consumer<ITextComponent> feedback, Consumer<ITextComponent> error) {
        if (!Emojis.hasCategory(category)) {
            error.accept(new TextComponentString("Emoji category \"" + category + "\" doesn't exist!"));
            return false;
        }

        EmojiContainer container = Emojis.getCategory(category);
        Collection<String> unicodeValues = container.getLookup().unicodeValues();
        EmojiUnicodeLookup lookup = container.getLookup();

        // give the category a title
        feedback.accept(new TextComponentString(String.format("--- %s (%s) ---", container.name, unicodeValues.size())).setStyle(ColorUtils.Colors.AWESOME_BLUE.style));


        // Gather each emoji name and append it into a single message
        TextComponentString comp = new TextComponentString("");
        unicodeValues.stream().sorted().forEach(unicode -> {
            String[] aliases = lookup.getNames(unicode);
            if (aliases != null) {
                TextComponentString msg = new TextComponentString("");
                for (int i = 0; i < aliases.length; i++) {
                    msg.appendSibling(new TextComponentString(aliases[i]).setStyle(ColorUtils.Colors.AWESOME_BLUE.style));
                    if (i < aliases.length - 1) {
                        msg.appendSibling(COMMA_SPACE);
                    }
                }
                msg.appendSibling(new TextComponentString("\ncodepoint: " + unicode.codePointAt(0)).setStyle(new Style().setColor(TextFormatting.GRAY)));
                comp.appendSibling(Emojis.getEmoji(aliases[0], msg));
            }
        });

        feedback.accept(comp);

        return true;
    }
}
