package org.figuramc.figura.commands;

import com.google.gson.*;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.event.ClickEvent;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.animation.Animation;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.local.CacheAvatarLoader;
import org.figuramc.figura.avatar.local.LocalAvatarFetcher;
import org.figuramc.figura.avatar.local.LocalAvatarLoader;
import org.figuramc.figura.backend2.NetworkStuff;
import org.figuramc.figura.config.ConfigManager;
import org.figuramc.figura.config.ConfigType;
import org.figuramc.figura.lua.api.ConfigAPI;
import org.figuramc.figura.mixin.CompressedStreamToolsAccessor;
import org.figuramc.figura.permissions.PermissionManager;
import org.figuramc.figura.permissions.PermissionPack;
import org.figuramc.figura.permissions.Permissions;
import org.figuramc.figura.resources.FiguraRuntimeResources;
import org.figuramc.figura.utils.*;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.zip.GZIPOutputStream;

class DebugCommand {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().serializeNulls().setPrettyPrinting().create();

    public static class DebugSubCommand extends FiguraCommands.FiguraSubCommand {

        public DebugSubCommand() {
            super("debug");
        }

        @Override
        public void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] args) throws CommandException {
            commandAction(iCommandSender);
        }
    }

    private static int commandAction(ICommandSender context) {
        try {
            // get path
            Path targetPath = FiguraMod.getFiguraDirectory().resolve("debug_data.json");

            // create file
            if (!Files.exists(targetPath))
                Files.createFile(targetPath);

            // write file
            OutputStream fs = Files.newOutputStream(targetPath);
            fs.write(fetchStatus(AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID())).getBytes());
            fs.close();

            // feedback
            ((FiguraClientCommandSource)context).figura$sendFeedback(
                    new FiguraText("command.debug.success")
                            .appendText(" ")
                            .appendSibling(new FiguraText("command.click_to_open")
                                    .setStyle(new Style().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, targetPath.toString())).setUnderlined(true))
                            )
            );
            return 1;
        } catch (Exception e) {
            ((FiguraClientCommandSource)context).figura$sendError(new FiguraText("command.debug.error"));
            FiguraMod.LOGGER.error("Failed to save " + FiguraMod.MOD_NAME + " debug data!", e);
            return 0;
        }
    }

    public static String fetchStatus(Avatar avatar) {
        // root
        JsonObject root = new JsonObject();

        // mod meta
        JsonObject meta = new JsonObject();

        meta.addProperty("version", FiguraMod.VERSION.toString());
        meta.addProperty("localUUID", FiguraMod.getLocalPlayerUUID().toString());
        meta.addProperty("ticks", FiguraMod.ticks);
        meta.addProperty("figuraDirectory", FiguraMod.getFiguraDirectory().toString());
        meta.addProperty("figuraAvatarDirectory", LocalAvatarFetcher.getLocalAvatarDirectory().toString());
        meta.addProperty("figuraAvatarDataDirectory", ConfigAPI.getConfigDataDir().toString());
        meta.addProperty("figuraCacheDirectory", FiguraMod.getCacheDirectory().toString());
        meta.addProperty("figuraAvatarCacheDirectory", CacheAvatarLoader.getAvatarCacheDirectory().toString());
        meta.addProperty("figuraResourcesDirectory", FiguraRuntimeResources.getRootDirectory().toString());
        meta.addProperty("figuraAssetsDirectory", FiguraRuntimeResources.getAssetsDirectory().toString());
        meta.addProperty("backendStatus", NetworkStuff.backendStatus);
        meta.addProperty("backendConnected", NetworkStuff.isConnected());
        meta.addProperty("backendDisconnectedReason", NetworkStuff.disconnectedReason);
        meta.addProperty("uploaded", AvatarManager.localUploaded);
        meta.addProperty("lastLoadedPath", Objects.toString(LocalAvatarLoader.getLastLoadedPath(), null));
        meta.addProperty("panicMode", AvatarManager.panic);

        root.add("meta", meta);

        // config
        JsonObject config = new JsonObject();

        for (ConfigType<?> value : ConfigManager.REGISTRY)
            if (value.value != null)
                config.addProperty(value.id, value.value.toString());

        root.add("config", config);

        // all permissions
        JsonObject permissions = new JsonObject();

        for (PermissionPack.CategoryPermissionPack group : PermissionManager.CATEGORIES.values()) {
            JsonObject allPermissions = new JsonObject();

            JsonObject standard = new JsonObject();
            for (Map.Entry<Permissions, Integer> entry : group.getPermissions().entrySet())
                standard.addProperty(entry.getKey().name, entry.getValue());

            allPermissions.add("standard", standard);

            JsonObject customPermissions = new JsonObject();
            for (Map.Entry<String, Map<Permissions, Integer>> entry : group.getCustomPermissions().entrySet()) {
                JsonObject obj = new JsonObject();
                for (Map.Entry<Permissions, Integer> entry1 : entry.getValue().entrySet())
                    obj.addProperty(entry1.getKey().name, entry1.getValue());

                customPermissions.add(entry.getKey(), obj);
            }

            allPermissions.add("custom", customPermissions);

            permissions.add(group.name, allPermissions);
        }

        root.add("permissions", permissions);

        // avatars
        LocalAvatarFetcher.reloadAvatars().join();
        root.add("avatars", getAvatarsPaths(LocalAvatarFetcher.ALL_AVATARS));


        // -- avatar -- // 


        if (avatar == null)
            return GSON.toJson(root);

        JsonObject a = new JsonObject();

        // permissions
        JsonObject aPermissions = new JsonObject();

        aPermissions.addProperty("category", avatar.permissions.category.name);

        JsonObject standard = new JsonObject();
        for (Map.Entry<Permissions, Integer> entry : avatar.permissions.getPermissions().entrySet())
            standard.addProperty(entry.getKey().name, entry.getValue());

        aPermissions.add("standard", standard);

        JsonObject customPermissions = new JsonObject();
        for (Map.Entry<String, Map<Permissions, Integer>> entry : avatar.permissions.getCustomPermissions().entrySet()) {
            JsonObject obj = new JsonObject();
            for (Map.Entry<Permissions, Integer> entry1 : entry.getValue().entrySet())
                obj.addProperty(entry1.getKey().name, entry1.getValue());

            customPermissions.add(entry.getKey(), obj);
        }

        aPermissions.add("custom", customPermissions);

        a.add("permissions", aPermissions);

        // avatar metadata
        JsonObject aMeta = new JsonObject();

        aMeta.addProperty("version", avatar.version.toString());
        aMeta.addProperty("versionStatus", avatar.versionStatus);
        aMeta.addProperty("color", avatar.color);
        aMeta.addProperty("authors", avatar.authors);
        aMeta.addProperty("name", avatar.name);
        aMeta.addProperty("entityName", avatar.entityName);
        aMeta.addProperty("fileSize", avatar.fileSize);
        aMeta.addProperty("isHost", avatar.isHost);
        aMeta.addProperty("loaded", avatar.loaded);
        aMeta.addProperty("owner", avatar.owner.toString());
        aMeta.addProperty("scriptError", avatar.scriptError);
        aMeta.addProperty("hasTexture", avatar.hasTexture);
        aMeta.addProperty("hasLuaRuntime", avatar.luaRuntime != null);
        aMeta.addProperty("hasRenderer", avatar.renderer != null);
        aMeta.addProperty("hasData", avatar.nbt != null);
        for (Map.Entry<String, String> entry: avatar.badgeToColor.entrySet()) {
            aMeta.addProperty(entry.getKey(), entry.getValue());
        }

        a.add("meta", aMeta);

        // avatar complexity
        JsonObject inst = new JsonObject();

        inst.addProperty("animationComplexity", avatar.animationComplexity);
        inst.addProperty("animationInstructions", avatar.animation.pre);
        inst.addProperty("complexity", avatar.complexity.pre);
        inst.addProperty("entityInitInstructions", avatar.init.post);
        inst.addProperty("entityRenderInstructions", avatar.render.pre);
        inst.addProperty("entityTickInstructions", avatar.tick.pre);
        inst.addProperty("initInstructions", avatar.init.pre);
        inst.addProperty("postEntityRenderInstructions", avatar.render.post);
        inst.addProperty("postWorldRenderInstructions", avatar.worldRender.post);
        inst.addProperty("worldRenderInstructions", avatar.worldRender.pre);
        inst.addProperty("worldTickInstructions", avatar.worldTick.pre);
        inst.addProperty("particlesRemaining", avatar.particlesRemaining.peek());
        inst.addProperty("soundsRemaining", avatar.soundsRemaining.peek());

        a.add("instructions", inst);

        // sounds
        JsonArray sounds = new JsonArray();

        for (String s : avatar.customSounds.keySet())
            sounds.add(s);

        a.add("sounds", sounds);

        // animations
        JsonArray animations = new JsonArray();

        for (Animation animation : avatar.animations.values())
            animations.add(animation.modelName + "/" + animation.name);

        a.add("animations", animations);

        // sizes
        if (avatar.nbt != null)
            a.add("sizes", parseNbtSizes(avatar.nbt));

        // return as string
        root.add("avatar", a);
        return GSON.toJson(root);
    }

    private static JsonObject getAvatarsPaths(List<LocalAvatarFetcher.AvatarPath> list) {
        JsonObject avatar = new JsonObject();

        for (LocalAvatarFetcher.AvatarPath path : list) {
            String name = IOUtils.getFileNameOrEmpty(path.getPath());

            if (path instanceof LocalAvatarFetcher.FolderPath) {
                LocalAvatarFetcher.FolderPath folder = (LocalAvatarFetcher.FolderPath) path;
                avatar.add(name, getAvatarsPaths(folder.getChildren()));
            } else
                avatar.addProperty(name, path.getName());
        }

        return avatar;
    }

    private static JsonObject parseNbtSizes(NBTTagCompound nbt) {
        JsonObject sizes = new JsonObject();

        // metadata
        sizes.addProperty("metadata", parseSize(getBytesFromNbt(nbt.getCompoundTag("metadata"))));

        // models
        NBTTagCompound modelsNbt = nbt.getCompoundTag("models");
        NBTTagList childrenNbt = modelsNbt.getTagList("chld", NbtType.COMPOUND.getValue());
        JsonObject models = parseListSize(childrenNbt, tag -> tag.getString("name"));
        sizes.add("models", models);
        sizes.addProperty("models_total", parseSize(getBytesFromNbt(modelsNbt)));

        // animations
        NBTTagList animationsNbt = nbt.getTagList("animations", NbtType.COMPOUND.getValue());
        JsonObject animations = parseListSize(animationsNbt, tag -> tag.getString("mdl") + "." + tag.getString("name"));
        sizes.add("animations", animations);
        sizes.addProperty("animations_total", parseSize(getBytesFromNbt(animationsNbt)));

        // textures
        NBTTagCompound texturesNbt = nbt.getCompoundTag("textures");
        NBTTagCompound textureSrc = texturesNbt.getCompoundTag("src");
        JsonObject textures = parseCompoundSize(textureSrc);
        sizes.add("textures", textures);
        sizes.addProperty("textures_total", parseSize(getBytesFromNbt(texturesNbt)));

        // scripts
        NBTTagCompound scriptsNbt = nbt.getCompoundTag("scripts");
        JsonElement scripts = parseCompoundSize(scriptsNbt);
        sizes.add("scripts", scripts);
        sizes.addProperty("scripts_total", parseSize(getBytesFromNbt(scriptsNbt)));

        // sounds
        NBTTagCompound soundsNbt = nbt.getCompoundTag("sounds");
        JsonObject sounds = parseCompoundSize(soundsNbt);
        sizes.add("sounds", sounds);
        sizes.addProperty("sounds_total", parseSize(getBytesFromNbt(soundsNbt)));

        // total
        sizes.addProperty("total", parseSize(getBytesFromNbt(nbt)));
        return sizes;
    }

    private static int getBytesFromNbt(NBTBase nbt) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(baos)));
            CompressedStreamToolsAccessor.figura$invokeWriteUnnamedTag(nbt, dos);
            dos.close();

            int size = baos.size();
            baos.close();

            return size;
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static String parseSize(int size) {
        return size < 1000 ? size + "b" : MathUtils.asFileSize(size) + " (" + size + "b)";
    }

    private static JsonObject parseListSize(NBTTagList listNbt, Function<NBTTagCompound, String> function) {
        JsonObject target = new JsonObject();
        HashMap<String, Integer> sizesMap = new HashMap<>();

        for (int i = 0; i < listNbt.tagCount(); i++) {
            NBTTagCompound compound = listNbt.getCompoundTagAt(i);
            sizesMap.put(function.apply(compound), getBytesFromNbt(compound));
        }
        insertJsonSortedData(sizesMap, target);

        return target;
    }

    private static JsonObject parseCompoundSize(NBTTagCompound compoundNbt) {
        JsonObject target = new JsonObject();
        HashMap<String, Integer> sizesMap = new HashMap<>();

        for (String key : compoundNbt.getKeySet())
            sizesMap.put(key, getBytesFromNbt(compoundNbt.getTag(key)));
        insertJsonSortedData(sizesMap, target);

        return target;
    }

    private static JsonElement parseTagRecursive(NBTBase tag) {
        if (tag instanceof NBTTagCompound) {
            NBTTagCompound compoundTag = (NBTTagCompound) tag;
            JsonObject obj = new JsonObject();
            HashMap<String, Integer> sizesMap = new HashMap<>();
            for (String key : compoundTag.getKeySet()) {
                JsonElement value = parseTagRecursive(compoundTag.getTag(key));
                if (value instanceof JsonPrimitive && ((JsonPrimitive) value).isNumber()) {
                    JsonPrimitive size = (JsonPrimitive) value;
                    sizesMap.put(key, size.getAsInt());
                } else
                    obj.add(key, value);
            }
            insertJsonSortedData(sizesMap, obj);
            return obj;
        }
        else {
            return new JsonPrimitive(getBytesFromNbt(tag));
        }
    }

    private static void insertJsonSortedData(HashMap<String, Integer> sizesMap, JsonObject json) {
        sizesMap.entrySet().stream().sorted((Map.Entry.<String, Integer>comparingByValue().reversed())).forEach(e -> json.addProperty(e.getKey(), parseSize(e.getValue())));
    }
}
