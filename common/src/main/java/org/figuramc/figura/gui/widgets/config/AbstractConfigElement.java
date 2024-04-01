package org.figuramc.figura.gui.widgets.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import org.figuramc.figura.config.ConfigType;
import org.figuramc.figura.gui.widgets.AbstractContainerElement;
import org.figuramc.figura.gui.widgets.Button;
import org.figuramc.figura.gui.widgets.ParentedButton;
import org.figuramc.figura.gui.widgets.lists.ConfigList;
import org.figuramc.figura.mixin.font.FontRendererAccessor;
import org.figuramc.figura.utils.ui.UIHelper;

import java.util.Objects;

public abstract class AbstractConfigElement extends AbstractContainerElement {

    protected final ConfigType<?> config;
    protected final ConfigList parentList;
    protected final CategoryWidget parentCategory;

    protected Button resetButton;

    protected Object initValue;

    private String filter = "";

    public AbstractConfigElement(int width, ConfigType<?> config, ConfigList parentList, CategoryWidget parentCategory) {
        super(0, 0, width, 20);
        this.config = config;
        this.parentList = parentList;
        this.parentCategory = parentCategory;
        this.initValue = config.value;

        // reset button
        children.add(resetButton = new ParentedButton(0, 0, 60, 20, new TextComponentTranslation("controls.reset"), this, button -> config.resetTemp()));
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY, float delta) {
        if (!this.isVisible()) return;

        // vars
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int textY = getY() + getHeight() / 2 - font.FONT_HEIGHT / 2;

        // hovered arrow
        setHovered(mouseOver(mouseX, mouseY));
        if (isHovered()) font.drawString(HOVERED_ARROW.getFormattedText(), (int) (getX() + 8 - font.getStringWidth(HOVERED_ARROW.getFormattedText()) / 2f), textY, 0xFFFFFF);

        // render name
        renderTitle(font, textY);

        // render children
        super.draw(mc, mouseX, mouseY, delta);
    }

    public void renderTitle(FontRenderer font, int y) {
        font.drawString(config.name.getFormattedText(), getX() + 16, y, ((FontRendererAccessor)font).getColors()[(config.disabled ? TextFormatting.DARK_GRAY : TextFormatting.WHITE).getColorIndex()]);
    }

    @Override
    public boolean mouseOver(double mouseX, double mouseY) {
        boolean over = this.parentList.isInsideScissors(mouseX, mouseY) && super.mouseOver(mouseX, mouseY);

        if (over && mouseX < this.getX() + this.getWidth() - 158)
            UIHelper.setTooltip(getTooltip());

        return over;
    }

    public ITextComponent getTooltip() {
        return config.tooltip.createCopy();
    }

    public boolean isDefault() {
        return this.config.isDefault();
    }

    public boolean isChanged() {
        return !Objects.equals(this.config.tempValue, this.initValue);
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        resetButton.setX(x + getWidth() - 60);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        resetButton.setY(y);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible && parentCategory.isShowingChildren() && matchesFilter());
    }

    public void updateFilter(String query) {
        this.filter = query;
    }

    public boolean matchesFilter() {
        return config.name.getFormattedText().toLowerCase().contains(filter) || config.tooltip.getFormattedText().toLowerCase().contains(filter);
    }
}
