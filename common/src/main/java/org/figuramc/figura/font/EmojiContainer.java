package org.figuramc.figura.font;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.HoverEvent;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.ducks.extensions.StyleExtension;
import org.figuramc.figura.utils.FiguraIdentifier;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.JsonUtils;
import org.figuramc.figura.utils.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.figuramc.figura.font.Emojis.DELIMITER;


public class EmojiContainer {
    public static final String JSON_KEY_FRAMES = "frames";
    public static final String JSON_KEY_FRAME_TIME = "frametime";
    public static final String JSON_KEY_WIDTH = "width";
    public static final String JSON_KEY_NAMES = "names";
    public static final String JSON_KEY_SHORTCUTS = "shortcuts";

    private static final String ERROR_MSG = "Invalid emoji metadata \"{}\" @ \"{}\", Reason: Field '{}' {}";

    private static final Style STYLE = new Style().setColor(TextFormatting.WHITE);

    public final String name;
    private final ResourceLocation font;
    private final EmojiUnicodeLookup lookup = new EmojiUnicodeLookup();
    private final String blacklist;

    public EmojiContainer(String containerName, JsonObject data) {
        this.name = containerName;
        this.font = new FiguraIdentifier("emoji_" + containerName);
        this.blacklist = data.get("blacklist").getAsString();

        // key = emoji unicode, value = array of names
        for (Map.Entry<String, JsonElement> emoji : data.get("emojis").getAsJsonObject().entrySet()) {
            String curUnicode = emoji.getKey();
            JsonElement curValue = emoji.getValue();

            JsonArray namesArray = null;
            JsonArray shortcutsArray = null;
            if (curValue.isJsonArray()) {
                namesArray = curValue.getAsJsonArray();
            } else {
                JsonObject obj = curValue.getAsJsonObject();
                if (JsonUtils.validate(obj, JSON_KEY_NAMES, JsonElement::isJsonArray, ERROR_MSG, curUnicode.codePointAt(0), containerName, JSON_KEY_NAMES, "must be an array")) {
                    namesArray = obj.getAsJsonArray(JSON_KEY_NAMES);
                }

                if ((obj.has(JSON_KEY_FRAME_TIME) || obj.has(JSON_KEY_FRAME_TIME)) &&
                        JsonUtils.validate(obj, JSON_KEY_FRAMES, JsonElement::isJsonPrimitive, ERROR_MSG, curUnicode.codePointAt(0), containerName, JSON_KEY_FRAMES, "field must be an int") &&
                        JsonUtils.validate(obj, JSON_KEY_FRAME_TIME, JsonElement::isJsonPrimitive, ERROR_MSG, curUnicode.codePointAt(0), containerName, JSON_KEY_FRAME_TIME, "field must be an int")) {
                    lookup.putMetadata(curUnicode.codePointAt(0), new EmojiMetadata(obj));
                }

                if (obj.has(JSON_KEY_SHORTCUTS) && JsonUtils.validate(obj, JSON_KEY_SHORTCUTS, JsonElement::isJsonArray, ERROR_MSG, curUnicode.codePointAt(0), containerName, JSON_KEY_SHORTCUTS, "field must be an array")) {
                    shortcutsArray = obj.getAsJsonArray(JSON_KEY_SHORTCUTS);
                }
            }

            if (namesArray != null) {
                List<String> validAliases = new ArrayList<>();
                if (validateAliases(containerName, namesArray, validAliases::add)) {
                    String[] arr = new String[validAliases.size()];
                    lookup.putAliases(validAliases.toArray(arr), curUnicode);
                }
            }

            if (shortcutsArray != null) {
                List<String> validAliases = new ArrayList<>();
                if (validateAliases(containerName, shortcutsArray, validAliases::add)) {
                    String[] arr = new String[validAliases.size()];
                    lookup.putShortcuts(validAliases.toArray(arr), curUnicode);
                }
            }
        }
    }

    public EmojiUnicodeLookup getLookup() {
        return lookup;
    }

    private static boolean validateAliases(String containerName, JsonArray aliasArray, Consumer<String> consumer) {
        boolean atLeastOne = false;
        for (JsonElement element : aliasArray) {
            String alias = element.getAsString();
            if (alias.trim().isEmpty() || alias.indexOf(' ') != -1 || alias.indexOf(DELIMITER) != -1) {
                FiguraMod.LOGGER.warn("Invalid emoji name \"{}\" in container: {}", alias, containerName);
            } else {
                consumer.accept(alias);
                atLeastOne = true;
            }
        }
        return atLeastOne;
    }

    public void tickAnimations() {
        for (EmojiMetadata metadata : lookup.metadataValues()) {
            metadata.tickAnimation();
        }
    }

    public ITextComponent getEmojiComponent(String key) {
        return getEmojiComponent(key, new TextComponentString(DELIMITER + key + DELIMITER));
    }

    public ITextComponent getEmojiComponent(String key, ITextComponent hover) {
        String unicode = lookup.getUnicode(key);
        if (unicode == null)
            return null;
        return makeComponent(unicode, hover);
    }

    public ITextComponent getShortcutComponent(String shortcut) {
        String unicode = lookup.getUnicodeForShortcut(shortcut);
        if (unicode == null)
            return null;
        return makeComponent(unicode, new TextComponentString(shortcut));
    }

    private ITextComponent makeComponent(String unicode, ITextComponent hover) {
        return new TextComponentString(unicode).setStyle(((StyleExtension)STYLE).setFont(font).setHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover
                        .appendText("\n")
                        .appendSibling(new FiguraText("emoji." + name).setStyle(new Style().setColor(TextFormatting.DARK_GRAY))))
        ));
    }

    public ITextComponent blacklist(ITextComponent text) {
        if (blacklist.trim().isEmpty())
            return text;
        return TextUtils.replaceInText(text, "[" + blacklist + "]", TextUtils.UNKNOWN, (s, style) -> ((StyleExtension)style).getFont().equals(font), Integer.MAX_VALUE);
    }

    public ResourceLocation getFont() {
        return font;
    }
}