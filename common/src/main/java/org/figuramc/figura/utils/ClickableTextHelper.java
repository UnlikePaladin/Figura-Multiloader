package org.figuramc.figura.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import org.figuramc.figura.utils.ui.UIHelper;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.util.vector.Vector4f;

import java.util.*;

public class ClickableTextHelper {
    protected TextLine[] lines;
    protected HashMap<Vector4f, String> clickUrls = new HashMap<>();
    protected HashMap<Vector4f, ITextComponent> hoverText = new HashMap<>();

    protected boolean dirty = true;
    protected ITextComponent message;

    public void setMessage(@Nullable ITextComponent message) {
        if (this.message == message) return;
        this.message = message;
        if (message == null) {
            clear();
            return;
        }
        dirty = true;
    }

    public void renderDebug(Minecraft mc, int x, int y, int mouseX, int mouseY) {
        for (Vector4f area : hoverText.keySet()) {
            UIHelper.renderOutline((int) (x + area.x), (int) (y + area.y), (int) (area.z - area.x), (int) (area.w - area.y), isPointWithinBounds(area, x, y, mouseX, mouseY) ? 0xFF00FF00 : 0xFFFF00FF);
        }
        for (Vector4f area : clickUrls.keySet()) {
            UIHelper.renderOutline((int) (x + area.x), (int) (y + area.y), (int) (area.z - area.x), (int) (area.w - area.y), isPointWithinBounds(area, x, y, mouseX, mouseY) ? 0xFF00FF00 : 0xFFFF00FF);
        }
        UIHelper.renderOutline( mouseX-1, mouseY-1, 3, 3, 0xFF00FFFF);
    }

    public void update(FontRenderer font, int lineWidth) {
        if (!dirty || message == null) return;
        dirty = false;

        clear();

        List<TextLine> lines = new ArrayList<>();
        List<String> split = font.listFormattedStringToWidth(message.getFormattedText(), lineWidth);
        for (String curLine : split) {
            List<TextNode> nodes = new ArrayList<>();

            // Convert the Text into a list
            for (ITextComponent component : TextUtils.formattedTextToText(curLine)) {
                nodes.add(new TextNode(component.getUnformattedComponentText(), component.getStyle()));
            }

            lines.add(new TextLine(nodes.toArray(new TextNode[0])));
        }

        this.lines = lines.toArray(new TextLine[0]);

        // Finds all the hover/click events and stores them for later
        visit((a, style, x, y, w, h) -> {
            ClickEvent clickEvent = style.getClickEvent();
            HoverEvent hoverEvent = style.getHoverEvent();
            if (clickEvent == null && hoverEvent == null) return;

            // Calculates the bounding box of the text, for calculating if the mouse is hovering
            Vector4f rect = new Vector4f(x, y, x + w, y + h);

            if (clickEvent != null) {
                if (clickEvent.getAction() == ClickEvent.Action.OPEN_URL) {
                    clickUrls.put(rect, clickEvent.getValue());
                }
            }

            if (hoverEvent != null) {
                Object value = hoverEvent.getValue();
                if (value instanceof ITextComponent) {
                    ITextComponent component = (ITextComponent) value;
                    hoverText.put(rect, component);
                }
            }
        });
    }

    public @Nullable ITextComponent getHoverTooltip(int cx, int cy, int mouseX, int mouseY) {
        for (Vector4f area : hoverText.keySet()) {
            if (isPointWithinBounds(area, cx, cy, mouseX, mouseY)) {
                return hoverText.get(area);
            }
        }
        return null;
    }

    public @Nullable String getClickLink(int cx, int cy, int mouseX, int mouseY) {
        for (Vector4f area : clickUrls.keySet()) {
            if (isPointWithinBounds(area, cx, cy, mouseX, mouseY)) {
                return clickUrls.get(area);
            }
        }
        return null;
    }

    public int lineCount() {
        return lines == null ? 1 : lines.length;
    }

    private static boolean isPointWithinBounds(Vector4f area, int xOffset, int yOffset, int x, int y) {
        final int x1 = (int) (area.x + xOffset);
        final int y1 = (int) (area.y + yOffset);
        final int x2 = (int) (area.z + xOffset);
        final int y2 = (int) (area.w + yOffset);

        return x > x1 && x < x2 && y > y1 && y < y2;
    }

    public void visit(MultilineTextVisitor visitor) {
        if (lines == null) return;
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int y = 0;
        for (TextLine line : lines) {
            int x = 0;
            for (TextNode node : line.nodes) {
                int width = node.getWidth(font);
                visitor.visit(node.text, node.style, x, y, width, font.FONT_HEIGHT);
                x += width;
            }
            y += font.FONT_HEIGHT;
        }
    }

    public void clear() {
        lines = new TextLine[0];
        clickUrls.clear();
        hoverText.clear();
    }

    public void markDirty() {
        dirty = true;
    }

    @FunctionalInterface
    public interface MultilineTextVisitor {
        void visit(String text, Style style, int x, int y, int textWidth, int textHeight);
    }

    protected static final class TextLine {
        private final TextNode[] nodes;

        protected TextLine(TextNode[] nodes) {
            this.nodes = nodes;
        }

        public TextNode[] nodes() {
            return nodes;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            TextLine that = (TextLine) obj;
            return Arrays.equals(this.nodes, that.nodes);
        }

        @Override
        public int hashCode() {
            return Objects.hash((Object[]) nodes);
        }

        @Override
        public String toString() {
            return "TextLine[" +
                    "nodes=" + Arrays.toString(nodes) + ']';
        }
    }

    protected static final class TextNode {
        private final String text;
        private final Style style;

        protected TextNode(String text, Style style) {
            this.text = text;
            this.style = style;
        }

        public int getWidth(FontRenderer font) {
            return font.getStringWidth(asText().getFormattedText());
        }

        public ITextComponent asText() {
            return new TextComponentString(text).setStyle(style);
        }

        public String text() {
            return text;
        }

        public Style style() {
            return style;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            TextNode that = (TextNode) obj;
            return Objects.equals(this.text, that.text) &&
                    Objects.equals(this.style, that.style);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text, style);
        }

        @Override
        public String toString() {
            return "TextNode[" +
                    "text=" + text + ", " +
                    "style=" + style + ']';
        }

    }
}
