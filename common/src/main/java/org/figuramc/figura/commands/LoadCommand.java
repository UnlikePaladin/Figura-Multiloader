package org.figuramc.figura.commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.local.LocalAvatarFetcher;
import org.figuramc.figura.utils.FiguraClientCommandSource;
import org.figuramc.figura.utils.FiguraText;

import java.nio.file.Path;
import java.nio.file.Paths;

class LoadCommand {

    public static class LoadSubCommand extends FiguraCommands.FiguraSubCommand {

        public LoadSubCommand() {
            super("load");
        }

        @Override
        public void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] args) throws CommandException {
            loadAvatar(iCommandSender, CommandBase.buildString(args, 0));
        }
    }

    private static int loadAvatar(ICommandSender context, String str) {
        try {
            // parse path
            Path p = LocalAvatarFetcher.getLocalAvatarDirectory().resolve(Paths.get(str));

            // try to load avatar
            AvatarManager.loadLocalAvatar(p);
            ((FiguraClientCommandSource)context).figura$sendFeedback(new FiguraText("command.load.loading"));
            return 1;
        } catch (Exception e) {
            ((FiguraClientCommandSource)context).figura$sendError(new FiguraText("command.load.invalid", str));
        }

        return 0;
    }
}
