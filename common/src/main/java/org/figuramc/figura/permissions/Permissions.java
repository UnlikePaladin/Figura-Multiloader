package org.figuramc.figura.permissions;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import org.figuramc.figura.mixin.font.FontRendererAccessor;
import org.figuramc.figura.utils.ColorUtils;
import org.figuramc.figura.utils.FiguraText;

import java.util.Arrays;
import java.util.List;

public class Permissions {

    // default permissions
    public static final Permissions
            INIT_INST = new Permissions("INIT_INST", 0, 524287, 0, 32768, 65536, 262144, Integer.MAX_VALUE),
            WORLD_TICK_INST = new Permissions("WORLD_TICK_INST", 0, 32767, 0, 64, 128, 256, Integer.MAX_VALUE),
            TICK_INST = new Permissions("TICK_INST", 0, 65535, 0, 4096, 8192, 32768, Integer.MAX_VALUE),
            WORLD_RENDER_INST = new Permissions("WORLD_RENDER_INST", 0, 32767, 0, 32, 64, 256, Integer.MAX_VALUE),
            RENDER_INST = new Permissions("RENDER_INST", 0, 65535, 0, 4096, 8192, 32768, Integer.MAX_VALUE),
            COMPLEXITY = new Permissions("COMPLEXITY", 0, 8191, 0, 512, 2048, 4096, Integer.MAX_VALUE),
            PARTICLES = new Permissions("PARTICLES", 0, 127, 0, 20, 50, 100, Integer.MAX_VALUE),
            SOUNDS = new Permissions("SOUNDS", 0, 127, 0, 5, 20, 100, Integer.MAX_VALUE),
            VOLUME = new Permissions("VOLUME", 0, 99, 0, 100, 100, 100, 100) {
                @Override
                public boolean checkInfinity(int value) {
                    return false;
                }
            },
            BB_ANIMATIONS = new Permissions("BB_ANIMATIONS", 0, 511, 0, 32, 128, 256, Integer.MAX_VALUE),
            ANIMATION_INST = new Permissions("ANIMATION_INST", 0, 32767, 0, 2048, 4096, 8192, Integer.MAX_VALUE),
            TEXTURE_SIZE = new Permissions("TEXTURE_SIZE", 0, 2048, 64, 0, 128, 512, 2048, 2048),
            VANILLA_MODEL_EDIT = new Permissions("VANILLA_MODEL_EDIT", 0, 0, 1, 1, 1),
            NAMEPLATE_EDIT = new Permissions("NAMEPLATE_EDIT", 0, 0, 0, 1, 1),
            OFFSCREEN_RENDERING = new Permissions("OFFSCREEN_RENDERING", 0, 0, 0, 1, 1),
            // CUSTOM_SHADERS = new Permissions("CUSTOM_SHADERS", 0, 0, 1, 1, 1),
            CUSTOM_SOUNDS = new Permissions("CUSTOM_SOUNDS", 0, 0, 1, 1, 1),
            CANCEL_SOUNDS = new Permissions("CANCEL_SOUNDS", 0, 0, 0, 1, 1),
            CUSTOM_SKULL = new Permissions("CUSTOM_SKULL", 0, 0, 1, 1, 1),
            BUFFER_SIZE = new Permissions("BUFFER_SIZE", 0, 3072000, 0, 128000, 1024000, 2048000, Integer.MAX_VALUE),
            BUFFERS_COUNT = new Permissions("BUFFERS_COUNT", 0, 32, 0, 2, 4, 16, 32),
            NETWORKING = new Permissions("NETWORKING", 0,0,0,1,1),
            MAX_SOCKETS = new Permissions("MAX_SOCKETS_COUNT", 0, 16, 0, 1, 2, 8, 16);

    public static final List<Permissions> DEFAULT = Arrays.asList(
            INIT_INST,
            WORLD_TICK_INST,
            TICK_INST,
            WORLD_RENDER_INST,
            RENDER_INST,
            COMPLEXITY,
            PARTICLES,
            SOUNDS,
            VOLUME,
            BB_ANIMATIONS,
            ANIMATION_INST,
            TEXTURE_SIZE,
            VANILLA_MODEL_EDIT,
            NAMEPLATE_EDIT,
            OFFSCREEN_RENDERING,
            CUSTOM_SOUNDS,
            CANCEL_SOUNDS,
            CUSTOM_SKULL,
            BUFFER_SIZE,
            BUFFERS_COUNT,
            NETWORKING,
            MAX_SOCKETS
    );


    // -- permissions stuff -- // 


    // stuff
    public final String name;
    private final List<Integer> defaults;

    // toggle check
    public final boolean isToggle;

    // used only for sliders
    public final Integer min;
    public final Integer max;
    public final int stepSize;

    // toggle constructor
    public Permissions(String name, int blocked, int low, int def, int high, int max) {
        this(name, null, null, blocked, low, def, high, max);
    }

    // slider constructor
    public Permissions(String name, Integer sliderMin, Integer sliderMax, int blocked, int low, int def, int high, int max) {
        this(name, sliderMin, sliderMax, 1, blocked, low, def, high, max);
    }
    public Permissions(String name, Integer sliderMin, Integer sliderMax, int stepSize, int blocked, int low, int def, int high, int max) {
        this.name = name;
        this.isToggle = sliderMin == null || sliderMax == null;
        this.min = sliderMin;
        this.max = sliderMax;
        this.stepSize = stepSize;
        this.defaults = Arrays.asList(blocked, low, def, high, max);
    }

    // infinity check :p
    public boolean checkInfinity(int value) {
        return max != null && value > max;
    }

    // if this slider should show the steps
    public boolean showSteps() {
        return stepSize > 1;
    }

    // transform to boolean
    public boolean asBoolean(int value) {
        return value >= 1;
    }

    public int getDefault(Category category) {
        if (category.index >= 0 && category.index < defaults.size())
            return defaults.get(category.index);
        return -1;
    }

    public enum Category {
        BLOCKED(0, TextFormatting.RED),
        LOW(1, TextFormatting.YELLOW),
        DEFAULT(2, TextFormatting.WHITE),
        HIGH(3, TextFormatting.GREEN),
        MAX(4, ColorUtils.Colors.LUA_PING);

        public final int index;
        public final int color;
        public final ITextComponent text, info;

        Category(int index, ColorUtils.Colors color) {
            this(index, color.hex, color.style);
        }

        Category(int index, TextFormatting formatting) {
            this(index, ((FontRendererAccessor)Minecraft.getMinecraft().fontRenderer).getColors()[formatting.getColorIndex()], new Style().setColor(formatting));
        }

        Category(int index, int color, Style style) {
            this.index = index;
            this.color = color;
            String name = "permissions.category." + name().toLowerCase();
            text = new FiguraText(name).setStyle(style);
            info = new FiguraText(name + ".info");
        }

        public static Category indexOf(int i) {
            for (Category value : values()) {
                if (value.index == i)
                    return value;
            }

            return null;
        }
    }
}
