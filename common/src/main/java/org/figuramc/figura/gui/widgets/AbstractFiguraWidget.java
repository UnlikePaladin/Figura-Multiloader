package org.figuramc.figura.gui.widgets;


import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import org.lwjgl.input.Mouse;

// There was no unified concept of a Widget back in the 1.12 days, therefore we for our sake we've made our own
public abstract class AbstractFiguraWidget extends Gui implements FiguraRenderable, FiguraGuiEventListener, FiguraNarratable {
    private ITextComponent messageToNarrate;
    float alpha = 1.0f;
    protected int x, y, width, height;
    private long timeToNextNarrationMessage = Long.MAX_VALUE;
    protected boolean active = true;
    public boolean visible = true;
    protected boolean isHovered;
    protected boolean focused;
    private final ResourceLocation VANILLA_WIDGETS = new ResourceLocation("textures/gui/widgets.png");
    private boolean wasHovered;

    AbstractFiguraWidget(int x, int y, int width, int height, ITextComponent message) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.messageToNarrate = message;
    }

    @Override
    public void draw(Minecraft minecraft, int mouseX, int mouseY, float delta) {
        if (!this.visible) return;
        drawWidget(minecraft, mouseX, mouseY, delta);
        isHovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;

        if (wasHovered != isHovered) {
            if (this.focused)
                this.queueNarration(200);
            else
                this.queueNarration(750);
        } else{
            this.queueNarration(Long.MAX_VALUE);
        }

        narrate();
        this.wasHovered = isHovered;
    }

    public void drawWidget(Minecraft minecraft, int mouseX, int mouseY, float delta) {
        FontRenderer font = minecraft.fontRenderer;
        minecraft.getTextureManager().bindTexture(VANILLA_WIDGETS);
        GlStateManager.color(1.0f, 1.0f, 1.0f, this.alpha);
        int uvOffset = 1;
        if (!this.active) {
            uvOffset = 0;
        } else if (isHovered) {
            uvOffset = 2;
        }
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.enableDepth();
        drawTexturedModalRect(this.x, this.y, 0, 46 + uvOffset * 20, this.width / 2, this.height);
        drawTexturedModalRect(this.x + this.width / 2, this.y, 200 - this.width / 2, 46 + uvOffset * 20, this.width / 2, this.height);
        this.drawBackground(minecraft, mouseX, mouseY, delta);

        int textColor = (this.active ? 0xFFFFFF : 0xA0A0A0) | MathHelper.ceil(this.alpha * 255.0f) << 24;
        drawCenteredString(font, this.getMessage().getFormattedText(), this.x + this.width / 2, this.y + (this.height - 8) / 2, textColor);
    }

    public void drawBackground(Minecraft minecraft, int mouseX, int mouseY, float delta) {
    }

    public void setMessage(ITextComponent message) {
        this.messageToNarrate = message;
    }

    public ITextComponent getMessage() {
        return messageToNarrate;
    }

    public boolean isHovered() {
        return isHovered || this.focused;
    }

    @Override
    public void queueNarration(long time) {
        this.timeToNextNarrationMessage = time;
    }

    public void narrate() {
        if (this.active && this.isHovered && Minecraft.getSystemTime() > timeToNextNarrationMessage && !getNarrationMessage().getUnformattedText().isEmpty()) {
            NARRATOR_INSTANCE.say(getNarrationMessage().getFormattedText());
            timeToNextNarrationMessage = Long.MAX_VALUE;
        }
    }

    @Override
    public ITextComponent getNarrationMessage() {
        return new TextComponentTranslation("gui.narrate.button", messageToNarrate.getFormattedText());
    }

    public void playPressedSound(SoundHandler handler) {
        handler.playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    public void widgetPressed(int mouseX, int mouseY) {

    }

    @Override
    public boolean mouseButtonClicked(int mouseX, int mouseY, int mouseButton) {
        if (!active || !visible)
            return false;

        if (mouseButton == 0 && clickedOnWidget(mouseX, mouseY)) {
            widgetPressed(mouseX, mouseY);
            playPressedSound(Minecraft.getMinecraft().getSoundHandler());
            return true;
        }
        return false;
    }

    protected boolean clickedOnWidget(double mouseX, double mouseY) {
        return this.active && this.visible && mouseX >= (double)this.x && mouseY >= (double)this.y && mouseX < (double)(this.x + this.width) && mouseY < (double)(this.y + this.height);
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getWidth() {
        return width;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public boolean focusChange(boolean focused) {
        if (!this.active || !this.visible) {
            return false;
        }
        this.focused = !this.focused;
        return this.focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public boolean isFocused() {
        return focused;
    }

    @Override
    public boolean mouseOver(double mouseX, double mouseY) {
        return this.active && this.visible && mouseX >= (double)this.x && mouseY >= (double)this.y && mouseX < (double)(this.x + this.width) && mouseY < (double)(this.y + this.height);
    }
}
