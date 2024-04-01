package org.figuramc.figura.lua.docs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.item.EnumAction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.animation.Animation;
import org.figuramc.figura.commands.FiguraCommands;
import org.figuramc.figura.mixin.input.KeyBindingAccessor;
import org.figuramc.figura.mixin.render.EntityRendererAccessor;
import org.figuramc.figura.model.ParentType;
import org.figuramc.figura.model.rendering.EntityRenderMode;
import org.figuramc.figura.model.rendering.texture.FiguraTextureSet;
import org.figuramc.figura.model.rendering.texture.RenderTypes;
import org.figuramc.figura.utils.ColorUtils;
import org.figuramc.figura.utils.FiguraClientCommandSource;
import org.figuramc.figura.utils.FiguraText;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

/**
 * Adds docs for functions that have specific set of String names.
 */
public class FiguraListDocs {

    // -- types --//

    public static final LinkedHashSet<String> KEYBINDS = new LinkedHashSet<>();
    private static final LinkedHashMap<String, List<String>> PARENT_TYPES = new LinkedHashMap<String, List<String>>() {{
        for (ParentType value : ParentType.values())
            put(value.name(), Arrays.asList(value.aliases));
    }};
    private static final LinkedHashMap<String, List<String>> STRING_ENCODINGS = new LinkedHashMap<String, List<String>>() {{
        put("utf8", Collections.singletonList("utf_8"));
        put("utf16", Collections.singletonList("utf_16"));
        put("utf16be", Collections.singletonList("utf_16_be"));
        put("utf16le", Collections.singletonList("utf_16_le"));
        put("ascii", Collections.emptyList());
        put("iso88591", Collections.singletonList("iso_8859_1"));
    }};
    private static final LinkedHashSet<String> RENDER_TYPES = new LinkedHashSet<String>() {{
        for (RenderTypes value : RenderTypes.values())
            add(value.name());
    }};
    private static final LinkedHashSet<String> TEXTURE_TYPES = new LinkedHashSet<String>() {{
        for (FiguraTextureSet.OverrideType value : FiguraTextureSet.OverrideType.values())
            add(value.name());
    }};
    private static final LinkedHashSet<String> ENTITY_POSES = new LinkedHashSet<String>() {{
        String[] poses = new String[] {
                "STANDING",
                "FALL_FLYING",
                "SLEEPING",
                "SWIMMING",
                "SPIN_ATTACK",
                "CROUCHING",
                "DYING"
        };
        this.addAll(Arrays.asList(poses));
    }};
    private static final LinkedHashSet<String> ITEM_DISPLAY_MODES = new LinkedHashSet<String>() {{
        for (ItemCameraTransforms.TransformType value : ItemCameraTransforms.TransformType.values())
            add(value.name());
    }};
    private static final LinkedHashSet<String> POST_EFFECTS = new LinkedHashSet<String>() {{
        for (ResourceLocation effect : EntityRendererAccessor.getEffects()) {
            String[] split = effect.getResourcePath().split("/");
            String name = split[split.length - 1];
            add(name.split("\\.")[0]);
        }
    }};
    private static final LinkedHashSet<String> PLAY_STATES = new LinkedHashSet<String>() {{
        for (Animation.PlayState value : Animation.PlayState.values())
            add(value.name());
    }};
    private static final LinkedHashSet<String> LOOP_MODES = new LinkedHashSet<String>() {{
        for (Animation.LoopMode value : Animation.LoopMode.values())
            add(value.name());
    }};
    private static final LinkedHashMap<String, List<String>> COLORS = new LinkedHashMap<String, List<String>>() {{
        for (ColorUtils.Colors value : ColorUtils.Colors.values())
            put(value.name(), Collections.singletonList(value.name()));
    }};
    private static final LinkedHashSet<String> PLAYER_MODEL_PARTS = new LinkedHashSet<String>() {{
        for (EnumPlayerModelParts value : EnumPlayerModelParts.values()) {
            String name = value.name();
            add(name.endsWith("_LEG") ? name.substring(0, name.length() - 4) : name);
        }
    }};
    private static final LinkedHashSet<String> USE_ACTIONS = new LinkedHashSet<String>() {{
        for (EnumAction value : EnumAction.values())
            add(value.name());
        add("SPEAR");
        add("CROSSBOW");
    }};
    private static final LinkedHashSet<String> RENDER_MODES = new LinkedHashSet<String>() {{
        for (EntityRenderMode value : EntityRenderMode.values())
            add(value.name());
    }};
    private static final LinkedHashSet<String> BLOCK_RAYCAST_TYPE = new LinkedHashSet<String>() {{
        add("COLLIDER");
        add("OUTLINE");
        add("VISUAL");
    }};
    private static final LinkedHashSet<String> FLUID_RAYCAST_TYPE = new LinkedHashSet<String>() {{
        add("NONE");
        add("SOURCE");
        add("ANY");
    }};

    private static final LinkedHashSet<String> HEIGHTMAP_TYPE = new LinkedHashSet<String>() {{
        String[] types = new String[]{
                "WORLD_SURFACE_WG",
                "WORLD_SURFACE",
                "OCEAN_FLOOR_WG",
                "OCEAN_FLOOR",
                "MOTION_BLOCKING",
                "MOTION_BLOCKING_NO_LEAVES"
        };
        this.addAll(Arrays.asList(types));
    }};
    private static final LinkedHashSet<String> REGISTRIES = new LinkedHashSet<String>() {{
        String[] registries = new String[] {
                "sound_event",
                "fluid",
                "mob_effect",
                "block",
                "enchantment",
                "entity_type",
                "item",
                "potion",
                "particle_type",
                "block_entity_type",
                "motive",
                "custom_stat",
                "chunk_status",
                "rule_test",
                "pos_rule_test",
                "menu",
                "recipe_type",
                "recipe_serializer",
                "attribute",
                "stat_type",
                "villager_type",
                "villager_profession",
                "point_of_interest_type",
                "memory_module_type",
                "sensor_type",
                "schedule",
                "activity",
                "loot_pool_entry_type",
                "loot_function_type",
                "loot_condition_type",
                "worldgen/surface_builder",
                "worldgen/carver",
                "worldgen/feature",
                "worldgen/structure_feature",
                "worldgen/structure_piece",
                "worldgen/decorator",
                "worldgen/block_state_provider_type",
                "worldgen/block_placer_type",
                "worldgen/foliage_placer_type",
                "worldgen/trunk_placer_type",
                "worldgen/tree_decorator_type",
                "worldgen/feature_size_type",
                "worldgen/biome_source",
                "worldgen/chunk_generator",
                "worldgen/structure_processor",
                "worldgen/structure_pool_element"
        };

        this.addAll(Arrays.asList(registries));
    }};

    private enum ListDoc {
        KEYBINDS(() -> FiguraListDocs.KEYBINDS, "Keybinds", "keybinds", 2),
        PARENT_TYPES(() -> FiguraListDocs.PARENT_TYPES, "ParentTypes", "parent_types", 1),
        RENDER_TYPES(() -> FiguraListDocs.RENDER_TYPES, "RenderTypes", "render_types", 1),
        TEXTURE_TYPES(() -> FiguraListDocs.TEXTURE_TYPES, "TextureTypes", "texture_types", 1),
        KEY_IDS(() -> new LinkedHashSet<String>() {{this.addAll(KeyBindingAccessor.getAll().keySet());}}, "KeyIDs", "key_ids", 2),
        ENTITY_POSES(() -> FiguraListDocs.ENTITY_POSES, "EntityPoses", "entity_poses", 2),
        ITEM_RENDER_TYPES(() -> FiguraListDocs.ITEM_DISPLAY_MODES, "ItemDisplayModes", "item_display_modes", 1),
        POST_EFFECTS(() -> FiguraListDocs.POST_EFFECTS, "PostEffects", "post_effects", 2),
        PLAY_STATES(() -> FiguraListDocs.PLAY_STATES, "PlayStates", "play_states", 1),
        LOOP_MODES(() -> FiguraListDocs.LOOP_MODES, "LoopModes", "loop_modes", 1),
        COLORS(() -> FiguraListDocs.COLORS, "Colors", "colors", 1),
        PLAYER_MODEL_PARTS(() -> FiguraListDocs.PLAYER_MODEL_PARTS, "PlayerModelParts", "player_model_parts", 1),
        USE_ACTIONS(() -> FiguraListDocs.USE_ACTIONS, "UseActions", "use_actions", 1),
        RENDER_MODES(() -> FiguraListDocs.RENDER_MODES, "RenderModes", "render_modes", 1),
        STRING_ENCODINGS(() -> FiguraListDocs.STRING_ENCODINGS, "StringEncodings", "string_encodings", 1),
        BLOCK_RAYCAST_TYPE(() -> FiguraListDocs.BLOCK_RAYCAST_TYPE, "BlockRaycastTypes", "block_raycast_types", 1),
        FLUID_RAYCAST_TYPE(() -> FiguraListDocs.FLUID_RAYCAST_TYPE, "FluidRaycastTypes", "fluid_raycast_types", 1),
        HEIGHTMAP_TYPE(() -> FiguraListDocs.HEIGHTMAP_TYPE, "HeightmapTypes", "heightmap_types", 1),
        REGISTRIES(() -> FiguraListDocs.REGISTRIES, "Registries", "registries", 1);

        private final Supplier<Object> supplier;
        private final String name, id;
        private final int split;

        ListDoc(Supplier<Object> supplier, String name, String id, int split) {
            this.supplier = supplier;
            this.name = name;
            this.id = id;
            this.split = split;
        }

        private Collection<?> get() {
            Object obj = supplier.get();
            if (obj instanceof LinkedHashSet<?>) {
                LinkedHashSet<?> set = (LinkedHashSet<?>) obj;
                return set;
            } else if (obj instanceof Map<?, ?>) {
                Map<?, ?> map = (Map<?, ?>) obj;
                return map.entrySet();
            } else
                throw new UnsupportedOperationException("Invalid object " + obj);
        }

        private JsonElement generateJson(boolean translate) {
            JsonObject object = new JsonObject();

            // list properties
            object.addProperty("name", name);
            object.addProperty("description", translate ? new FiguraText("docs.enum." + id).getFormattedText() : FiguraMod.MOD_ID + "." + "docs.enum." + id);

            // list entries
            Collection<?> coll = get();
            if (coll.size() == 0)
                return object;

            JsonArray entries = new JsonArray();
            for (Object o : coll) {
                if (o instanceof Map.Entry) {
                    Map.Entry e = (Map.Entry) o;
                    entries.add(e.getKey().toString());
                    for (String s : (List<String>) e.getValue())
                        entries.add(s);
                } else {
                    entries.add(o.toString());
                }
            }

            object.add("entries", entries);
            return object;
        }

        private class FiguraEntrySubCommand extends FiguraCommands.FiguraSubCommand {

            Object entryObj;
            Map<String, FiguraCommands.FiguraSubCommand> childEntries = new HashMap<>();
            public FiguraEntrySubCommand(Object entryObj, String name) {
                super(name);
                this.entryObj = entryObj;

                if (entryObj instanceof Map.Entry) {
                    Map.Entry e = (Map.Entry) entryObj;
                    for (String s : (List<String>) e.getValue()) {
                        FiguraCommands.FiguraSubCommand child = new FiguraCommands.FiguraSubCommand(s) {
                            @Override
                            public void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] args) throws CommandException {
                                FiguraMod.sendChatMessage(new TextComponentString(getName()).setStyle(ColorUtils.Colors.AWESOME_BLUE.style));
                            }
                        };
                        childEntries.put(s, child);
                    }
                }
            }

            @Override
            public void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] args) throws CommandException {
                if (args.length == 0)
                    FiguraMod.sendChatMessage(new TextComponentString(name).setStyle(ColorUtils.Colors.AWESOME_BLUE.style));
                else if (childEntries.containsKey(args[0]))
                    childEntries.get(args[0]).execute(minecraftServer, iCommandSender, Arrays.copyOfRange(args, 1, args.length));
            }

            @Override
            public List<String> getTabCompletions(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] strings, @Nullable BlockPos targetPos) {
                if (strings.length > 0 && childEntries.containsKey(strings[0]))
                    return childEntries.get(strings[0]).getTabCompletions(minecraftServer, iCommandSender, Arrays.copyOfRange(strings, 1, strings.length), targetPos);

                return CommandBase.getListOfStringsMatchingLastWord(strings, childEntries.keySet());
            }
        }

        public class FiguraListDocSubCommand extends FiguraCommands.FiguraSubCommand {

            Map<String, FiguraCommands.FiguraSubCommand> childEntries = new HashMap<>();
            public FiguraListDocSubCommand() {
                super(id);

                Collection<?> coll = get();
                // add collection as child for easy navigation
                for (Object o : coll) {
                    String text = o instanceof Map.Entry ? ((Map.Entry) o).getKey().toString() : o.toString();
                    FiguraEntrySubCommand entrySubCommand = new FiguraEntrySubCommand(o, text);
                    childEntries.put(text, entrySubCommand);
                }
            }

            @Override
            public void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] args) throws CommandException {
                Collection<?> coll = get();
                if (coll.size() == 0) {
                    FiguraMod.sendChatMessage(new FiguraText("docs.enum.empty"));
                    return;
                } else if (args.length > 0 && childEntries.containsKey(args[0])){
                    childEntries.get(args[0]).execute(minecraftServer, iCommandSender, Arrays.copyOfRange(args, 1, args.length));
                    return;
                }

                ITextComponent text = FiguraDoc.HEADER.createCopy()
                        .appendText("\n\n")
                        .appendSibling(new TextComponentString("• ")
                                .appendSibling(new FiguraText("docs.text.description"))
                                .appendText(":")
                                .setStyle(ColorUtils.Colors.PURPLE.style))
                        .appendText("\n\t")
                        .appendSibling(new TextComponentString("• ")
                                .appendSibling(new FiguraText("docs.enum." + id))
                                .setStyle(ColorUtils.Colors.BLUE.style))
                        .appendText("\n\n")
                        .appendSibling(new TextComponentString("• ")
                                .appendSibling(new FiguraText("docs.text.entries"))
                                .appendText(":")
                                .setStyle(ColorUtils.Colors.PURPLE.style));

                int i = 0;
                for (Object o : coll) {
                    ITextComponent component;

                    if (o instanceof Map.Entry) {
                        Map.Entry e = (Map.Entry) o;
                        component = new TextComponentString(e.getKey().toString()).setStyle(new Style().setColor(TextFormatting.WHITE));
                        for (String s : (List<String>) e.getValue()) {
                            component.appendSibling(new TextComponentString(" | ").setStyle(new Style().setColor(TextFormatting.YELLOW)))
                                    .appendSibling(new TextComponentString(s).setStyle(new Style().setColor(TextFormatting.GRAY)));
                        }
                    } else {
                        component = new TextComponentString(o.toString()).setStyle(new Style().setColor(TextFormatting.WHITE));
                    }

                    text.appendText(i % split == 0 ? "\n\t" : "\t");
                    text.appendSibling(new TextComponentString("• ").setStyle(new Style().setColor(TextFormatting.YELLOW))).appendSibling(component);
                    i++;
                }

                FiguraMod.sendChatMessage(text);
            }

            @Override
            public List<String> getTabCompletions(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] strings, @Nullable BlockPos targetPos) {
                if (strings.length > 0 && childEntries.containsKey(strings[0]))
                    return childEntries.get(strings[0]).getTabCompletions(minecraftServer, iCommandSender, Arrays.copyOfRange(strings, 1, strings.length), targetPos);

                return CommandBase.getListOfStringsMatchingLastWord(strings, childEntries.keySet());
            }
        }

        private FiguraCommands.FiguraSubCommand generateCommand() {
            // command
            return new FiguraListDocSubCommand();
            // return
        }
    }

    // -- doc methods -- //

    public static FiguraCommands.FiguraSubCommand getCommand() {
        // self
        return new FiguraListDocsSubCommand();
    }

    public static class FiguraListDocsSubCommand extends FiguraCommands.FiguraSubCommand {

        private Map<String, FiguraCommands.FiguraSubCommand> figuraListDocSubCommandMap = new HashMap<>();
        public FiguraListDocsSubCommand() {
            super("enums");
            for (ListDoc value : ListDoc.values())
                figuraListDocSubCommandMap.put(value.id, value.generateCommand());
        }

        @Override
        public void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] args) throws CommandException {
            if (args.length == 0)  {
                FiguraMod.sendChatMessage(FiguraDoc.HEADER.createCopy()
                        .appendText("\n\n")
                        .appendSibling(new TextComponentString("• ")
                                .appendSibling(new FiguraText("docs.text.type"))
                                .appendText(":")
                                .setStyle(ColorUtils.Colors.PURPLE.style))
                        .appendText("\n\t")
                        .appendSibling(new TextComponentString("• ")
                                .appendSibling(new TextComponentString("enumerators"))
                                .setStyle(ColorUtils.Colors.BLUE.style))

                        .appendText("\n\n")
                        .appendSibling(new TextComponentString("• ")
                                .appendSibling(new FiguraText("docs.text.description"))
                                .appendText(":")
                                .setStyle(ColorUtils.Colors.PURPLE.style))
                        .appendText("\n\t")
                        .appendSibling(new TextComponentString("• ")
                                .appendSibling(new FiguraText("docs.enum"))
                                .setStyle(ColorUtils.Colors.BLUE.style))
                );
            } else if (figuraListDocSubCommandMap.containsKey(args[0])) {
                figuraListDocSubCommandMap.get(args[0]).execute(minecraftServer, iCommandSender, Arrays.copyOfRange(args, 1 , args.length));
            }
        }

        @Override
        public List<String> getTabCompletions(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] strings, @Nullable BlockPos targetPos) {
            if (strings.length > 0 && figuraListDocSubCommandMap.containsKey(strings[0]))
                return figuraListDocSubCommandMap.get(strings[0]).getTabCompletions(minecraftServer, iCommandSender, Arrays.copyOfRange(strings, 1, strings.length), targetPos);
            return CommandBase.getListOfStringsMatchingLastWord(strings, figuraListDocSubCommandMap.keySet());
        }
    }

    public static List<String> getEnumValues(String enumName) {
        try {
            ListDoc enumListDoc = ListDoc.valueOf(enumName.toUpperCase());

            Collection<?> enumValues = enumListDoc.get();
            List<String> enumValueList = new ArrayList<>();
            for (Object value : enumValues) {
                if (value instanceof Map.Entry<?, ?>) {
                    Map.Entry<?, ?> entry = (Map.Entry<?, ?>) value;
                    enumValueList.add(entry.getKey().toString());
                    if (entry.getValue() instanceof Collection<?>) {
                        for (Object alias : (Collection<?>) entry.getValue()) {
                            enumValueList.add(alias.toString());
                        }
                    }
                } else {
                    enumValueList.add(value.toString());
                }
            }

            return enumValueList;
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Enum " + enumName + " does not exist");
        }
    }

    public static JsonElement toJson(boolean translate) {
        JsonArray array = new JsonArray();
        for (ListDoc value : ListDoc.values())
            array.add(value.generateJson(translate));
        return array;
    }
}
