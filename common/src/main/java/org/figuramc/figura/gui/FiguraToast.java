package org.figuramc.figura.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.toasts.GuiToast;
import net.minecraft.client.gui.toasts.IToast;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.ducks.extensions.StyleExtension;
import org.figuramc.figura.utils.ColorUtils;
import org.figuramc.figura.utils.FiguraIdentifier;
import org.figuramc.figura.utils.ui.UIHelper;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class FiguraToast implements IToast {

    private final ToastType type;
    private ITextComponent title, message;

    private long startTime;
    private boolean update;

    public FiguraToast(ITextComponent title, ITextComponent message, ToastType type) {
        this.type = type;
        update(title, message, false);
    }

    public void update(ITextComponent title, ITextComponent message, boolean update) {
        this.title = new TextComponentString("").setStyle(type.style).appendSibling(title);
        this.message = message;
        this.update = update;
    }

    @Override
    public IToast.Visibility draw(GuiToast guiToast, long startTime) {
        int time = Math.round(Configs.TOAST_TIME.value * 1000);
        int titleTime = Math.round(Configs.TOAST_TITLE_TIME.value * 1000);

        if (this.update) {
            if (startTime - this.startTime < time)
                Visibility.SHOW.playSound(guiToast.getMinecraft().getSoundHandler());
            this.startTime = startTime;
            this.update = false;
        }

        long timeDiff = startTime - this.startTime;

        UIHelper.setupTexture(type.texture);
        int frame = Configs.REDUCED_MOTION.value ? 0 : (int) ((FiguraMod.ticks / 5f) % type.frames);
        UIHelper.blit( 0, 0, 0f, frame * height(), width(), height(), width(), height() * type.frames);

        FontRenderer font = guiToast.getMinecraft().fontRenderer;
        if (this.message.getFormattedText().trim().isEmpty()) {
            renderText(this.title, font, 0xFF);
        } else if (this.title.getFormattedText().trim().isEmpty()) {
            renderText(this.message, font, 0xFF);
        } else {
            List<String> a = font.listFormattedStringToWidth(this.title.getFormattedText(), width() - type.spacing - 1);
            List<String> b = font.listFormattedStringToWidth(this.message.getFormattedText(), width() - type.spacing - 1);

            if (a.size() == 1 && b.size() == 1) {
                int y = Math.round(height() / 2f - font.FONT_HEIGHT - 1);
                font.drawString(this.title.getFormattedText(), type.spacing, y, 0xFFFFFF);
                font.drawString(this.message.getFormattedText(), type.spacing, y * 2 + 4, 0xFFFFFF);
            } else if (timeDiff < titleTime) {
                renderText(this.title, font, Math.round(Math.min(Math.max((titleTime - timeDiff) / 300f, 0), 1) * 255));
            } else {
                renderText(this.message, font, Math.round(Math.min(Math.max((timeDiff - titleTime) / 300f, 0), 1) * 255));
            }
        }

        return timeDiff < time ? Visibility.SHOW : Visibility.HIDE;
    }

    public void renderText(ITextComponent text, FontRenderer font, int alpha) {
        List<String> list = font.listFormattedStringToWidth(text.getFormattedText(), width() - type.spacing - 1);
        if (list.size() == 1)
            font.drawString(text.getFormattedText(), type.spacing, Math.round(height() / 2f - font.FONT_HEIGHT / 2f), 0xFFFFFF + (alpha << 24));
        else {
            int y = Math.round(height() / 2f - font.FONT_HEIGHT - 1);
            for (int i = 0; i < list.size(); i++)
                font.drawString(list.get(i), type.spacing, y * (i + 1) + 4 * i, 0xFFFFFF + (alpha << 24));
        }
    }

    @Override
    public Object getType() {
        return this.type;
    }

    // Unluckily, depending on how you see it, toast width and height are hardcoded to 32 and 160
    // luckily all our toasts are these dimensions, one less mixin for us
    //@Override
    public int width() {
        return type.width;
    }

    //@Override
    public int height() {
        return 32;
    }

    // new toast
    public static void sendToast(Object title) {
        sendToast(title, new TextComponentString(""));
    }

    public static void sendToast(Object title, ToastType type) {
        sendToast(title, new TextComponentString(""), type);
    }

    public static void sendToast(Object title, Object message) {
        sendToast(title, message, ToastType.DEFAULT);
    }

    public static void sendToast(Object title, Object message, ToastType type) {
        ITextComponent text = title instanceof ITextComponent ? (ITextComponent) title : new TextComponentTranslation(title.toString());
        ITextComponent text2 = message instanceof ITextComponent ? (ITextComponent) message : new TextComponentTranslation(message.toString());

        if (type == ToastType.DEFAULT && Configs.EASTER_EGGS.value) {
            Calendar calendar = FiguraMod.CALENDAR;
            calendar.setTime(new Date());

            if ((calendar.get(Calendar.DAY_OF_MONTH) == 1 && calendar.get(Calendar.MONTH) == Calendar.APRIL) || Math.random() < 0.0001)
                type = ToastType.CHEESE;
        }

        GuiToast toasts = Minecraft.getMinecraft().getToastGui();
        FiguraToast toast = toasts.getToast(FiguraToast.class, type);

        FiguraMod.debug("Sent toast: \"{}\", \"{}\" of type: \"{}\"", text.getFormattedText(), text2.getFormattedText(), type.name());

        if (toast != null)
            toast.update(text, text2, true);
        else
            toasts.add(new FiguraToast(text, text2, type));
    }

    public enum ToastType {
        DEFAULT(new FiguraIdentifier("textures/gui/toast/default.png"), 4, 160, 31, 0x55FFFF),
        WARNING(new FiguraIdentifier("textures/gui/toast/warning.png"), 4, 160, 31, 0xFFFF00),
        ERROR(new FiguraIdentifier("textures/gui/toast/error.png"), 4, 160, 31, 0xFF0000),
        CHEESE(new FiguraIdentifier("textures/gui/toast/cheese.png"), 1, 160, 31, ColorUtils.Colors.CHEESE.hex);

        private final ResourceLocation texture;
        private final int frames;
        private final Style style;
        private final int width, spacing;

        ToastType(ResourceLocation texture, int frames, int width, int spacing, int color) {
            this.texture = texture;
            this.frames = frames;
            this.width = width;
            this.spacing = spacing;
            this.style = ((StyleExtension)new Style()).setRGBColor(color);
        }
    }
}
