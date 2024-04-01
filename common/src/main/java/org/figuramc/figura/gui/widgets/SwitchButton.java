package org.figuramc.figura.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import org.figuramc.figura.utils.FiguraIdentifier;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.MathUtils;
import org.figuramc.figura.utils.ui.UIHelper;

public class SwitchButton extends Button {

    public static final ResourceLocation SWITCH_TEXTURE = new FiguraIdentifier("textures/gui/switch.png");
    public static final ITextComponent ON = new FiguraText("gui.on");
    public static final ITextComponent OFF = new FiguraText("gui.off");

    protected boolean toggled = false;
    private boolean defaultTexture = false;
    private boolean underline = true;
    private float headPos = 0f;

    // text constructor
    public SwitchButton(int x, int y, int width, int height, ITextComponent text, ITextComponent tooltip, ButtonAction pressAction) {
        super(x, y, width, height, text, tooltip, pressAction);
    }

    // texture constructor
    public SwitchButton(int x, int y, int width, int height, int u, int v, int interactionOffset, ResourceLocation texture, int textureWidth, int textureHeight, ITextComponent tooltip, ButtonAction pressAction) {
        super(x, y, width, height, u, v, interactionOffset, texture, textureWidth, textureHeight, tooltip, pressAction);
    }

    // default texture constructor
    public SwitchButton(int x, int y, int width, int height, ITextComponent text, boolean toggled) {
        super(x, y, width, height, text, null, button -> {});
        this.toggled = toggled;
        this.headPos = toggled ? 20f : 0f;
        defaultTexture = true;
    }

    @Override
    public void widgetPressed(int mouseX, int mouseY) {
        this.toggled = !this.toggled;
        super.widgetPressed(mouseX, mouseY);
    }

    @Override
    protected void renderText(Minecraft mc, float delta) {
        // draw text
        ITextComponent text = this.toggled && underline ? getMessage().createCopy().setStyle(new Style().setUnderlined(true)) : getMessage();
        int x = this.x + 1;
        int width = getWidth() - 2;

        if (defaultTexture) {
            x += 31;
            width -= 31;
        }

        UIHelper.renderCenteredScrollingText(text, x, this.y, width, getHeight(), getTextColor());
    }

    @Override
    protected void renderDefaultTexture(Minecraft mc, float delta) {
        if (!defaultTexture) {
            super.renderDefaultTexture(mc, delta);
            return;
        }

        // set texture
        UIHelper.setupTexture(SWITCH_TEXTURE);
        int x = getX();
        int y = getY();

        // render switch
        UIHelper.blit(x + 5, y + 5, 20, 10, 10f, (this.toggled ? 20f : 0f) + ((this.isHovered()) ? 10f : 0f), 20, 10, 30, 40);

        // render head
        headPos = (float) MathUtils.lerp(1f - Math.pow(0.2f, delta), headPos, this.toggled ? 20f : 0f);
        UIHelper.blit(Math.round(x + headPos), y, 10, 20, 0f, (this.isHovered()) ? 20f : 0f, 10, 20, 30, 40);
    }

    @Override
    protected int getV() {
        return isToggled() ? 1 : super.getV();
    }

    public boolean isToggled() {
        return this.toggled;
    }

    public void setToggled(boolean toggled) {
        this.toggled = toggled;
    }

    public void setUnderline(boolean underline) {
        this.underline = underline;
    }
}
