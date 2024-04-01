package org.figuramc.figura.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiPageButtonList;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import org.figuramc.figura.mixin.font.FontRendererAccessor;
import org.figuramc.figura.mixin.gui.GuiTextFieldAccessor;
import org.figuramc.figura.utils.FiguraIdentifier;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.TextUtils;
import org.figuramc.figura.utils.ui.UIHelper;

public class TextField extends AbstractContainerElement {

    public static final ResourceLocation BACKGROUND = new FiguraIdentifier("textures/gui/text_field.png");
    public static Integer ENABLED_COLOR = null;
    public static Integer DISABLED_COLOR = null;

    public static int getEnabledColor() {
        if (ENABLED_COLOR == null) {
            ENABLED_COLOR = ((FontRendererAccessor)Minecraft.getMinecraft().fontRenderer).getColors()[TextFormatting.WHITE.getColorIndex()];
        }
        return ENABLED_COLOR;
    }

    public static int getDisabledColor() {
        if (DISABLED_COLOR == null) {
            DISABLED_COLOR = ((FontRendererAccessor)Minecraft.getMinecraft().fontRenderer).getColors()[TextFormatting.DARK_GRAY.getColorIndex()];
        }
        return DISABLED_COLOR;
    }

    private final HintType hint;
    private final GuiTextField field;
    private int borderColour = 0xFFFFFFFF;
    private boolean enabled = true;

    public TextField(int x, int y, int width, int height, HintType hint, GuiPageButtonList.GuiResponder changedListener) {
        super(x, y, width, height);
        this.hint = hint;

        field = new GuiTextField(0, Minecraft.getMinecraft().fontRenderer, x + 4, y + (height - 8) / 2, width - 12, height - (height - 8) / 2);
        field.setText("");
        field.setMaxStringLength(32767);
        field.setEnableBackgroundDrawing(false);
        field.setGuiResponder(changedListener);
    }

    @Override
    public void tick() {
        field.updateCursorCounter();
        super.tick();
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY, float delta) {
        if (!isVisible()) return;

        // render background
        UIHelper.renderSliced(getX(), getY(), getWidth(), getHeight(), !isEnabled() ? 0f : this.mouseOver(mouseX, mouseY) ? 32f : 16f, 0f, 16, 16, 48, 16, BACKGROUND);

        // render outline
        if (isFocused())
            UIHelper.fillOutline(getX(), getY(), getWidth(), getHeight(), borderColour);

        // hint text
        if (hint != null && field.getText().isEmpty() && !field.isFocused())
            renderHint(mc);

        // children
        super.draw(mc, mouseX, mouseY, delta);
        field.drawTextBox();
    }

    protected void renderHint(Minecraft mc) {
        FontRenderer font = mc.fontRenderer;
        font.drawStringWithShadow(
                hint.hint.createCopy().appendSibling(TextUtils.ELLIPSIS).setStyle(new Style().setColor(TextFormatting.DARK_GRAY).setItalic(true)).getUnformattedText(),
                getX() + 4, getY() + (int) ((getHeight() - font.FONT_HEIGHT + 1) / 2f), 0xFFFFFF
        );
    }

    @Override
    public boolean mouseButtonClicked(int mouseX, int mouseY, int button) {
        // mouse over check
        if (!isEnabled() || !this.mouseOver(mouseX, mouseY))
            return false;

        // hacky
        mouseX = MathHelper.clamp(mouseX, field.x, field.x + field.getWidth() - 1);
        mouseY = MathHelper.clamp(mouseY, field.y, field.y + ((GuiTextFieldAccessor)field).getHeight() - 1);

        return super.mouseButtonClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseButtonReleased(int mouseX, int mouseY, int button) {
        return !field.isFocused();
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        this.field.x = (x + 4);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        this.field.y = y + (this.getHeight() - 8) / 2;
    }

    public void setBorderColour(int borderColour) {
        this.borderColour = borderColour;
    }

    public int getBorderColour() {
        return borderColour;
    }

    public GuiTextField getField() {
        return field;
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible == isVisible())
            return;

        super.setVisible(visible);
        this.field.setFocused(false);
    }

    public void setColor(int color) {
        this.field.setTextColor(enabled ? color : getDisabledColor());
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        setColor(getEnabledColor());
    }

    @Override
    public boolean focusChange(boolean bl) {
        if (!field.getVisible() || !((GuiTextFieldAccessor)field).isEnabled()) return false;

        this.field.setFocused(bl);
        return true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isFocused() {
        return isEnabled() && field.isFocused();
    }

    public enum HintType {
        ANY,
        INT,
        POSITIVE_INT,
        FLOAT,
        POSITIVE_FLOAT,
        HEX_COLOR,
        FOLDER_PATH,
        IP,
        SEARCH,
        NAME;

        private final ITextComponent hint;

        HintType() {
            this.hint = new FiguraText("gui.text_hint." + this.name().toLowerCase());
        }
    }
}
