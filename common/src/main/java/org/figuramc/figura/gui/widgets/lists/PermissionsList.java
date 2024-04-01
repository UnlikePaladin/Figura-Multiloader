package org.figuramc.figura.gui.widgets.lists;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiPageButtonList;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.ducks.extensions.StyleExtension;
import org.figuramc.figura.gui.widgets.*;
import org.figuramc.figura.permissions.PermissionManager;
import org.figuramc.figura.permissions.PermissionPack;
import org.figuramc.figura.permissions.Permissions;
import org.figuramc.figura.utils.ColorUtils;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.MathUtils;
import org.figuramc.figura.utils.ui.UIHelper;

import java.util.*;
import java.util.function.Predicate;

public class PermissionsList extends AbstractList {

    public boolean precise = false;

    private final Map<ITextComponent, List<FiguraGuiEventListener>> permissions = new LinkedHashMap<>();

    public PermissionsList(int x, int y, int width, int height) {
        super(x, y, width, height);
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
        int lineHeight = font.FONT_HEIGHT;
        int entryHeight = 27 + lineHeight; // 11 (slider) + font height + 16 (padding)
        int titleHeight = 16 + lineHeight;

        int size = 0;
        for (List<FiguraGuiEventListener> value : permissions.values())
            size += value.size();
        int totalHeight = size * entryHeight;

        boolean titles = permissions.size() > 1;
        if (titles) totalHeight += permissions.size() * titleHeight;

        scrollBar.setY(y + 4);
        scrollBar.setVisible(totalHeight > height);
        scrollBar.setScrollRatio(entryHeight, totalHeight - height);

        // render
        int xOffset = scrollBar.isVisible() ? 8 : 15;
        int yOffset = scrollBar.isVisible() ? (int) -(MathUtils.lerp(scrollBar.getScrollProgress(), -16, totalHeight - height)) : 16;

        for (Map.Entry<ITextComponent, List<FiguraGuiEventListener>> entry : permissions.entrySet()) {
            // titles
            if (titles) {
                UIHelper.drawCenteredString(font, entry.getKey(), x + (width - xOffset) / 2, y + yOffset, 0xFFFFFF);
                yOffset += titleHeight;
            }

            // elements
            for (FiguraGuiEventListener widget : entry.getValue()) {
                ((FiguraWidget) widget).setX(x + xOffset);
                ((FiguraWidget) widget).setY(y + yOffset);
                yOffset += entryHeight;
            }
        }

        // render children
        super.draw(mc, mouseX, mouseY, delta);

        // reset scissor
        UIHelper.disableScissor();
    }

    public void updateList(PermissionPack container) {
        // clear old widgets
        for (List<FiguraGuiEventListener> list : permissions.values())
            list.forEach(children::remove);
        permissions.clear();

        // add new permissions

        // defaults
        permissions.put(new FiguraText(), generateWidgets(container, Permissions.DEFAULT, FiguraMod.MOD_ID));

        // custom
        for (Map.Entry<String, Collection<Permissions>> entry : PermissionManager.CUSTOM_PERMISSIONS.entrySet())
            permissions.put(new TextComponentTranslation(entry.getKey()), generateWidgets(container, entry.getValue(), entry.getKey()));
    }

    private List<FiguraGuiEventListener> generateWidgets(PermissionPack container, Collection<Permissions> coll, String id) {
        List<FiguraGuiEventListener> list = new ArrayList<>();

        int x = getX();
        int y = getWidth();
        int width = getWidth();

        for (Permissions permissions : coll) {
            int lineHeight = Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT;

            FiguraGuiEventListener widget;
            String text = id + ".permissions.value." + permissions.name.toLowerCase();
            if (!permissions.isToggle) {
                if (!precise)
                    widget = new PermissionSlider(x + 8, y, width - 30, 11 + lineHeight, container, permissions, this, id, text);
                else
                    widget = new PermissionField(x + 8, y, width - 30, 11 + lineHeight, container, permissions, this, id, text);
            } else {
                widget = new PermissionSwitch(x + 8, y, width - 30, 20 + lineHeight, container, permissions, this, id, text);
            }

            list.add(widget);
            children.add(widget);
        }

        return list;
    }

    private static class PermissionSlider extends SliderWidget {

        private static final ITextComponent INFINITY = new FiguraText("permissions.infinity");

        private final PermissionPack container;
        private final Permissions permissions;
        private final PermissionsList parent;
        private final String text;
        private ITextComponent value;
        private boolean changed;

        public PermissionSlider(int x, int y, int width, int height, PermissionPack container, Permissions permissions, PermissionsList parent, String id, String text) {
            super(x, y, width, height, MathHelper.clamp(container.get(permissions) / (permissions.max + 1d), 0d, 1d), permissions.max / permissions.stepSize + 1, permissions.showSteps());
            this.container = container;
            this.permissions = permissions;
            this.parent = parent;
            this.text = text;
            this.value = container.get(permissions) == Integer.MAX_VALUE ? INFINITY : new TextComponentString(String.valueOf(container.get(permissions)));
            this.changed = container.isChanged(permissions);

            setAction(slider -> {
                // update permission
                int value = this.showSteps ? ((SliderWidget) slider).getIntValue() * permissions.stepSize : (int) ((permissions.max + 1d) * slider.getScrollProgress());
                boolean infinity = permissions.checkInfinity(value);

                container.insert(permissions, infinity ? Integer.MAX_VALUE : value, id);
                changed = container.isChanged(permissions);

                // update text
                this.value = infinity ? INFINITY : new TextComponentString(String.valueOf(value));
            });
        }

        @Override
        public void draw(Minecraft mc, int mouseX, int mouseY, float delta) {
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;

            // button
            GlStateManager.pushMatrix();
            GlStateManager.translate(0f, font.FONT_HEIGHT, 0f);
            super.draw(mc, mouseX, mouseY, delta);
            GlStateManager.popMatrix();

            // texts
            ITextComponent name = new TextComponentTranslation(this.text);
            if (changed) name = new TextComponentString("*").setStyle(FiguraMod.getAccentColor()).appendSibling(name).appendText("*");
            int valueX = getX() + getWidth() - font.getStringWidth(value.getFormattedText()) - 1;

            int x = getX() + 1;
            int y = getY() + 1;
            int width = valueX - getX() - 2;

            UIHelper.renderScrollingText(name, x, y, width, 0xFFFFFF);
            font.drawString(value.createCopy().setStyle(FiguraMod.getAccentColor()).getFormattedText(), valueX, getY() + 1, 0xFFFFFF);

            if (parent.isInsideScissors(mouseX, mouseY) && UIHelper.isMouseOver(x, y, width, font.FONT_HEIGHT, mouseX, mouseY))
                UIHelper.setTooltip(new TextComponentTranslation(this.text + ".tooltip"));
        }

        @Override
        public boolean mouseButtonClicked(int mouseX, int mouseY, int button) {
            if (!this.isActive() || !(this.isHovered()) || !this.mouseOver(mouseX, mouseY))
                return false;

            if (button == 1) {
                container.reset(permissions);
                this.parent.updateList(container);
                playPressedSound(Minecraft.getMinecraft().getSoundHandler());
                return true;
            }

            return super.mouseButtonClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseOver(double mouseX, double mouseY) {
            return this.parent.isInsideScissors(mouseX, mouseY) && super.mouseOver(mouseX, mouseY);
        }
    }

    private static class PermissionSwitch extends SwitchButton {

        private final PermissionPack container;
        private final Permissions permissions;
        private final PermissionsList parent;
        private final String id;
        private final String text;
        private ITextComponent value;
        private boolean changed;

        public PermissionSwitch(int x, int y, int width, int height, PermissionPack container, Permissions permissions, PermissionsList parent, String id, String text) {
            super(x, y, width, height, new TextComponentTranslation(text), permissions.asBoolean(container.get(permissions)));
            this.container = container;
            this.permissions = permissions;
            this.parent = parent;
            this.id = id;
            this.text = text;
            this.changed = container.isChanged(permissions);
            this.value = new FiguraText("permissions." + (toggled ? "enabled" : "disabled"));
        }

        @Override
        public void widgetPressed(int mouseX, int mouseY) {
            // update permission
            boolean value = !this.isToggled();

            container.insert(permissions, value ? 1 : 0, id);
            this.changed = container.isChanged(permissions);

            // update text
            this.value = new FiguraText("permissions." + (value ? "enabled" : "disabled"));

            super.widgetPressed(mouseX, mouseY);
        }

        @Override
        public void drawWidget(Minecraft mc, int mouseX, int mouseY, float delta) {
            super.drawWidget(mc, mouseX, mouseY, delta);
            if (parent.isInsideScissors(mouseX, mouseY) && UIHelper.isMouseOver(getX() + 1, getY() + 1, getWidth() - 2, Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT, mouseX, mouseY))
                UIHelper.setTooltip(new TextComponentTranslation(this.text + ".tooltip"));
        }

        @Override
        protected void renderDefaultTexture(Minecraft mc, float delta) {
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;

            // button
            GlStateManager.pushMatrix();
            GlStateManager.translate(0f, font.FONT_HEIGHT, 0f);
            super.renderDefaultTexture(mc, delta);
            GlStateManager.popMatrix();
        }

        @Override
        protected void renderText(Minecraft mc, float delta) {
            FontRenderer font = mc.fontRenderer;

            // texts
            ITextComponent name = getMessage().createCopy();
            if (changed) name = new TextComponentString("*").setStyle(FiguraMod.getAccentColor()).appendSibling(name).appendText("*");
            int valueX = getX() + getWidth() - font.getStringWidth(value.getFormattedText()) - 1;
            int valueY = getY() + font.FONT_HEIGHT + 11 - font.FONT_HEIGHT / 2;

            UIHelper.renderScrollingText(name, getX() + 1, getY() + 1, getWidth() - 2, 0xFFFFFF);
            font.drawString(value.createCopy().setStyle(FiguraMod.getAccentColor()).getFormattedText(), valueX, valueY, 0xFFFFFF);
        }

        @Override
        public boolean mouseButtonClicked(int mouseX, int mouseY, int button) {
            if (!this.isActive() || !(this.isHovered()) || !this.mouseOver(mouseX, mouseY))
                return false;

            if (button == 1) {
                container.reset(permissions);
                this.parent.updateList(container);
                playPressedSound(Minecraft.getMinecraft().getSoundHandler());
                return true;
            }

            return super.mouseButtonClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseOver(double mouseX, double mouseY) {
            return this.parent.isInsideScissors(mouseX, mouseY) && super.mouseOver(mouseX, mouseY);
        }
    }

    private static class PermissionField extends TextField {

        private final static Predicate<String> validator = s -> {
            try {
                Integer i = Integer.parseInt(s);
                return i >= 0;
            } catch (Exception ignored) {
                return false;
            }
        };

        private final PermissionPack container;
        private final Permissions permissions;
        private final PermissionsList parent;
        private final String text;
        private ITextComponent value;
        private boolean changed;

        public PermissionField(int x, int y, int width, int height, PermissionPack container, Permissions permissions, PermissionsList parent, String id, String text) {
            super(x, y, width, height, null, null);

            this.container = container;
            this.permissions = permissions;
            this.parent = parent;
            this.text = text;
            String val = String.valueOf(container.get(permissions));
            this.value = new TextComponentString(val);
            this.changed = container.isChanged(permissions);

            this.getField().setText(val);
            this.getField().setGuiResponder(new GuiPageButtonList.GuiResponder() {
                @Override
                public void setEntryValue(int i, boolean value) {}
                @Override
                public void setEntryValue(int i, float v) {
                    if (!validator.test(String.valueOf(v)))
                        return;

                    int val = (int)v;

                    container.insert(permissions, val, id);
                    changed = container.isChanged(permissions);

                    // update text
                    value = new TextComponentString(String.valueOf(val));
                }
                @Override
                public void setEntryValue(int i, String txt) {
                    if (!validator.test(txt))
                        return;

                    int val = Integer.parseInt(txt);

                    container.insert(permissions, val, id);
                    changed = container.isChanged(permissions);

                    // update text
                    value = new TextComponentString(String.valueOf(val));
                }
            });
        }

        @Override
        public void draw(Minecraft mc, int mouseX, int mouseY, float delta) {
            FontRenderer font = mc.fontRenderer;

            // text colour
            int color = 0xFFFFFF;

            // invalid value
            String text = getField().getText();
            if (!validator.test(text)) {
                color = 0xFF5555;
            }
            // changed value
            else if (changed) {
                Integer textColor = ((StyleExtension)FiguraMod.getAccentColor()).getRGBColor();
                color = textColor == null ? ColorUtils.Colors.AWESOME_BLUE.hex : textColor;
            }

            // set text colour
            setColor(color);
            setBorderColour(0xFF000000 + color);

            // field
            GlStateManager.pushMatrix();
            //stack.translate(0f, font.lineHeight, 0f);
            super.draw(mc, mouseX, mouseY, delta);
            GlStateManager.popMatrix();

            // texts
            ITextComponent name = new TextComponentTranslation(this.text);
            if (changed) name = new TextComponentString("*").setStyle(FiguraMod.getAccentColor()).appendSibling(name).appendText("*");
            int valueX = getX() + getWidth() - font.getStringWidth(value.getFormattedText()) - 1;

            int x = getX() + 1;
            int y = getY() + 1 - font.FONT_HEIGHT;
            int width = valueX - getX() - 2;

            UIHelper.renderScrollingText(name, x, y, width, 0xFFFFFF);
            font.drawString(value.createCopy().setStyle(FiguraMod.getAccentColor()).getFormattedText(), valueX, getY() + 1 - font.FONT_HEIGHT, 0xFFFFFF);

            if (parent.isInsideScissors(mouseX, mouseY) && UIHelper.isMouseOver(x, y, width, font.FONT_HEIGHT, mouseX, mouseY))
                UIHelper.setTooltip(new TextComponentTranslation(this.text + ".tooltip"));
        }

        @Override
        public boolean mouseButtonClicked(int mouseX, int mouseY, int button) {
            if (!this.isEnabled() || !this.mouseOver(mouseX, mouseY))
                return false;

            if (button == 1) {
                container.reset(permissions);
                this.parent.updateList(container);
                Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                return true;
            }

            return super.mouseButtonClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseOver(double mouseX, double mouseY) {
            return this.parent.isInsideScissors(mouseX, mouseY) && super.mouseOver(mouseX, mouseY);
        }
    }
}
