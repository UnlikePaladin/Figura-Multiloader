package org.figuramc.figura.commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.backend2.BackendCommands;
import org.figuramc.figura.lua.FiguraLuaRuntime;
import org.figuramc.figura.lua.docs.FiguraDocsManager;
import org.figuramc.figura.model.rendering.AvatarRenderer;
import org.figuramc.figura.utils.FiguraClientCommandSource;
import org.figuramc.figura.utils.FiguraText;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FiguraCommands {

    private static Map<String, FiguraSubCommand> subCommands = new HashMap<>();

    public static class FiguraRootCommand extends CommandBase {
        FiguraRootCommand() {
            registerSubCommands();
        }

        @Override
        public String getName() {
            return FiguraMod.MOD_ID;
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "";
        } // TODO : add usage for commands

        @Override
        public void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] args) throws CommandException {
            if (args.length > 0) {
                if (subCommands.containsKey(args[0])) {
                    subCommands.get(args[0]).execute(minecraftServer, iCommandSender, Arrays.copyOfRange(args, 1, args.length));
                }
            }
        }

        @Override
        public List<String> getTabCompletions(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] args, @Nullable BlockPos targetPos) {
            if (args.length > 0) {
                if (subCommands.containsKey(args[0])) {
                    subCommands.get(args[0]).getTabCompletions(minecraftServer, iCommandSender, Arrays.copyOfRange(args, 1, args.length), targetPos);
                }
            }
            return super.getTabCompletions(minecraftServer, iCommandSender, args, targetPos);
        }
    }

    public static void registerSubCommands() {
        // docs
        new FiguraDocsManager.DocsCommand().registerSubCommand();

        // links
        new LinkCommand.LinkSubCommand().registerSubCommand();

        // run
        new RunCommand.RunSubCommand().registerSubCommand();

        // load
        new LoadCommand.LoadSubCommand().registerSubCommand();

        // reload
        new ReloadCommand.ReloadSubCommand().registerSubCommand();

        // debug
        new DebugCommand.DebugSubCommand().registerSubCommand();

        // export
        new ExportCommand.ExportSubCommand().registerSubCommand();

        if (FiguraMod.debugModeEnabled()) {
            // backend debug
            new BackendCommands.BackendSubCommand().registerSubCommand();

            // set avatar command
            new AvatarManager.SetAvatarCommand().registerSubCommand();
        }

        // emoji list
        new EmojiListCommand.EmojiListSubCommand().registerSubCommand();
    }

    protected static Avatar checkAvatar(ICommandSender context) {
        Avatar avatar = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());
        if (avatar == null) {
            ((FiguraClientCommandSource)context).figura$sendError(new FiguraText("command.no_avatar_error"));
            return null;
        }
        return avatar;
    }

    protected static FiguraLuaRuntime getRuntime(ICommandSender context) {
        Avatar avatar = checkAvatar(context);
        if (avatar == null)
            return null;
        if (avatar.luaRuntime == null || avatar.scriptError) {
            ((FiguraClientCommandSource)context).figura$sendError(new FiguraText("command.no_script_error"));
            return null;
        }
        return avatar.luaRuntime;
    }

    protected static AvatarRenderer getRenderer(ICommandSender context) {
        Avatar avatar = checkAvatar(context);
        if (avatar == null)
            return null;
        if (avatar.renderer == null) {
            ((FiguraClientCommandSource)context).figura$sendError(new FiguraText("command.no_renderer_error"));
            return null;
        }
        return avatar.renderer;
    }


    /**
     * Helper class used for sub commands in Figura
     */
    public abstract static class FiguraSubCommand {
        String name;
        public FiguraSubCommand(String name) {
            this.name = name;
        }
        public String getName() {
            return name;
        }

        /**
         * This method adds the command to the main figura root, it should only be called on
         * sub commands that follow it such as docs, avatar, etc...
         */
        public void registerSubCommand() {
            subCommands.put(this.getName(), this);
        }

        public abstract void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] args) throws CommandException;

        public List<String> getTabCompletions(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] strings, @Nullable BlockPos targetPos) {
            return Collections.emptyList();
        }
    }
}
