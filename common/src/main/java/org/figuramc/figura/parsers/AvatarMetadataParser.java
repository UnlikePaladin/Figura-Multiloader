package org.figuramc.figura.parsers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.nbt.*;

import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.model.ParentType;
import org.figuramc.figura.model.rendering.texture.RenderTypes;
import org.figuramc.figura.utils.NbtType;
import org.figuramc.figura.utils.PathUtils;
import org.figuramc.figura.utils.Version;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// parses a metadata json
// and return a nbt compound of it
public class AvatarMetadataParser {

    private static final Gson GSON = new GsonBuilder().create();
    private static final Map<String, String> PARTS_TO_MOVE = new HashMap<>();

    public static Metadata read(String json) {
        Metadata metadata = GSON.fromJson(json, Metadata.class);
        return metadata == null ? new Metadata() : metadata;
    }

    static JsonParser parser = new JsonParser();
    public static NBTTagCompound parse(String json, String filename) {
        // parse json -> object
        Metadata metadata = read(json);

        // nbt
        NBTTagCompound nbt = new NBTTagCompound();
        JsonElement jsonElement = parser.parse(json);
        if (jsonElement != null && !jsonElement.isJsonNull() && !jsonElement.getAsJsonObject().entrySet().isEmpty()) {
            for (Map.Entry<String, JsonElement> jsonElementEntry : jsonElement.getAsJsonObject().entrySet()) {
                if (jsonElementEntry.getKey() != null && !jsonElementEntry.getKey().trim().isEmpty() && jsonElementEntry.getKey().contains("badge_color_")) {
                    nbt.setString(jsonElementEntry.getKey(), jsonElementEntry.getValue().getAsString());
                }
            }
        }

        // version
        Version version = new Version(metadata.version);
        if (version.invalid)
            version = FiguraMod.VERSION;

        nbt.setString("name", metadata.name == null || metadata.name.trim().isEmpty() ? filename : metadata.name);
        nbt.setString("ver", version.toString());
        if (metadata.color != null) nbt.setString("color", metadata.color);
        if (metadata.background != null) nbt.setString("bg", metadata.background);
        if (metadata.id != null) nbt.setString("id", metadata.id);

        if (metadata.authors != null) {
            StringBuilder authors = new StringBuilder();

            for (int i = 0; i < metadata.authors.length; i++) {
                String name = metadata.authors[i];
                authors.append(name);

                if (i < metadata.authors.length - 1)
                    authors.append("\n");
            }

            nbt.setString("authors", authors.toString());
        } else {
            nbt.setString("authors", metadata.author == null ? "?" : metadata.author);
        }

        if (metadata.autoScripts != null) {
            NBTTagList autoScripts = new NBTTagList();
            for (String name : metadata.autoScripts) {
                autoScripts.appendTag(new NBTTagString(PathUtils.computeSafeString(name.replaceAll("\\.lua$", ""))));
            }
            nbt.setTag("autoScripts", autoScripts);
        }

        if (Configs.FORMAT_SCRIPT.value >= 2)
            nbt.setBoolean("minify", true);

        if (metadata.autoAnims != null) {
            NBTTagList autoAnims = new NBTTagList();
            for (String name : metadata.autoAnims)
                autoAnims.appendTag(new NBTTagString(name));
            nbt.setTag("autoAnims", autoAnims);
        }

        if (metadata.resources != null) {
            NBTTagList resourcesPaths = new NBTTagList();
            for (String resource :
                    metadata.resources) {
                resourcesPaths.appendTag(new NBTTagString(resource));
            }
            nbt.setTag("resources_paths", resourcesPaths);
        }

        return nbt;
    }

    public static void injectToModels(String json, NBTTagCompound models) throws IOException {
        PARTS_TO_MOVE.clear();

        Metadata metadata = GSON.fromJson(json, Metadata.class);
        if (metadata != null && metadata.customizations != null) {
            for (Map.Entry<String, Customization> entry : metadata.customizations.entrySet())
                injectCustomization(entry.getKey(), entry.getValue(), models);
        }

        for (Map.Entry<String, String> entry : PARTS_TO_MOVE.entrySet()) {
            NBTTagCompound modelPart = getTag(models, entry.getKey(), true);
            NBTTagCompound targetPart = getTag(models, entry.getValue(), false);

            NBTTagList list = !targetPart.hasKey("chld") ? new NBTTagList() : targetPart.getTagList("chld", NbtType.COMPOUND.getValue());
            list.appendTag(modelPart);
            targetPart.setTag("chld", list);
        }
    }

    private static void injectCustomization(String path, Customization customization, NBTTagCompound models) throws IOException {
        boolean remove = customization.remove != null && customization.remove;
        NBTTagCompound modelPart = getTag(models, path, remove);

        // Add more of these later
        if (remove) {
            return;
        }
        if (customization.primaryRenderType != null) {
            try {
                modelPart.setString("primary", RenderTypes.valueOf(customization.primaryRenderType.toUpperCase()).name());
            } catch (Exception ignored) {
                throw new IOException("Invalid render type \"" + customization.primaryRenderType + "\"!");
            }
        }
        if (customization.secondaryRenderType != null) {
            try {
                modelPart.setString("secondary", RenderTypes.valueOf(customization.secondaryRenderType.toUpperCase()).name());
            } catch (Exception ignored) {
                throw new IOException("Invalid render type \"" + customization.secondaryRenderType + "\"!");
            }
        }
        if (customization.parentType != null) {
            ParentType type = ParentType.get(customization.parentType);

            if (type == ParentType.None)
                modelPart.removeTag("pt");
            else
                modelPart.setString("pt", type.name());
        }
        if (customization.moveTo != null) {
            PARTS_TO_MOVE.put(path, customization.moveTo);
        }
        if (customization.visible != null) {
            if (customization.visible) {
                modelPart.removeTag("vsb");
            } else {
                modelPart.setBoolean("vsb", false);
            }
        }
        if (customization.smooth != null) {
            modelPart.setBoolean("smo", customization.smooth);
        }
    }

    private static NBTTagCompound getTag(NBTTagCompound models, String path, boolean remove) throws IOException {
        String[] keys = path.replaceFirst("^models", "").split("\\.", 0);
        NBTTagCompound current = models;

        for (int i = 0; i < keys.length; i++) {
            if (keys[i].isEmpty())
                continue;

            if (!current.hasKey("chld"))
                throw new IOException("Invalid part path: \"" + path + "\"");

            NBTTagList children = current.getTagList("chld", NbtType.COMPOUND.getValue());
            int j = 0;
            for (; j < children.tagCount(); j++) {
                NBTTagCompound child = children.getCompoundTagAt(j);

                if (child.getString("name").equals(keys[i])) {
                    current = child;
                    break;
                }

                if (j == children.tagCount() - 1)
                    throw new IOException("Invalid part path: \"" + path + "\"");
            }

            if (remove && i == keys.length - 1)
                children.removeTag(j);
        }

        return current;
    }

    public static void injectToTextures(String json, NBTTagCompound textures) {
        Metadata metadata = GSON.fromJson(json, Metadata.class);
        if (metadata == null || metadata.ignoredTextures == null)
            return;

        NBTTagCompound src = textures.getCompoundTag("src");

        for (String texture : metadata.ignoredTextures) {
            byte[] bytes = src.getByteArray(texture);
            int[] size = BlockbenchModelParser.getTextureSize(bytes);
            NBTTagList list = new NBTTagList();
            list.appendTag(new NBTTagInt(size[0]));
            list.appendTag(new NBTTagInt(size[1]));
            src.setTag(texture, list);
        }
    }

    // json object class
    public static class Metadata {
        public String name, description, author, version, color, background, id;
        public String[] authors, autoScripts, autoAnims, ignoredTextures, resources;
        public HashMap<String, Customization> customizations;
    }

    /**
     * Contains only things you can't normally set in blockbench.
     * So nothing about position, rotation, scale, uv, whatever
     * customizations you could just put in the model regularly yourself.
     */
    public static class Customization {
        public String primaryRenderType, secondaryRenderType;
        public String parentType;
        public String moveTo;
        public Boolean visible, remove, smooth;
    }
}
