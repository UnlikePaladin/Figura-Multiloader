package org.figuramc.figura.gui.widgets.lists;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.config.ConfigKeyBinding;
import org.figuramc.figura.config.ConfigManager;
import org.figuramc.figura.config.ConfigType;
import org.figuramc.figura.gui.screens.ConfigScreen;
import org.figuramc.figura.gui.widgets.FiguraGuiEventListener;
import org.figuramc.figura.gui.widgets.TextField;
import org.figuramc.figura.gui.widgets.config.CategoryWidget;
import org.figuramc.figura.gui.widgets.config.InputElement;
import org.figuramc.figura.utils.MathUtils;
import org.figuramc.figura.utils.ui.UIHelper;

import java.util.ArrayList;
import java.util.List;

public class ConfigList extends AbstractList {

    private final List<CategoryWidget> configs = new ArrayList<>();
    public final ConfigScreen parentScreen;
    public KeyBinding focusedBinding;

    private int totalHeight = 0;

    public ConfigList(int x, int y, int width, int height, ConfigScreen parentScreen) {
        super(x, y, width, height);
        this.parentScreen = parentScreen;
        updateList();
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
        totalHeight = -4;
        int visibleConfig = 0;
        for (CategoryWidget config : configs) {
            if (config.isVisible()) {
                totalHeight += config.getHeight() + 8;
                visibleConfig++;
            }
        }
        int entryHeight = visibleConfig == 0 ? 0 : totalHeight / visibleConfig;

        scrollBar.setVisible(totalHeight > height);
        scrollBar.setScrollRatio(entryHeight, totalHeight - height);

        // render list
        int xOffset = scrollBar.isVisible() ? 4 : 11;
        int yOffset = scrollBar.isVisible() ? (int) -(MathUtils.lerp(scrollBar.getScrollProgress(), -4, totalHeight - height)) : 4;
        for (CategoryWidget config : configs) {
            if (!config.isVisible())
                continue;

            config.setX(x + xOffset);
            config.setY(y + yOffset);
            yOffset += config.getHeight() + 8;
        }

        // children
        super.draw(mc, mouseX, mouseY, delta);

        // reset scissor
        UIHelper.disableScissor();
    }

    @Override
    public boolean mouseButtonClicked(int mouseX, int mouseY, int button) {
        // fix mojang focusing for text fields
        for (CategoryWidget categoryWidget : configs) {
            for (FiguraGuiEventListener children : categoryWidget.children()) {
                if (children instanceof InputElement) {
                    InputElement inputElement = (InputElement) children;
                    TextField field = inputElement.getTextField();
                    field.getField().setFocused(field.isEnabled() && field.mouseOver(mouseX, mouseY));
                }
            }
        }

        return super.mouseButtonClicked(mouseX, mouseY, button);
    }

    public void updateList() {
        // clear old widgets
        for (CategoryWidget config : configs)
            children.remove(config);
        configs.clear();

        // add configs
        for (ConfigType.Category category : ConfigManager.CATEGORIES_REGISTRY.values()) {
            CategoryWidget widget = new CategoryWidget(getWidth() - 22, category, this);

            for (ConfigType<?> config : category.children)
                widget.addConfig(config);

            configs.add(widget);
            children.add(widget);
        }

        // fix expanded status
        for (CategoryWidget config : configs)
            config.setShowChildren(config.isShowingChildren());
    }

    public void updateScroll() {
        // store old scroll pos
        double pastScroll = (totalHeight - getHeight()) * scrollBar.getScrollProgress();

        // get new height
        totalHeight = -4;
        for (CategoryWidget config : configs)
            if (config.isVisible())
                totalHeight += config.getHeight() + 8;

        // set new scroll percentage
        scrollBar.setScrollProgress(pastScroll / (totalHeight - getHeight()));
    }

    public boolean hasChanges() {
        for (CategoryWidget config : configs)
            if (config.isChanged())
                return true;
        return false;
    }

    public boolean updateKey(int keyCode) {
        if (focusedBinding == null)
            return false;

        focusedBinding.setKeyCode(keyCode);
        if (focusedBinding instanceof ConfigKeyBinding)
            ((ConfigKeyBinding)focusedBinding).saveConfigChanges();

        focusedBinding = null;
        FiguraMod.processingKeybind = false;

        updateKeybinds();
        return true;
    }

    public void updateKeybinds() {
        for (CategoryWidget widget : configs)
            widget.updateKeybinds();
    }

    public void updateSearch(String query) {
        for (CategoryWidget widget : configs)
            widget.updateFilter(query);
    }
}
