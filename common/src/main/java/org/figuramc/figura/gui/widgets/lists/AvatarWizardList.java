package org.figuramc.figura.gui.widgets.lists;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiPageButtonList;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.gui.widgets.FiguraGuiEventListener;
import org.figuramc.figura.gui.widgets.FiguraWidget;
import org.figuramc.figura.gui.widgets.SwitchButton;
import org.figuramc.figura.gui.widgets.TextField;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.MathUtils;
import org.figuramc.figura.utils.ui.UIHelper;
import org.figuramc.figura.wizards.AvatarWizard;
import org.figuramc.figura.wizards.WizardEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AvatarWizardList extends AbstractList {

    private final AvatarWizard wizard;
    private final Map<ITextComponent, List<FiguraGuiEventListener>> map = new LinkedHashMap<>();

    public AvatarWizardList(int x, int y, int width, int height, AvatarWizard wizard) {
        super(x, y, width, height);
        this.wizard = wizard;
        generate();
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();

        // background and scissors
        UIHelper.renderSliced(x, y, width, height, UIHelper.OUTLINE_FILL);
        UIHelper.setupScissor(x + scissorsX, y + scissorsY, width + scissorsWidth, height + scissorsHeight);

        // scrollbar
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int lineHeight = font.FONT_HEIGHT + 8;
        int entryHeight = 24;

        int size = 0;
        for (List<FiguraGuiEventListener> list : map.values()) {
            for (FiguraGuiEventListener widget : list) {
                if (widget instanceof WizardInputBox) {
                    WizardInputBox ib = (WizardInputBox) widget;
                    ib.setVisible(wizard.checkDependency(ib.entry));
                    if (ib.isVisible()) size++;
                } else if (widget instanceof WizardToggleButton) {
                    WizardToggleButton tb = (WizardToggleButton) widget;
                    tb.setVisible(wizard.checkDependency(tb.entry));
                    if (tb.isVisible()) size++;
                }
            }
        }
        int totalHeight = entryHeight * size + lineHeight * map.size();

        scrollBar.setVisible(totalHeight > height);
        scrollBar.setScrollRatio(entryHeight, totalHeight - height);

        // render list
        int yOffset = scrollBar.isVisible() ? (int) -(MathUtils.lerp(scrollBar.getScrollProgress(), -4, totalHeight - height)) : 4;
        for (Map.Entry<ITextComponent, List<FiguraGuiEventListener>> entry : map.entrySet()) {
            List<FiguraGuiEventListener> value = entry.getValue();
            if (value.isEmpty())
                continue;

            int newY = yOffset + lineHeight;
            // elements
            for (FiguraGuiEventListener w : entry.getValue()) {
                FiguraWidget widget = (FiguraWidget) w;
                if (widget.isVisible()) {
                    widget.setY(y + newY);
                    newY += entryHeight;
                }
            }

            if (newY == yOffset + lineHeight)
                continue;

            // category
            UIHelper.drawCenteredString(font, entry.getKey(), x + width / 2, y + yOffset + 4, 0xFFFFFF);
            yOffset = newY;
        }

        // children
        super.draw(mc, mouseX, mouseY, delta);

        // reset scissor
        UIHelper.disableScissor();
    }

    @Override
    public boolean mouseButtonClicked(int mouseX, int mouseY, int button) {
        // fix mojang focusing for text fields
        for (FiguraGuiEventListener widget : children()) {
            if (widget instanceof TextField) {
                TextField field = (TextField) widget;
                field.getField().setFocused(field.isEnabled() && field.mouseOver(mouseX, mouseY));
            }
        }

        return super.mouseButtonClicked(mouseX, mouseY, button);
    }

    private void generate() {
        for (List<FiguraGuiEventListener> value : map.values())
            children.removeAll(value);
        map.clear();

        int x = this.getX() + getWidth() / 2 + 4;
        int width = this.getWidth() / 2 - 20;

        ITextComponent lastName = new TextComponentString("");
        List<FiguraGuiEventListener> lastList = new ArrayList<>();

        for (WizardEntry value : WizardEntry.all()) {
            switch (value.type) {
                case CATEGORY:
                    if (!lastList.isEmpty()) {
                        map.put(lastName, lastList);
                        children.addAll(lastList);
                    }
                    lastName = new FiguraText("gui.avatar_wizard." + value.name.toLowerCase());
                    lastList = new ArrayList<>();
                    break;
                case TEXT:
                    lastList.add(new WizardInputBox(x, width, this, value));
                    break;
                case TOGGLE:
                    lastList.add(new WizardToggleButton(x, width, this, value));
                    break;
            }
        }

        map.put(lastName, lastList);
        children.addAll(lastList);
    }

    private static class WizardInputBox extends TextField {

        private final AvatarWizardList parent;
        private final WizardEntry entry;
        private final ITextComponent name;

        public WizardInputBox(int x, int width, AvatarWizardList parent, WizardEntry entry) {
            super(x, 0, width, 20, HintType.ANY, new GuiPageButtonList.GuiResponder() {
                @Override
                public void setEntryValue(int i, boolean value) {
                    parent.wizard.changeEntry(entry, value);
                }

                @Override
                public void setEntryValue(int i, float value) {
                    parent.wizard.changeEntry(entry, value);
                }

                @Override
                public void setEntryValue(int i, String value) {
                    parent.wizard.changeEntry(entry, value);
                }
            });
            this.parent = parent;
            this.entry = entry;
            this.name = new FiguraText("gui.avatar_wizard." + entry.name.toLowerCase());
            this.getField().setText(String.valueOf(parent.wizard.getEntry(entry, "")));
        }


        @Override
        public void draw(Minecraft mc, int mouseX, int mouseY, float delta) {
            if (!isVisible()) return;
            super.draw(mc, mouseX, mouseY, delta);

            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            ITextComponent name = this.name.createCopy();
            if (!this.getField().getText().trim().isEmpty())
                name.setStyle(FiguraMod.getAccentColor());
            font.drawString(name.getFormattedText(), getX() - getWidth() - 8, (int) (getY() + (getHeight() - font.FONT_HEIGHT) / 2f), 0xFFFFFF);
        }

        @Override
        public boolean mouseOver(double mouseX, double mouseY) {
            return this.parent.isInsideScissors(mouseX, mouseY) && super.mouseOver(mouseX, mouseY);
        }
    }

    private static class WizardToggleButton extends SwitchButton {

        private final AvatarWizardList parent;
        private final WizardEntry entry;

        public WizardToggleButton(int x, int width, AvatarWizardList parent, WizardEntry entry) {
            super(x, 0, width, 20, new FiguraText("gui.avatar_wizard." + entry.name.toLowerCase()), false);
            this.parent = parent;
            this.entry = entry;
            this.setToggled((boolean) parent.wizard.getEntry(entry, false));
        }

        @Override
        public void widgetPressed(int mouseX, int mouseY) {
            super.widgetPressed(mouseX, mouseY);
            parent.wizard.changeEntry(entry, this.isToggled());
        }

        @Override
        protected void renderDefaultTexture(Minecraft mc, float delta) {
            // button
            GlStateManager.pushMatrix();
            GlStateManager.translate(getWidth() - 30, 0, 0);
            super.renderDefaultTexture(mc, delta);
            GlStateManager.popMatrix();
        }

        @Override
        protected void renderText(Minecraft mc, float delta) {
            // name
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            ITextComponent name = getMessage().createCopy();
            if (this.isToggled())
                name.setStyle(FiguraMod.getAccentColor());
            font.drawString(name.getFormattedText(), getX() - getWidth() - 8, (int) (getY() + (getHeight() - font.FONT_HEIGHT) / 2f), 0xFFFFFF);
        }

        @Override
        public boolean mouseOver(double mouseX, double mouseY) {
            return this.parent.isInsideScissors(mouseX, mouseY) && super.mouseOver(mouseX, mouseY);
        }
    }
}
