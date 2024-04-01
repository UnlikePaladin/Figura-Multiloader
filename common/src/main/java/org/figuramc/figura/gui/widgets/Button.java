package org.figuramc.figura.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.figuramc.figura.mixin.font.FontRendererAccessor;
import org.figuramc.figura.utils.FiguraIdentifier;
import org.figuramc.figura.utils.ui.UIHelper;

public class Button extends AbstractFiguraWidget implements FiguraWidget {

    // default textures
    private static final ResourceLocation TEXTURE = new FiguraIdentifier("textures/gui/button.png");

    // texture data
    protected Integer u;
    protected Integer v;

    protected final Integer textureWidth;
    protected final Integer textureHeight;
    protected final Integer regionSize;
    protected final ResourceLocation texture;

    // extra fields
    protected ITextComponent tooltip;
    private boolean hasBackground = true;
    private final ButtonAction pressAction;

    // texture and text constructor
    public Button(int x, int y, int width, int height, Integer u, Integer v, Integer regionSize, ResourceLocation texture, Integer textureWidth, Integer textureHeight, ITextComponent text, ITextComponent tooltip, ButtonAction pressAction) {
        super(x, y, width, height, text);
        this.pressAction = pressAction;
        this.u = u;
        this.v = v;
        this.regionSize = regionSize;
        this.texture = texture;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.tooltip = tooltip;
    }

    // text constructor
    public Button(int x, int y, int width, int height, ITextComponent text, ITextComponent tooltip, ButtonAction pressAction) {
        this(x, y, width, height, null, null, null, null, null, null, text, tooltip, pressAction);
    }

    // texture constructor
    public Button(int x, int y, int width, int height, int u, int v, int regionSize, ResourceLocation texture, int textureWidth, int textureHeight, ITextComponent tooltip, ButtonAction pressAction) {
        this(x, y, width, height, u, v, regionSize, texture, textureWidth, textureHeight, new TextComponentString(""), tooltip, pressAction);
    }

    @Override
    public void draw(Minecraft minecraft, int mouseX, int mouseY, float delta) {
        if (!this.isVisible())
            return;

        // update hovered
        this.setHovered(this.mouseOver(mouseX, mouseY));

        // render button
        this.drawWidget(minecraft, mouseX, mouseY, delta);
    }

    @Override
    public void drawWidget(Minecraft minecraft, int mouseX, int mouseY, float delta) {
        // render texture
        if (this.texture != null) {
            renderTexture(minecraft, delta);
        } else {
            renderDefaultTexture(minecraft, delta);
        }

        // render text
        renderText(minecraft, delta);
    }

    @Override
    public boolean mouseButtonClicked(int mouseX, int mouseY, int button) {
        return (this.isFocused() || this.isHovered()) && this.mouseOver(mouseX, mouseY) && super.mouseButtonClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseOver(double mouseX, double mouseY) {
        boolean over = UIHelper.isMouseOver(getX(), getY(), getWidth(), getHeight(), mouseX, mouseY);
        if (over && this.tooltip != null)
            UIHelper.setTooltip(this.tooltip);
        return over;
    }

    protected void renderDefaultTexture(Minecraft mc, float delta) {
        UIHelper.renderSliced(getX(), getY(), getWidth(), getHeight(), getU() * 16f, getV() * 16f, 16, 16, 48, 32, TEXTURE);
    }

    protected void renderTexture(Minecraft mc, float delta) {
        // uv transforms
        int u = this.u + this.getU() * this.regionSize;
        int v = this.v + this.getV() * this.regionSize;

        // draw texture
        UIHelper.setupTexture(this.texture);

        int size = this.regionSize;
        UIHelper.blit(this.getX() + this.getWidth() / 2 - size / 2, this.getY() + this.getHeight() / 2 - size / 2, u, v, size, size, this.textureWidth, this.textureHeight);
    }

    protected void renderText(Minecraft mc, float delta) {
        UIHelper.renderCenteredScrollingText(getMessage(), getX() + 1, getY(), getWidth() - 2, getHeight(), getTextColor());
    }

    protected void renderVanillaBackground(Minecraft mc, int mouseX, int mouseY, float delta) {
        ITextComponent message = getMessage();
        setMessage(new TextComponentString(""));
        super.drawWidget(mc, mouseX, mouseY, delta);
        setMessage(message);
    }

    protected int getU() {
        if (!this.isActive())
            return 0;
        else if ((this.isHovered()))
            return 2;
        else
            return 1;
    }

    protected int getV() {
        return hasBackground ? 0 : 1;
    }

    protected int getTextColor() {
        return ((FontRendererAccessor)Minecraft.getMinecraft().fontRenderer).getColors()[(!this.isActive() ? TextFormatting.DARK_GRAY : TextFormatting.WHITE).getColorIndex()];
    }

    public void setTooltip(ITextComponent tooltip) {
        this.tooltip = tooltip;
    }

    public ITextComponent getTooltip() {
        return tooltip;
    }

    public void shouldHaveBackground(boolean bool) {
        this.hasBackground = bool;
    }

    public void setHovered(boolean hovered) {
        this.isHovered = hovered;
    }

    public void run() {
        playPressedSound(Minecraft.getMinecraft().getSoundHandler());
        widgetPressed(0, 0);
    }

    @Override
    public boolean isVisible() {
        return this.visible;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public int getX() {
        return this.x;
    }

    @Override
    public void setX(int x) {
        this.x = x;
    }

    @Override
    public int getY() {
        return this.y;
    }

    @Override
    public void setY(int y) {
        this.y = y;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public void widgetPressed(int mouseX, int mouseY) {
        super.widgetPressed(mouseX, mouseY);
        pressAction.onPress(this);
    }

    public interface ButtonAction {
        void onPress(Button button);
    }
}
