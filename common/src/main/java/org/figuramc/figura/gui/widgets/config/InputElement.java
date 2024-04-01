package org.figuramc.figura.gui.widgets.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPageButtonList;
import net.minecraft.util.text.TextComponentTranslation;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.config.ConfigType;
import org.figuramc.figura.config.InputType;
import org.figuramc.figura.ducks.extensions.StyleExtension;
import org.figuramc.figura.gui.widgets.ParentedButton;
import org.figuramc.figura.gui.widgets.TextField;
import org.figuramc.figura.gui.widgets.lists.ConfigList;
import org.figuramc.figura.utils.ColorUtils;
import org.figuramc.figura.utils.ui.UIHelper;

public class InputElement extends AbstractConfigElement {

    private final TextField textField;
    private final InputType inputType;

    public InputElement(int width, ConfigType.InputConfig<?> config, ConfigList parentList, CategoryWidget parentCategory) {
        super(width, config, parentList, parentCategory);

        // get input type
        this.inputType = config.inputType;

        // text field
        textField = new InputField(0, 0, 90, 20, inputType.hint, this, new GuiPageButtonList.GuiResponder() {
            @Override
            public void setEntryValue(int i, boolean value) {

            }

            @Override
            public void setEntryValue(int i, float value) {

            }

            @Override
            public void setEntryValue(int i, String value) {
                // only write config value if it's valid
                if (inputType.validator.test(value))
                    config.setTempValue(value);
            }
        });
        updateTextFieldText(formatText(config.tempValue));
        textField.getField().setCursorPositionZero();
        textField.setEnabled(FiguraMod.debugModeEnabled() || !config.disabled);

        children.add(0, textField);

        // overwrite reset button to update the text field
        children.remove(resetButton);
        children.add(resetButton = new ParentedButton(getX() + width - 60, getY(), 60, 20, new TextComponentTranslation("controls.reset"), this, button -> {
            config.resetTemp();
            updateTextFieldText(formatText(config.tempValue));
        }));
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY, float delta) {
        if (!this.isVisible()) return;

        // reset enabled
        this.resetButton.setActive(!isDefault());

        // text colour
        int color = 0xFFFFFF;

        // invalid config
        String text = textField.getField().getText();
        if (!inputType.validator.test(text)) {
            color = 0xFF5555;
        }
        // config was changed
        else if (!text.equals(formatText(initValue))) {
            Integer textColor = ((StyleExtension)(FiguraMod.getAccentColor())).getRGBColor();
            color = textColor == null ? ColorUtils.Colors.AWESOME_BLUE.hex : textColor;
        }

        // set text colour
        textField.setColor(color);
        textField.setBorderColour(0xFF000000 + color);

        // super render
        super.draw(mc, mouseX, mouseY, delta);

        // hex colour preview
        if (inputType == InputType.HEX_COLOR) {
            int x = this.getX() + getWidth() - 178;
            int y = this.getY();

            // border
            if (getTextField().isFocused())
                UIHelper.fillRounded(x, y, 20, 20, getTextField().getBorderColour());
            else
                UIHelper.renderSliced(x, y, 20, 20, UIHelper.OUTLINE);

            // inside
            UIHelper.fillRounded(x + 1, y + 1, 18, 18, (int) config.tempValue + (0xFF << 24));
        }
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        this.textField.setX(x + getWidth() - 154);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        this.textField.setY(y);
    }

    @Override
    public boolean isDefault() {
        return textField.getField().getText().equals(formatText(config.defaultValue));
    }

    @Override
    public boolean isChanged() {
        return !textField.getField().getText().equals(formatText(initValue));
    }

    public TextField getTextField() {
        return textField;
    }

    public void updateTextFieldText(String text) {
        textField.getField().setText(text);
    }

    private String formatText(Object configValue) {
        return inputType == InputType.HEX_COLOR ? String.format("#%06X", (int) configValue) : configValue.toString();
    }

    private static class InputField extends TextField {

        private final InputElement parent;

        public InputField(int x, int y, int width, int height, HintType hint, InputElement parent, GuiPageButtonList.GuiResponder changedListener) {
            super(x, y, width, height, hint, changedListener);
            this.parent = parent;
        }

        @Override
        public boolean mouseOver(double mouseX, double mouseY) {
            return this.parent.isHovered() && super.mouseOver(mouseX, mouseY);
        }
    }
}
