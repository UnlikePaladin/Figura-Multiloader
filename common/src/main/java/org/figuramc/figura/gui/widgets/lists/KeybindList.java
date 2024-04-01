package org.figuramc.figura.gui.widgets.lists;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.gui.widgets.*;
import org.figuramc.figura.lua.api.keybind.FiguraKeybind;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.MathUtils;
import org.figuramc.figura.utils.TextUtils;
import org.figuramc.figura.utils.ui.UIHelper;

import java.util.ArrayList;
import java.util.List;

public class KeybindList extends AbstractList {

    private final List<KeybindElement> keybinds = new ArrayList<>();
    private final Avatar owner;
    private final Button resetAllButton;

    private FiguraKeybind focusedKeybind;

    public KeybindList(int x, int y, int width, int height, Avatar owner, Button resetAllButton) {
        super(x, y, width, height);
        this.owner = owner;
        this.resetAllButton = resetAllButton;
        updateList();

        Label noOwner, noKeys;
        this.children.add(noOwner = new Label(new FiguraText("gui.error.no_avatar").setStyle(new Style().setColor(TextFormatting.YELLOW)), x + width / 2, y + height / 2, TextUtils.Alignment.CENTER, 0));
        this.children.add(noKeys = new Label(new FiguraText("gui.error.no_keybinds").setStyle(new Style().setColor(TextFormatting.YELLOW)), x + width / 2, y + height / 2, TextUtils.Alignment.CENTER, 0));
        noOwner.centerVertically = noKeys.centerVertically = true;

        noOwner.setVisible(owner == null);
        noKeys.setVisible(!noOwner.isVisible() && keybinds.isEmpty());
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY, float delta) {
        // background and scissors
        UIHelper.renderSliced(getX(), getY(), getWidth(), getHeight(), UIHelper.OUTLINE_FILL);
        UIHelper.setupScissor(getX() + scissorsX, getY() + scissorsY, getWidth() + scissorsWidth, getHeight() + scissorsHeight);

        if (!keybinds.isEmpty())
            updateEntries();

        // children
        super.draw(mc, mouseX, mouseY, delta);

        // reset scissor
        UIHelper.disableScissor();
    }

    private void updateEntries() {
        // scrollbar
        int totalHeight = -4;
        for (KeybindElement keybind : keybinds)
            totalHeight += keybind.getHeight() + 8;
        int entryHeight = keybinds.isEmpty() ? 0 : totalHeight / keybinds.size();

        scrollBar.setVisible(totalHeight > getHeight());
        scrollBar.setScrollRatio(entryHeight, totalHeight - getHeight());

        //render list
        int xOffset = scrollBar.isVisible() ? 4 : 11;
        int yOffset = scrollBar.isVisible() ? (int) -(MathUtils.lerp(scrollBar.getScrollProgress(), -4, totalHeight - getHeight())) : 4;
        for (KeybindElement keybind : keybinds) {
            keybind.setX(getX() + xOffset);
            keybind.setY(getY() + yOffset);
            yOffset += keybind.getHeight() + 8;
        }
    }

    private void updateList() {
        // clear old widgets
        keybinds.forEach(children::remove);

        // add new keybinds
        if (owner == null || owner.luaRuntime == null)
            return;

        for (FiguraKeybind keybind : owner.luaRuntime.keybinds.keyBindings) {
            KeybindElement element = new KeybindElement(getWidth() - 22, keybind, this);
            keybinds.add(element);
            children.add(element);
        }

        updateBindings();
    }

    public boolean updateKey(int key) {
        if (focusedKeybind == null)
            return false;

        focusedKeybind.setKey(key);
        focusedKeybind = null;
        FiguraMod.processingKeybind = false;

        updateBindings();
        return true;
    }

    public void updateBindings() {
        boolean active = false;

        for (KeybindElement keybind : keybinds) {
            keybind.updateText();
            if (!active && !keybind.keybind.isDefault())
                active = true;
        }

        resetAllButton.setActive(active);
    }

    private static class KeybindElement extends AbstractContainerElement {

        private final KeybindWidgetHelper helper = new KeybindWidgetHelper();
        private final FiguraKeybind keybind;
        private final KeybindList parent;
        private final Button resetButton;
        private final Button keybindButton;

        public KeybindElement(int width, FiguraKeybind keybind, KeybindList parent) {
            super(0, 0, width, 20);
            this.keybind = keybind;
            this.parent = parent;

            // toggle button
            children.add(0, keybindButton = new ParentedButton(0, 0, 90, 20, keybind.getTranslatedKeyMessage(), this, button -> {
                parent.focusedKeybind = keybind;
                FiguraMod.processingKeybind = true;
                updateText();
            }));

            // reset button
            children.add(resetButton = new ParentedButton(0, 0, 60, 20, new TextComponentTranslation("controls.reset"), this, button -> {
                keybind.resetDefaultKey();
                parent.updateBindings();
            }));
        }

        @Override
        public void draw(Minecraft mc, int mouseX, int mouseY, float delta) {
            if (!this.isVisible()) return;

            helper.renderConflictBars(keybindButton.getX() - 8, keybindButton.getY() + 2, 4, 16);

            // vars
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            int textY = getY() + getHeight() / 2 - font.FONT_HEIGHT / 2;

            // hovered arrow
            setHovered(mouseOver(mouseX, mouseY));
            if (isHovered()) {
                font.drawString(HOVERED_ARROW.getFormattedText(), getX() + 4, textY, 0xFFFFFF);
                if ((keybindButton.isHovered()))
                    helper.renderTooltip();
            }

            // render name
            font.drawString(this.keybind.getName(), getX() + 16, textY, 0xFFFFFF);

            // render children
            super.draw(mc, mouseX, mouseY, delta);
        }

        @Override
        public boolean mouseOver(double mouseX, double mouseY) {
            return this.parent.isInsideScissors(mouseX, mouseY) && super.mouseOver(mouseX, mouseY);
        }

        @Override
        public void setX(int x) {
            super.setX(x);
            resetButton.setX(x + getWidth() - 60);
            keybindButton.setX(x + getWidth() - 154);
        }

        @Override
        public void setY(int y) {
            super.setY(y);
            resetButton.setY(y);
            keybindButton.setY(y);
        }

        public void updateText() {
            // tooltip
            List<FiguraKeybind> temp = new ArrayList<>();
            for (KeybindElement keybind : parent.keybinds)
                temp.add(keybind.keybind);
            helper.setTooltip(this.keybind, temp);

            // reset enabled
            boolean isDefault = this.keybind.isDefault();
            this.resetButton.setActive(!isDefault);

            // text
            boolean selected = parent.focusedKeybind == this.keybind;
            ITextComponent text = helper.getText(isDefault, selected, this.keybind.getTranslatedKeyMessage());
            keybindButton.setMessage(text);
        }
    }
}
