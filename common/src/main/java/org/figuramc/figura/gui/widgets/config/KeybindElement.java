package org.figuramc.figura.gui.widgets.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.config.ConfigKeyBinding;
import org.figuramc.figura.config.ConfigType;
import org.figuramc.figura.gui.widgets.KeybindWidgetHelper;
import org.figuramc.figura.gui.widgets.ParentedButton;
import org.figuramc.figura.gui.widgets.lists.ConfigList;

public class KeybindElement extends AbstractConfigElement {

    private final KeybindWidgetHelper helper = new KeybindWidgetHelper();
    private final KeyBinding binding;
    private final ParentedButton button;

    public KeybindElement(int width, ConfigType.KeybindConfig config, ConfigList parentList, CategoryWidget parentCategory) {
        super(width, config, parentList, parentCategory);
        this.binding = config.keyBind;

        // toggle button
        children.add(0, button = new ParentedButton(0, 0, 90, 20, new TextComponentTranslation(this.binding.getKeyDescription()), this, button -> {
            parentList.focusedBinding = binding;
            FiguraMod.processingKeybind = true;
            updateText();
        }));
        button.setActive(FiguraMod.debugModeEnabled() || !config.disabled);

        // overwrite reset button to update the keybind
        children.remove(resetButton);
        children.add(resetButton = new ParentedButton(getX() + width - 60, getY(), 60, 20, new TextComponentTranslation("controls.reset"), this, button -> {
            binding.setKeyCode(binding.getKeyCodeDefault());
            ((ConfigKeyBinding)binding).saveConfigChanges();
            parentList.updateKeybinds();
        }));

        updateText();
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY, float delta) {
        if (!this.isVisible()) return;

        // reset enabled
        helper.renderConflictBars(button.getX() - 8, button.getY() + 2, 4, 16);

        // super render
        super.draw(mc, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseOver(double mouseX, double mouseY) {
        boolean bool = super.mouseOver(mouseX, mouseY);
        if (bool && button.mouseOver(mouseX, mouseY))
            helper.renderTooltip();
        return bool;
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        this.button.setX(x + getWidth() - 154);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        this.button.setY(y);
    }

    @Override
    public boolean isDefault() {
        return this.binding.getKeyCode() == this.binding.getKeyCodeDefault();
    }

    @Override
    public boolean isChanged() {
        return false;
    }

    public void updateText() {
        // tooltip
        helper.setTooltip(binding);

        // reset button
        boolean isDefault = isDefault();
        this.resetButton.setActive(!isDefault);

        // text
        boolean selected = parentList.focusedBinding == binding;
        ITextComponent text = helper.getText(isDefault, selected, new TextComponentTranslation(binding.getKeyDescription()));
        button.setMessage(text);
    }
}
