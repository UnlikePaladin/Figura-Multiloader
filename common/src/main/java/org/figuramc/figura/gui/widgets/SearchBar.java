package org.figuramc.figura.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiPageButtonList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.figuramc.figura.ducks.extensions.StyleExtension;
import org.figuramc.figura.mixin.gui.GuiTextFieldAccessor;
import org.figuramc.figura.utils.FiguraIdentifier;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.ui.UIHelper;

public class SearchBar extends TextField {

    public static final ResourceLocation CLEAR_TEXTURE = new FiguraIdentifier("textures/gui/search_clear.png");
    public static final ITextComponent SEARCH_ICON = new TextComponentString("\uD83D\uDD0E").setStyle(((StyleExtension)new Style()).setFont(UIHelper.UI_FONT).setColor(TextFormatting.DARK_GRAY));

    private final Button clearButton;

    public SearchBar(int x, int y, int width, int height, GuiPageButtonList.GuiResponder changedListener) {
        super(x, y, width, height, TextField.HintType.SEARCH, changedListener);
        clearButton = new Button(getX() + getWidth() - 18, getY() + ((getHeight() - 16) / 2), 16, 16, 0, 0, 16, CLEAR_TEXTURE, 48, 16, new FiguraText("gui.clear"), button -> {
            getField().setText("");
            setFocused(null);
        });
        children.add(clearButton);
        ((GuiTextFieldAccessor)getField()).setWidth(getField().getWidth() - 16);
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY, float delta) {
        clearButton.setVisible(!getField().getText().isEmpty());
        super.draw(mc, mouseX, mouseY, delta);
    }

    @Override
    protected void renderHint(Minecraft mc) {
        super.renderHint(mc);
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        font.drawStringWithShadow(SEARCH_ICON.getFormattedText(), getX() + getWidth() - font.getStringWidth(SEARCH_ICON.getFormattedText()) - 4, getY() + (int) ((getHeight() - font.FONT_HEIGHT + 1) / 2f), 0xFFFFFF);
    }

    @Override
    public boolean mouseButtonClicked(int mouseX, int mouseY, int button) {
        return (!clearButton.isVisible() || !clearButton.mouseButtonClicked(mouseX, mouseY, button)) && super.mouseButtonClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseOver(double mouseX, double mouseY) {
        return (!clearButton.isVisible() || !clearButton.mouseOver(mouseX, mouseY)) && super.mouseOver(mouseX, mouseY);
    }
}
