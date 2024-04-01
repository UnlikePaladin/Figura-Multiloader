package org.figuramc.figura.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.*;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.lua.api.keybind.FiguraKeybind;
import org.figuramc.figura.mixin.font.FontRendererAccessor;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.ui.UIHelper;
import org.lwjgl.input.Keyboard;

import java.util.List;

public class KeybindWidgetHelper {
    private ITextComponent tooltip;
    private boolean vanillaConflict, avatarConflict;

    public void renderConflictBars(int x, int y, int width, int height) {
        // conflict bars
        if (vanillaConflict || avatarConflict) {
            if (avatarConflict) {
                UIHelper.fill(x, y, x + width, y + height, ((FontRendererAccessor)Minecraft.getMinecraft().fontRenderer).getColors()[TextFormatting.YELLOW.getColorIndex()] | 0xFF000000);
                x -= width + 4;
            }
            if (vanillaConflict) {
                UIHelper.fill(x, y, x + width, y + height, ((FontRendererAccessor)Minecraft.getMinecraft().fontRenderer).getColors()[TextFormatting.RED.getColorIndex()] | 0xFF000000);
            }
        }
    }

    public void renderTooltip() {
        if (tooltip != null)
            UIHelper.setTooltip(tooltip);
    }


    // -- texts -- //



    // must be called before getText()
    public void setTooltip(FiguraKeybind keybind, List<FiguraKeybind> keyBindings) {
        ITextComponent text = new TextComponentString("");

        // avatar conflicts
        ITextComponent avatar = checkForAvatarConflicts(keybind, keyBindings);
        boolean hasAvatarConflict = avatar != null && !avatar.getFormattedText().trim().isEmpty();
        if (hasAvatarConflict)
            text.appendSibling(avatar);

        // vanilla conflicts
        ITextComponent vanilla = checkForVanillaConflicts(keybind);
        if (vanilla != null && !vanilla.getFormattedText().trim().isEmpty()) {
            if (hasAvatarConflict)
                text.appendText("\n");
            text.appendSibling(vanilla);
        }

        // set tooltip
        setTooltipTail(text);
    }

    public void setTooltip(KeyBinding keybind) {
        ITextComponent text = new TextComponentString("");

        // vanilla conflicts
        ITextComponent vanilla = checkForVanillaConflicts(keybind);
        if (vanilla != null && !vanilla.getFormattedText().trim().isEmpty())
            text.appendSibling(vanilla);

        // set tooltip
        setTooltipTail(text);
    }

    private void setTooltipTail(ITextComponent text) {
        if (vanillaConflict || avatarConflict) {
            this.tooltip = new FiguraText("gui.duplicate_keybind", text);
        } else {
            this.tooltip = null;
        }
    }

    public ITextComponent getText(boolean isDefault, boolean isSelected, ITextComponent initialMessage) {
        // button message
        ITextComponent message = initialMessage.createCopy();
        if (isDefault || isSelected) message.setStyle(new Style().setColor(TextFormatting.WHITE));
        else message.setStyle(FiguraMod.getAccentColor());

        if (isSelected) message.setStyle(new Style().setUnderlined(true));

        if (this.avatarConflict || this.vanillaConflict) {
            ITextComponent left = new TextComponentString("[ ").setStyle(new Style().setColor(this.vanillaConflict ? TextFormatting.RED : TextFormatting.YELLOW));
            ITextComponent right = new TextComponentString(" ]").setStyle(new Style().setColor(this.avatarConflict ? TextFormatting.YELLOW : TextFormatting.RED));
            message = left.appendSibling(message).appendSibling(right);
        }

        // selected
        if (isSelected)
            message = new TextComponentString("> ").appendSibling(message).appendText(" <").setStyle(FiguraMod.getAccentColor());

        return message;
    }


    // -- avatar conflict -- //


    public ITextComponent checkForAvatarConflicts(FiguraKeybind keybind, List<FiguraKeybind> keyBindings) {
        this.avatarConflict = false;

        int id = keybind.getID();
        if (id == -1)
            return null;

        ITextComponent message = new TextComponentString("");
        for (FiguraKeybind keyBinding : keyBindings) {
            if (keyBinding != keybind && keyBinding.getID() == id) {
                this.avatarConflict = true;
                message.appendSibling(new TextComponentString("\n• ").setStyle(new Style().setColor(TextFormatting.YELLOW)).appendText(keyBinding.getName()));
            }
        }

        return message;
    }


    // -- vanilla conflict -- //


    public ITextComponent checkForVanillaConflicts(FiguraKeybind keybind) {
        this.vanillaConflict = false;
        if (keybind.getID() == -1)
            return null;

        String keyName = keybind.getKey();
        ITextComponent message = new TextComponentString("");
        for (KeyBinding key : Minecraft.getMinecraft().gameSettings.keyBindings) {
            if (key.getKeyDescription().equals(keyName)) {
                this.vanillaConflict = true;
                message.appendSibling(new TextComponentString("\n• ").setStyle(new Style().setColor(TextFormatting.RED))
                        .appendSibling(new TextComponentTranslation(key.getKeyCategory()))
                        .appendText(": ")
                        .appendSibling(new TextComponentTranslation(key.getKeyDescription()))
                );
            }
        }

        return message;
    }

    public ITextComponent checkForVanillaConflicts(KeyBinding keybind) {
        this.vanillaConflict = false;
        if (keybind.getKeyCode() == Keyboard.KEY_NONE)
            return null;

        int keyCode = keybind.getKeyCode();
        ITextComponent message = new TextComponentString("");
        for (KeyBinding key : Minecraft.getMinecraft().gameSettings.keyBindings) {
            if (key != keybind && key.getKeyCode() == keyCode) {
                this.vanillaConflict = true;
                message.appendSibling(new TextComponentString("\n• ").setStyle(new Style().setColor(TextFormatting.RED))
                        .appendSibling(new TextComponentTranslation(key.getKeyCategory()))
                        .appendText(": ")
                        .appendSibling(new TextComponentTranslation(key.getKeyDescription()))
                );
            }
        }

        return message;
    }
}
