package org.figuramc.figura.backend2;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.commands.FiguraCommands;
import org.figuramc.figura.resources.FiguraRuntimeResources;
import org.figuramc.figura.utils.FiguraClientCommandSource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BackendCommands {
    public static class BackendSubCommand extends FiguraCommands.FiguraSubCommand {
        public Map<String, FiguraCommands.FiguraSubCommand> subCommandMap = new HashMap<>();
        public BackendSubCommand() {
            super("backend2");
            subCommandMap.put("connect", new FiguraCommands.FiguraSubCommand("connect") {
                @Override
                public void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] args) throws CommandException {
                    NetworkStuff.reAuth();
                }
            });

            subCommandMap.put("run", new FiguraCommands.FiguraSubCommand("run") {
                @Override
                public void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] args) throws CommandException {
                    if (args.length == 0) {
                        runRequest(iCommandSender, "");
                    }
                    runRequest(iCommandSender, CommandBase.buildString(args, 0));
                }
            });

            subCommandMap.put("debug", new FiguraCommands.FiguraSubCommand("debug") {
                @Override
                public void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] args) throws CommandException {
                    NetworkStuff.debug = !NetworkStuff.debug;
                    FiguraMod.sendChatMessage(new TextComponentString("Backend Debug Mode set to: " + NetworkStuff.debug).setStyle(new Style().setColor(NetworkStuff.debug ? TextFormatting.GREEN : TextFormatting.RED)));
                }
            });

            subCommandMap.put("checkResources", new FiguraCommands.FiguraSubCommand("checkResources") {
                @Override
                public void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] args) throws CommandException {
                    ((FiguraClientCommandSource)iCommandSender).figura$sendFeedback(new TextComponentString("Checking for resources..."));
                    FiguraRuntimeResources.init().thenRun(() -> ((FiguraClientCommandSource)iCommandSender).figura$sendFeedback(new TextComponentString("Resources checked!")));
                }
            });
        }

        @Override
        public void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] args) throws CommandException {
            if (args.length > 0 && subCommandMap.containsKey(args[0])) {
                subCommandMap.get(args[0]).execute(minecraftServer, iCommandSender, Arrays.copyOfRange(args, 1, args.length));
            }
        }
    }

    private static int runRequest(ICommandSender context, String request) {
        try {
            HttpAPI.runString(
                    NetworkStuff.api.header(request),
                    (code, data) -> FiguraMod.sendChatMessage(new TextComponentString(data))
            );
            return 1;
        } catch (Exception e) {
            ((FiguraClientCommandSource)context).figura$sendError(new TextComponentString(e.getMessage()));
            return 0;
        }
    }
}
