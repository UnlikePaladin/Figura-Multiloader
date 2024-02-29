package org.figuramc.figura.utils;

import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.*;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import org.figuramc.figura.ducks.extensions.StyleExtension;
import org.figuramc.figura.mixin.font.TextFormattingAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public class TextUtils {

    public static final ITextComponent TAB = new FiguraText("tab");
    public static final ITextComponent ELLIPSIS = new FiguraText("ellipsis");
    public static final ITextComponent UNKNOWN = new TextComponentString("�").setStyle(new Style().withFont(Style.DEFAULT_FONT));

    public static boolean allowScriptEvents;

    public static List<ITextComponent> splitText(ITextComponent text, String regex) {
        // list to return
        ArrayList<ITextComponent> textList = new ArrayList<>();

        // current line variable
        ITextComponent[] currentText = {new TextComponentString("")};

        // iterate over the text

        List<ITextComponent> components = new ArrayList<>();
        components.add(text);
        components.addAll(text.getSiblings());

        for (ITextComponent textComponent : components) {
            String string = textComponent.getUnformattedComponentText();
            Style style = textComponent.getStyle();
            // split text based on regex
            String[] lines = string.split(regex, -1);

            // iterate over the split text
            for (int i = 0; i < lines.length; i++) {
                // if it is not the first iteration, add to return list and reset the line variable
                if (i != 0) {
                    textList.add(currentText[0].createCopy());
                    currentText[0] = new TextComponentString("");
                }

                // append text with the line text
                currentText[0].appendSibling(new TextComponentString(lines[i]).setStyle(style));
            }
        }

        // add the last text iteration then return
        textList.add(currentText[0]);
        return textList;
    }

    public static ITextComponent removeClickableObjects(ITextComponent text) {
        return removeClickableObjects(text, p -> true);
    }

    public static ITextComponent removeClickableObjects(ITextComponent text, Predicate<ClickEvent> pred) {
        ITextComponent ret = new TextComponentString("");

        List<ITextComponent> components = new ArrayList<>();
        components.add(text);
        components.addAll(text.getSiblings());

        for (ITextComponent textComponent : components) {
            String string = textComponent.getUnformattedComponentText();
            Style style = textComponent.getStyle();
            ret.appendSibling(new TextComponentString(string).setStyle(style.getClickEvent() != null && pred.test(style.getClickEvent()) ? style.setClickEvent(null) : style));
        }
        return ret;
    }

    private static JsonParser parser = new JsonParser();
    public static ITextComponent tryParseJson(String text) {
        if (text == null)
            return new TextComponentString("");

        // text to return
        ITextComponent finalText;

        try {
            // check if its valid json text
            parser.parse(text);

            // attempt to parse json
            finalText = ITextComponent.Serializer.fromJsonLenient(text);

            // if failed, throw a dummy exception
            if (finalText == null)
                throw new Exception("Error parsing JSON string");
        } catch (Exception ignored) {
            // on any exception, make the text as-is
            finalText = new TextComponentString(text);
        }

        // return text
        return finalText;
    }

    public static ITextComponent replaceInText(ITextComponent text, String regex, Object replacement) {
        return replaceInText(text, regex, replacement, (s, style) -> true, Integer.MAX_VALUE);
    }

    public static ITextComponent replaceInText(ITextComponent text, String regex, Object replacement, BiPredicate<String, Style> predicate, int times) {
        return replaceInText(text, regex, replacement, predicate, 0, times);
    }

    public static ITextComponent replaceInText(ITextComponent text, String regex, Object replacement, BiPredicate<String, Style> predicate, int beginIndex, int times) {
        // fix replacement object
        ITextComponent replace = replacement instanceof ITextComponent ? (ITextComponent) replacement : new TextComponentString(replacement.toString());
        ITextComponent ret = new TextComponentString("");

        int[] ints = {beginIndex, times};
        List<ITextComponent> components = new ArrayList<>();
        components.add(text);
        components.addAll(text.getSiblings());

        for (ITextComponent textComponent : components) {
            String string = textComponent.getUnformattedComponentText();
            Style style = textComponent.getStyle();
            // test predicate

            // test predicate
            if (!predicate.test(string, style)) {
                ret.appendSibling(new TextComponentString(string).setStyle(style));
            }

            // split
            String[] split = string.split("((?<=" + regex + ")|(?=" + regex + "))");
            for (String s : split) {
                if (!s.matches(regex)) {
                    ret.appendSibling(new TextComponentString(s).setStyle(style));
                    continue;
                }

                if (ints[0] > 0 || ints[1] <= 0) {
                    ret.appendSibling(new TextComponentString(s).setStyle(style));
                } else {
                    ret.appendSibling(new TextComponentString("").setStyle(style).appendSibling(replace));
                }

                ints[0]--;
                ints[1]--;
            }
        }
        return ret;
    }

    public static ITextComponent trimToWidthEllipsis(FontRenderer font, ITextComponent text, int width, ITextComponent ellipsis) {
        // return text without changes if it is not larger than width
        if (font.getStringWidth(text.getUnformattedText()) <= width)
            return text;

        // add ellipsis
        return addEllipsis(font, text, width, ellipsis);
    }

    public static ITextComponent addEllipsis(FontRenderer font, ITextComponent text, int width, ITextComponent ellipsis) {
        // trim with the ellipsis size and return the modified text
        ITextComponent trimmed = font.substrByWidth(text, width - font.width(ellipsis));
        return formattedTextToText(trimmed).copy().append(ellipsis);
    }

    public static ITextComponent replaceTabs(ITextComponent text) {
        return TextUtils.replaceInText(text, "\\t", TAB);
    }

    public static List<ITextComponent> wrapTooltip(ITextComponent text, FontRenderer font, int mousePos, int screenWidth, int offset) {
        // first split the new line text
        List<ITextComponent> splitText = TextUtils.splitText(text, "\n");

        // get the possible tooltip width
        int left = mousePos - offset;
        int right = screenWidth - mousePos - offset;

        // get largest text size
        int largest = getWidth(splitText, font);

        // get the optimal side for warping
        int side = largest <= right ? right : largest <= left ? left : Math.max(left, right);

        // warp the unmodified text
        return wrapText(text, side, font);
    }

    // get the largest text width from a list
    public static int getWidth(List<?> text, FontRenderer font) {
        int width = 0;

        for (Object object : text) {
            int w;
            if (object instanceof ITextComponent) // instanceof switch case only for java 17 experimental ;-;
            {
                ITextComponent component = (ITextComponent) object;
                w = font.getStringWidth(component.getFormattedText());
            } else if (object instanceof String) {
                String s = (String) object;
                w = font.getStringWidth(s);
            } else
                w = 0;

            width = Math.max(width, w);
        }
        return width;
    }

    // correctly calculates the height of a list of text componennts
    public static int getHeight(List<?> text, FontRenderer font, int lineSpaceing) {
        int lines = text.size();
        return (lines * font.FONT_HEIGHT) + Math.max((lines-1)*lineSpaceing, 0);
    }

    public static int getHeight(List<?> text, FontRenderer font) {
        return getHeight(text, font, 1);
    }

    public static ITextComponent replaceStyle(ITextComponent text, Style newStyle, Predicate<Style> predicate) {
        ITextComponent ret = new TextComponentString("");
        List<ITextComponent> components = new ArrayList<>();
        components.add(text);
        components.addAll(text.getSiblings());
        for (ITextComponent textComponent : components) {
            String string = textComponent.getUnformattedComponentText();
            Style style = textComponent.getStyle();
            ret.appendSibling(new TextComponentString(string).setStyle(predicate.test(style) ? ((StyleExtension)newStyle).applyStyleToStyle(style) : style));
        }
        return ret;
    }

    public static ITextComponent setStyleAtWidth(ITextComponent text, int width, FontRenderer font, Style newStyle) {
        ITextComponent ret = new TextComponentString("");
        List<ITextComponent> components = new ArrayList<>();
        components.add(text);
        components.addAll(text.getSiblings());
        for (ITextComponent textComponent : components) {
            String string = textComponent.getUnformattedComponentText();
            Style style = textComponent.getStyle();
            ITextComponent current = new TextComponentString(string).setStyle(style);

            int prevWidth = font.getStringWidth(ret.getFormattedText());
            int currentWidth = font.getStringWidth(current.getFormattedText());
            if (prevWidth <= width && prevWidth + currentWidth > width)
                current.setStyle(newStyle);

            ret.appendSibling(current);
        }
        return ret;
    }

    public static List<ITextComponent> wrapText(ITextComponent text, int width, FontRenderer font) {
        List<ITextComponent> warp = new ArrayList<>();
        font.getSplitter().splitLines(text, width, Style.EMPTY, (formattedText, aBoolean) -> warp.add(Language.getInstance().getVisualOrder(formattedText)));
        return warp;
    }

    public static ITextComponent charSequenceToText(ITextComponent charSequence) {
        ITextComponent builder = new TextComponentString("");
        StringBuilder buffer = new StringBuilder();
        Style[] lastStyle = new Style[1];

        charSequence.accept((index, style, codePoint) -> {
            if (!style.equals(lastStyle[0])) {
                if (buffer.length() > 0) {
                    builder.append(new TextComponent(buffer.toString()).withStyle(lastStyle[0]));
                    buffer.setLength(0);
                }
                lastStyle[0] = style;
            }

            buffer.append(Character.toChars(codePoint));
            return true;
        });

        if (buffer.length() > 0)
            builder.append(new TextComponent(buffer.toString()).withStyle(lastStyle[0]));

        return builder;
    }

    public static ITextComponent formattedTextToText(ITextComponent formattedText) {
        if (formattedText instanceof ITextComponent) {
            ITextComponent c = (ITextComponent) formattedText;
            return c;
        }

        ITextComponent builder = new TextComponentString("");

        String string = formattedText.getUnformattedComponentText();
        Style style = formattedText.getStyle();
        builder.appendSibling(new TextComponentString(string).setStyle(style));

        return builder;
    }

    public static ITextComponent substring(ITextComponent text, int beginIndex, int endIndex) {
        StringBuilder counter = new StringBuilder();
        ITextComponent builder = new TextComponentString("");

        String string = text.getUnformattedComponentText();
        Style style = text.getStyle();
        int index = counter.length();
        int len = string.length();

        if (index <= endIndex && index + len >= beginIndex) {
            int sub = Math.max(beginIndex - index, 0);
            int top = Math.min(endIndex - index, len);
            builder.appendSibling(new TextComponentString(string.substring(sub, top)).setStyle(style));
        }

        counter.append(string);

        return builder;
    }

    public static ITextComponent parseLegacyFormatting(ITextComponent text) {
        ITextComponent builder = new TextComponentString("");

        String string = text.getUnformattedComponentText();
        Style style = text.getStyle();


        formatting: {
            // check for the string have the formatting char
            if (!string.contains("§"))
                break formatting;

            // split the string at the special char
            String[] split = string.split("§");
            if (split.length < 2)
                break formatting;

            // creates a new text with the left part of the string
            ITextComponent newText = new TextComponentString(split[0]).setStyle(style);

            // if right part has text
            for (int i = 1; i < split.length; i++) {
                String s = split[i];

                if (s.length() == 0)
                    continue;

                // get the formatting code and apply to the style
                TextFormatting formatting = getByCode(s.charAt(0));
                if (formatting != null)
                    style = style.applyLegacyFormat(formatting);

                // create right text, and yeet the formatting code
                newText.append(new TextComponent(s.substring(1)).withStyle(style));
            }

            builder.append(newText);
            return Optional.empty();
        }

        builder.append(new TextComponent(string).withStyle(style));

        return builder;
    }

    public static ITextComponent reverse(ITextComponent text) {
        ITextComponent[] builder = {new TextComponentString("")};
        String string = text.getUnformattedComponentText();
        Style style = text.getStyle();

        StringBuilder str = new StringBuilder(string).reverse();
        builder[0] = new TextComponentString(str.toString()).setStyle(style).appendSibling(builder[0]);

        return builder[0];
    }

    public static ITextComponent trim(ITextComponent text) {
        String string = text.getFormattedText();
        int start = 0;
        int end = string.length();

        // trim
        while (start < end && string.charAt(start) <= ' ')
            start++;
        while (start < end && string.charAt(end - 1) <= ' ')
            end--;

        // apply trim
        return substring(text, start, end);
    }

    public static List<ITextComponent> formatInBounds(ITextComponent text, FontRenderer font, int maxWidth, boolean wrap) {
        if (maxWidth > 0) {
            if (wrap) {
                List<FormattedCharSequence> warped = wrapText(text, maxWidth, font);
                List<Component> newList = new ArrayList<>();
                for (FormattedCharSequence charSequence : warped)
                    newList.add(charSequenceToText(charSequence));
                return newList;
            } else {
                List<Component> list = splitText(text, "\n");
                List<Component> newList = new ArrayList<>();
                for (Component component : list)
                    newList.add(formattedTextToText(font.substrByWidth(component, maxWidth)));
                return newList;
            }
        } else {
            return splitText(text, "\n");
        }
    }

    public enum Alignment {
        LEFT((font, component) -> 0, i -> 0),
        RIGHT((font, component) -> font.getStringWidth(component.getFormattedText()), i -> i),
        CENTER((font, component) -> font.getStringWidth(component.getFormattedText()) / 2, i -> i / 2);

        private final BiFunction<FontRenderer, ITextComponent, Integer> textFunction;
        private final Function<Integer, Integer> integerFunction;

        Alignment(BiFunction<FontRenderer, ITextComponent, Integer> textFunction, Function<Integer, Integer> integerFunction) {
            this.textFunction = textFunction;
            this.integerFunction = integerFunction;
        }

        public int apply(FontRenderer font, ITextComponent component) {
            return textFunction.apply(font, component);
        }

        public int apply(int width) {
            return integerFunction.apply(width);
        }
    }

    public static class FiguraClickEvent extends ClickEvent {
        public final Runnable onClick;
        public FiguraClickEvent(Runnable onClick) {
            super(Action.SUGGEST_COMMAND, "");
            this.onClick = onClick;
        }
    }

    public static TextFormatting getByCode(char formattingCode) {
        char d = Character.toString(formattingCode).toLowerCase(Locale.ROOT).charAt(0);
        for (TextFormatting lv : TextFormatting.values()) {
            if (((TextFormattingAccessor)(Object)lv).getFormattingCode() != d) continue;
            return lv;
        }
        return null;
    }
}
