package org.figuramc.figura.commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.server.MinecraftServer;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.docs.FiguraDocsManager;
import org.figuramc.figura.model.rendering.AvatarRenderer;
import org.figuramc.figura.model.rendering.texture.FiguraTexture;
import org.figuramc.figura.utils.FiguraClientCommandSource;
import org.figuramc.figura.utils.FiguraText;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class ExportCommand {

    public static class ExportSubCommand extends FiguraCommands.FiguraSubCommand {

        public static Map<String, FiguraCommands.FiguraSubCommand> subCommandMap = new HashMap<>();
        public ExportSubCommand() {
            super("export");

            // texture
            subCommandMap.put("texture", new ExportTextureSubCommand());

            // docs
            subCommandMap.put("docs", new FiguraDocsManager.ExportCommand());

            // avatar
            subCommandMap.put("avatar", new ExportAvatarSubCommand());
        }

        @Override
        public void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] args) throws CommandException {
            if (args.length > 0 && subCommandMap.containsKey(args[0]))
                subCommandMap.get(args[0]).execute(minecraftServer, iCommandSender, Arrays.copyOfRange(args, 1, args.length));
        }
    }

    private static class ExportTextureSubCommand extends FiguraCommands.FiguraSubCommand {
        public ExportTextureSubCommand() {
            super("texture");
        }

        @Override
        public void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] args) throws CommandException {
            if (args.length == 1) {
                runTextureExport(iCommandSender, "exportedTexture", args[0]);
            } else if (args.length > 1){
                runTextureExport(iCommandSender, CommandBase.buildString(args, 1), args[0]);
            }
        }
    }

    private static int runTextureExport(ICommandSender context, String filename, String textureName) {
        AvatarRenderer renderer = FiguraCommands.getRenderer(context);
        if (renderer == null)
            return 0;

        try {
            FiguraTexture texture = renderer.getTexture(textureName);
            if (texture == null)
                throw new Exception();

            texture.writeTexture(FiguraMod.getFiguraDirectory().resolve(filename + ".png"));

            ((FiguraClientCommandSource)context).figura$sendFeedback(new FiguraText("command.export_texture.success"));
            return 1;
        } catch (Exception e) {
            ((FiguraClientCommandSource)context).figura$sendError(new FiguraText("command.export_texture.error"));
            return 0;
        }
    }

    public static class ExportAvatarSubCommand extends FiguraCommands.FiguraSubCommand {

        public ExportAvatarSubCommand() {
            super("avatar");
        }

        @Override
        public void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] args) throws CommandException {
            if (args.length == 0) {
                runAvatarExport(iCommandSender, "exported_avatar");
            } else {
                runAvatarExport(iCommandSender, CommandBase.buildString(args, 0));
            }
        }
    }

    private static int runAvatarExport(ICommandSender context, String filename) {
        Avatar avatar = FiguraCommands.checkAvatar(context);
        if (avatar == null)
            return 0;

        try {
            if (avatar.nbt == null)
                throw new Exception();

            CompressedStreamTools.writeCompressed(avatar.nbt, Files.newOutputStream(FiguraMod.getFiguraDirectory().resolve(filename + ".moon")));

            ((FiguraClientCommandSource)context).figura$sendFeedback(new FiguraText("command.export_avatar.success"));
            return 1;
        } catch (Exception e) {
            ((FiguraClientCommandSource)context).figura$sendError(new FiguraText("command.export_avatar.error"));
            return 0;
        }
    }
}
