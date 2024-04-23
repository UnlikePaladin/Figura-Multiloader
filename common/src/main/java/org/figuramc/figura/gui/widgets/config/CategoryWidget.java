package org.figuramc.figura.gui.widgets.config;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.config.ConfigType;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.gui.screens.ConfigScreen;
import org.figuramc.figura.gui.widgets.AbstractContainerElement;
import org.figuramc.figura.gui.widgets.ContainerButton;
import org.figuramc.figura.gui.widgets.lists.ConfigList;
import org.figuramc.figura.utils.ui.UIHelper;

import java.util.ArrayList;
import java.util.List;

public class CategoryWidget extends AbstractContainerElement {

    protected final List<AbstractConfigElement> entries = new ArrayList<>();
    private final ConfigType.Category config;
    private final ConfigList parent;
    private ContainerButton parentConfig;

    public CategoryWidget(int width, ConfigType.Category config, ConfigList parent) {
        super(0, 0, width, 20);
        this.config = config;
        this.parent = parent;

        this.parentConfig = new ContainerButton(parent, 0,0, width, 20, config == null ? new TextComponentString("") : config.name, config == null ? null : config.tooltip, button -> {
            boolean toggled = this.parentConfig.isToggled();
            setShowChildren(toggled);
            ConfigScreen.CATEGORY_DATA.put(config, toggled);
            parent.updateScroll();
        });

        Boolean expanded = ConfigScreen.CATEGORY_DATA.get(config);
        this.parentConfig.setToggled(expanded == null || expanded);
        this.parentConfig.shouldHaveBackground(false);
        children.add(this.parentConfig);
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY, float delta) {
        if (!isVisible())
            return;

        // children background
        if (parentConfig.isToggled() && entries.size() > 0)
            UIHelper.fill(getX(), getY() + 21, getX() + getWidth(), getY() + getHeight(), 0x11FFFFFF);

        if (config == Configs.PAPERDOLL)
            parent.parentScreen.renderPaperdoll = parentConfig.isToggled() && parent.mouseOver(mouseX, mouseY) && mouseOver(mouseX, mouseY);

        // children
        super.draw(mc, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseOver(double mouseX, double mouseY) {
        return UIHelper.isMouseOver(getX(), getY(), getWidth(), getHeight(), mouseX, mouseY);
    }

    public void addConfig(ConfigType<?> config) {
        if (config.hidden && !FiguraMod.debugModeEnabled()) return;
        int width = getWidth();

        AbstractConfigElement element;
        if (config instanceof ConfigType.BoolConfig) {
            ConfigType.BoolConfig boolConfig = (ConfigType.BoolConfig) config;
            element = new BooleanElement(width, boolConfig, parent, this);
        } else if (config instanceof ConfigType.EnumConfig) {
            ConfigType.EnumConfig enumConfig = (ConfigType.EnumConfig) config;
            element = new EnumElement(width, enumConfig, parent, this);
        } else if (config instanceof ConfigType.InputConfig<?>) {
            ConfigType.InputConfig<?> inputConfig = (ConfigType.InputConfig<?>) config;
            element = new InputElement(width, inputConfig, parent, this);
        } else if (config instanceof ConfigType.KeybindConfig) {
            ConfigType.KeybindConfig keybindConfig = (ConfigType.KeybindConfig) config;
            element = new KeybindElement(width, keybindConfig, parent, this);
        } else if (config instanceof ConfigType.ButtonConfig) {
            ConfigType.ButtonConfig buttonConfig = (ConfigType.ButtonConfig) config;
            element = new ButtonElement(width, buttonConfig, parent, this);
        } else {
            return;
        }

        this.setHeight(super.getHeight() + 22);
        this.children.add(element);
        this.entries.add(element);
    }

    public boolean isChanged() {
        for (AbstractConfigElement entry : entries)
            if (entry.isChanged())
                return true;
        return false;
    }

    @Override
    public int getHeight() {
        if (!parentConfig.isToggled())
            return 20;

        int height = 20;
        for (AbstractConfigElement entry : entries) {
            if (entry.isVisible())
                height += entry.getHeight() + 2;
        }

        return height;
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        this.parentConfig.setX(x);
        for (AbstractConfigElement entry : entries)
            entry.setX(x);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        this.parentConfig.setY(y);
        int i = 0;
        for (AbstractConfigElement entry : entries) {
            if (entry.isVisible()) {
                entry.setY(y + 22 * (i + 1));
                i++;
            }
        }
    }

    public void setShowChildren(boolean bool) {
        this.parentConfig.setToggled(bool);
        for (AbstractConfigElement element : entries)
            element.setVisible(bool);
    }

    public boolean isShowingChildren() {
        return parentConfig.isToggled();
    }

    public void updateKeybinds() {
        for (AbstractConfigElement element : entries) {
            if (element instanceof KeybindElement) {
                KeybindElement keybind = (KeybindElement) element;
                keybind.updateText();
            }
        }
    }

    public void updateFilter(String query) {
        boolean visible = false;

        for (AbstractConfigElement entry : entries) {
            entry.updateFilter(query);
            visible |= entry.matchesFilter();
        }

        this.setVisible(visible);
        if (visible)
            this.setY(this.getY());

        parent.updateScroll();
    }
}
