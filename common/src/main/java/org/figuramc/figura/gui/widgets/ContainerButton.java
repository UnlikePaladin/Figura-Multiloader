package org.figuramc.figura.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import org.figuramc.figura.gui.widgets.lists.AbstractList;
import org.figuramc.figura.mixin.font.FontRendererAccessor;
import org.figuramc.figura.utils.TextUtils;
import org.figuramc.figura.utils.ui.UIHelper;

public class ContainerButton extends SwitchButton {

    private final AbstractList parent;

    public ContainerButton(AbstractList parent, int x, int y, int width, int height, ITextComponent text, ITextComponent tooltip, ButtonAction pressAction) {
        super(x, y, width, height, text, tooltip, pressAction);
        this.parent = parent;
    }

    @Override
    protected void renderText(Minecraft mc, float delta) {
        // variables
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int color = getTextColor();
        ITextComponent arrow = this.toggled ? UIHelper.DOWN_ARROW : UIHelper.UP_ARROW;
        int arrowWidth = font.getStringWidth(arrow.getFormattedText());
        ITextComponent message = TextUtils.trimToWidthEllipsis(font, getMessage(), this.getWidth() - arrowWidth - 6, TextUtils.ELLIPSIS.createCopy().setStyle(getMessage().getStyle()));

        // draw text
        font.drawStringWithShadow(
                message.getFormattedText(),
                this.getX() + arrowWidth + 6, (int) (this.getY() + this.getHeight() / 2f - font.FONT_HEIGHT / 2f),
                color
        );

        // draw arrow
        font.drawStringWithShadow(
                arrow.getFormattedText(),
                this.getX() + 3, (int) (this.getY() + this.getHeight() / 2f - font.FONT_HEIGHT / 2f),
                color
        );

        // tooltip
        if (message != getMessage())
            this.setTooltip(getMessage());
    }

    @Override
    public boolean mouseOver(double mouseX, double mouseY) {
        return this.parent.isInsideScissors(mouseX, mouseY) && super.mouseOver(mouseX, mouseY);
    }

    @Override
    protected int getTextColor() {
        return !this.isToggled() ? ((FontRendererAccessor)Minecraft.getMinecraft().fontRenderer).getColors()[TextFormatting.DARK_GRAY.getColorIndex()] : super.getTextColor();
    }
}
