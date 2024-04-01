package org.figuramc.figura.commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import org.figuramc.figura.lua.FiguraLuaPrinter;
import org.figuramc.figura.lua.FiguraLuaRuntime;

class RunCommand {
    public static class RunSubCommand extends FiguraCommands.FiguraSubCommand {

        public RunSubCommand() {
            super("run");
        }

        @Override
        public void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] args) throws CommandException {
            executeCode(iCommandSender, CommandBase.buildString(args, 0));
        }
    }

    private static int executeCode(ICommandSender context, String lua) {
        FiguraLuaRuntime luaRuntime = FiguraCommands.getRuntime(context);
        if (luaRuntime == null)
            return 0;

        try {
            luaRuntime.load("runCommand", lua).call();
            return 1;
        } catch (Exception | StackOverflowError e) {
            FiguraLuaPrinter.sendLuaError(FiguraLuaRuntime.parseError(e), luaRuntime.owner);
            return 0;
        }
    }
}
