package org.figuramc.figura.lua.docs;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.animation.Animation;
import org.figuramc.figura.commands.FiguraCommands;
import org.figuramc.figura.entries.FiguraAPI;
import org.figuramc.figura.lua.api.*;
import org.figuramc.figura.lua.api.action_wheel.Action;
import org.figuramc.figura.lua.api.action_wheel.ActionWheelAPI;
import org.figuramc.figura.lua.api.action_wheel.Page;
import org.figuramc.figura.lua.api.data.*;
import org.figuramc.figura.lua.api.entity.EntityAPI;
import org.figuramc.figura.lua.api.entity.LivingEntityAPI;
import org.figuramc.figura.lua.api.entity.PlayerAPI;
import org.figuramc.figura.lua.api.entity.ViewerAPI;
import org.figuramc.figura.lua.api.event.EventsAPI;
import org.figuramc.figura.lua.api.event.LuaEvent;
import org.figuramc.figura.lua.api.json.*;
import org.figuramc.figura.lua.api.keybind.FiguraKeybind;
import org.figuramc.figura.lua.api.keybind.KeybindAPI;
import org.figuramc.figura.lua.api.math.MatricesAPI;
import org.figuramc.figura.lua.api.math.VectorsAPI;
import org.figuramc.figura.lua.api.nameplate.EntityNameplateCustomization;
import org.figuramc.figura.lua.api.nameplate.NameplateAPI;
import org.figuramc.figura.lua.api.nameplate.NameplateCustomization;
import org.figuramc.figura.lua.api.nameplate.NameplateCustomizationGroup;
import org.figuramc.figura.lua.api.net.FiguraSocket;
import org.figuramc.figura.lua.api.net.HttpRequestsAPI;
import org.figuramc.figura.lua.api.net.NetworkingAPI;
import org.figuramc.figura.lua.api.net.SocketAPI;
import org.figuramc.figura.lua.api.particle.LuaParticle;
import org.figuramc.figura.lua.api.particle.ParticleAPI;
import org.figuramc.figura.lua.api.ping.PingAPI;
import org.figuramc.figura.lua.api.ping.PingFunction;
import org.figuramc.figura.lua.api.sound.LuaSound;
import org.figuramc.figura.lua.api.sound.SoundAPI;
import org.figuramc.figura.lua.api.vanilla_model.VanillaGroupPart;
import org.figuramc.figura.lua.api.vanilla_model.VanillaModelAPI;
import org.figuramc.figura.lua.api.vanilla_model.VanillaModelPart;
import org.figuramc.figura.lua.api.vanilla_model.VanillaPart;
import org.figuramc.figura.lua.api.world.BiomeAPI;
import org.figuramc.figura.lua.api.world.BlockStateAPI;
import org.figuramc.figura.lua.api.world.ItemStackAPI;
import org.figuramc.figura.lua.api.world.WorldAPI;
import org.figuramc.figura.math.matrix.FiguraMat2;
import org.figuramc.figura.math.matrix.FiguraMat3;
import org.figuramc.figura.math.matrix.FiguraMat4;
import org.figuramc.figura.math.matrix.FiguraMatrix;
import org.figuramc.figura.math.vector.FiguraVec2;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.math.vector.FiguraVec4;
import org.figuramc.figura.math.vector.FiguraVector;
import org.figuramc.figura.model.FiguraModelPart;
import org.figuramc.figura.model.rendering.Vertex;
import org.figuramc.figura.model.rendering.texture.FiguraTexture;
import org.figuramc.figura.model.rendertasks.*;
import org.figuramc.figura.utils.FiguraClientCommandSource;
import org.figuramc.figura.utils.FiguraText;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.*;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class FiguraDocsManager {

    // class name map
    private static final Map<Class<?>, String> NAME_MAP = new HashMap<Class<?>, String>() {{
        // Built in type names, even for things that don't have docs
        put(Double.class, "Number");
        put(double.class, "Number");
        put(Float.class, "Number");
        put(float.class, "Number");
        put(Number.class, "Number");

        put(Integer.class, "Integer");
        put(int.class, "Integer");
        put(Long.class, "Integer");
        put(long.class, "Integer");

        put(void.class, "nil");

        put(String.class, "String");

        put(Object.class, "AnyType");
        put(LuaUserdata.class, "Userdata");

        put(Boolean.class, "Boolean");
        put(boolean.class, "Boolean");

        // Lua things
        put(LuaFunction.class, "Function");
        put(LuaTable.class, "Table");
        put(LuaValue.class, "AnyType");
        put(Varargs.class, "Varargs");

        // converted things
        put(Map.class, "Table");
        put(HashMap.class, "Table");
        put(List.class, "Table");
        put(ArrayList.class, "Table");

        // Figura types
        put(FiguraVector.class, "Vector");
        put(FiguraMatrix.class, "Matrix");
    }};
    private static final Map<Class<?>, String> CLASS_COMMAND_MAP = new HashMap<>();

    // -- docs generator data -- // 

    private static final Map<String, Collection<Class<?>>> GLOBAL_CHILDREN = new HashMap<String, Collection<Class<?>>>() {{
        put("action_wheel", Arrays.asList(
                ActionWheelAPI.class,
                Page.class,
                Action.class
        ));

        put("animations", Arrays.asList(
                AnimationAPI.class,
                Animation.class
        ));

        put("nameplate", Arrays.asList(
                NameplateAPI.class,
                NameplateCustomization.class,
                EntityNameplateCustomization.class,
                NameplateCustomizationGroup.class
        ));

        put("world", Arrays.asList(
                WorldAPI.class,
                BiomeAPI.class,
                BlockStateAPI.class,
                ItemStackAPI.class
        ));

        put("vanilla_model", Arrays.asList(
                VanillaModelAPI.class,
                VanillaPart.class,
                VanillaModelPart.class,
                VanillaGroupPart.class
        ));

        put("models", Arrays.asList(
                Vertex.class,
                FiguraModelPart.class,
                RenderTask.class,
                BlockTask.class,
                ItemTask.class,
                TextTask.class,
                SpriteTask.class
        ));

        put("player", Arrays.asList(
                EntityAPI.class,
                LivingEntityAPI.class,
                PlayerAPI.class,
                ViewerAPI.class
        ));

        put("events", Arrays.asList(
                EventsAPI.class,
                LuaEvent.class
        ));

        put("keybinds", Arrays.asList(
                KeybindAPI.class,
                FiguraKeybind.class
        ));

        put("vectors", Arrays.asList(
                VectorsAPI.class,
                FiguraVec2.class,
                FiguraVec3.class,
                FiguraVec4.class
        ));

        put("matrices", Arrays.asList(
                MatricesAPI.class,
                FiguraMat2.class,
                FiguraMat3.class,
                FiguraMat4.class
        ));

        put("client", Collections.singletonList(
                ClientAPI.class
        ));

        put("host", Collections.singletonList(
                HostAPI.class
        ));

        put("avatar", Collections.singletonList(
                AvatarAPI.class
        ));

        put("particles", Arrays.asList(
                ParticleAPI.class,
                LuaParticle.class
        ));

        put("sounds", Arrays.asList(
                SoundAPI.class,
                LuaSound.class
        ));

        put("renderer", Collections.singletonList(
                RendererAPI.class
        ));

        put("pings", Arrays.asList(
                PingAPI.class,
                PingFunction.class
        ));

        put("textures", Arrays.asList(
                TextureAPI.class,
                FiguraTexture.class,
                TextureAtlasAPI.class
        ));

        put("config", Collections.singletonList(
                ConfigAPI.class
        ));

        put("data", Arrays.asList(
                DataAPI.class,
                FiguraInputStream.class,
                FiguraOutputStream.class,
                FiguraBuffer.class,
                FiguraFuture.class
        ));

        put("net", Arrays.asList(
                NetworkingAPI.class,
                HttpRequestsAPI.class,
                HttpRequestsAPI.HttpResponse.class,
                HttpRequestsAPI.HttpRequestBuilder.class,
                SocketAPI.class,
                FiguraSocket.class
        ));

        put("file", Collections.singletonList(
                FileAPI.class
        ));

        put("json", Arrays.asList(
                JsonAPI.class,
                FiguraJsonBuilder.class,
                FiguraJsonSerializer.class,
                FiguraJsonObject.class,
                FiguraJsonArray.class
        ));

        put("resources", Collections.singletonList(
                ResourcesAPI.class
        ));
        put("raycast", Collections.singletonList(
                RaycastAPI.class
        ));
    }};
    private static final Map<String, List<FiguraDoc>> GENERATED_CHILDREN = new HashMap<>();

    private static FiguraDoc.ClassDoc global;

    private static final List<Class<?>> LUA_LIB_OVERRIDES = Collections.singletonList(
            FiguraMathDocs.class
    );
    private static final List<FiguraDoc> GENERATED_LIB_OVERRIDES = new ArrayList<>();

    public static void init() {
        // generate children override
        for (Map.Entry<String, Collection<Class<?>>> packageEntry : GLOBAL_CHILDREN.entrySet()) {
            for (Class<?> documentedClass : packageEntry.getValue()) {
                FiguraDoc.ClassDoc doc = generateDocFor(documentedClass, "globals " + packageEntry.getKey());
                if (doc != null)
                    GENERATED_CHILDREN.computeIfAbsent(packageEntry.getKey(), s -> new ArrayList<>()).add(doc);
            }
        }

        // generate standard libraries overrides
        for (Class<?> lib : LUA_LIB_OVERRIDES) {
            FiguraDoc.ClassDoc libDoc = generateDocFor(lib, null);
            if (libDoc != null)
                GENERATED_LIB_OVERRIDES.add(libDoc);
        }

        // generate globals
        Class<?> globalClass = FiguraGlobalsDocs.class;
        global = new FiguraDoc.ClassDoc(globalClass, globalClass.getAnnotation(LuaTypeDoc.class), GENERATED_CHILDREN);
    }

    public static void initEntryPoints(Set<FiguraAPI> set) {
        for (FiguraAPI api : set)
            GLOBAL_CHILDREN.put(api.getName(), api.getDocsClasses());
    }

    private static FiguraDoc.ClassDoc generateDocFor(Class<?> documentedClass, String pack) {
        if (!documentedClass.isAnnotationPresent(LuaTypeDoc.class))
            return null;

        FiguraDoc.ClassDoc doc = new FiguraDoc.ClassDoc(documentedClass, documentedClass.getAnnotation(LuaTypeDoc.class));
        NAME_MAP.put(documentedClass, doc.name);
        CLASS_COMMAND_MAP.put(documentedClass, "/figura docs " + (pack == null ? "" : pack) + " " + doc.name);
        return doc;
    }

    public static String getNameFor(Class<?> clazz) {
        return NAME_MAP.computeIfAbsent(clazz, aClass -> {
            if (clazz.isAnnotationPresent(LuaTypeDoc.class))
                return clazz.getAnnotation(LuaTypeDoc.class).name();
            else if (clazz.getName().startsWith("["))
                return "Varargs";
            else
                return clazz.getName();
        });
    }

    public static ITextComponent getClassText(Class<?> clazz) {
        String name = getNameFor(clazz);
        String doc = CLASS_COMMAND_MAP.get(clazz);

        ITextComponent text = new TextComponentString(name);
        if (doc == null)
            return text;

        text.setStyle(
                new Style().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, doc))
                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new FiguraText("command.docs_type_hover", new TextComponentString(name).setStyle(new Style().setColor(TextFormatting.DARK_PURPLE)))))
                .setUnderlined(true));
        return text;
    }

    // -- commands -- // 

    public static class DocsCommand extends FiguraCommands.FiguraSubCommand {

        public DocsCommand() {
            super("docs");

            // globals
            subSubCommand.put("globals", global.getCommand());

            // library overrides
            for (FiguraDoc figuraDoc : GENERATED_LIB_OVERRIDES)
                subSubCommand.put(figuraDoc.name, figuraDoc.getCommand());

            // list docs
            subSubCommand.put("enums", FiguraListDocs.getCommand());
        }

        public static Map<String, FiguraCommands.FiguraSubCommand> subSubCommand = new HashMap<>();
        @Override
        public void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] args) throws CommandException {
            if (args.length == 0) {
                FiguraDoc.printRoot();
                return;
            }
            if (subSubCommand.containsKey(args[0])) {
                subSubCommand.get(args[0]).execute(minecraftServer, iCommandSender, Arrays.copyOfRange(args, 1, args.length));
            }
        }

        @Override
        public List<String> getTabCompletions(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] strings, @Nullable BlockPos targetPos) {
            if (strings.length > 0 && subSubCommand.containsKey(strings[0]))
                return subSubCommand.get(strings[0]).getTabCompletions(minecraftServer, iCommandSender, Arrays.copyOfRange(strings, 1, strings.length), targetPos);
            return CommandBase.getListOfStringsMatchingLastWord(strings, subSubCommand.keySet());
        }
    }

    public static class ExportCommand extends FiguraCommands.FiguraSubCommand {

        public ExportCommand() {
            super("docs");
        }

        @Override
        public void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] args) throws CommandException {
            if (args.length == 0) {
                exportDocsFunction(iCommandSender, true);
                return;
            }
            boolean translate = CommandBase.parseBoolean(args[0]);
            exportDocsFunction(iCommandSender, translate);
        }

        @Override
        public List<String> getTabCompletions(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] strings, @Nullable BlockPos targetPos) {
            return CommandBase.getListOfStringsMatchingLastWord(strings, "true", "false");
        }
    }

    // -- export -- // 

    private static int exportDocsFunction(ICommandSender context, boolean translate) {
        try {
            // get path
            Path targetPath = FiguraMod.getFiguraDirectory().resolve("exported_docs.json");

            // create file
            if (!Files.exists(targetPath))
                Files.createFile(targetPath);

            // write file
            OutputStream fs = Files.newOutputStream(targetPath);
            fs.write(exportAsJsonString(translate).getBytes());
            fs.close();

            // feedback
            ((FiguraClientCommandSource)context).figura$sendFeedback(
                    new FiguraText("command.docs_export.success")
                            .appendText(" ")
                            .appendSibling(new FiguraText("command.click_to_open")
                                    .setStyle(new Style().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, targetPath.toString())).setUnderlined(true))
                            )
            );
            return 1;
        } catch (Exception e) {
            ((FiguraClientCommandSource)context).figura$sendError(new FiguraText("command.docs_export.error"));
            FiguraMod.LOGGER.error("Failed to export docs!", e);
            return 0;
        }
    }

    public static String exportAsJsonString(boolean translate) {
        // root
        JsonObject root = new JsonObject();

        // globals
        JsonObject globals = global == null ? new JsonObject() : global.toJson(translate);
        root.add("globals", globals);

        // library overrides
        for (FiguraDoc figuraDoc : GENERATED_LIB_OVERRIDES)
            root.add(figuraDoc.name, figuraDoc.toJson(translate));

        // lists
        root.add("lists", FiguraListDocs.toJson(translate));

        // return as string
        return new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create().toJson(root);
    }
}
