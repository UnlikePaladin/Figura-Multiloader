package org.figuramc.figura.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.gui.screens.AvatarScreen;
import org.figuramc.figura.model.rendering.EntityRenderMode;
import org.figuramc.figura.utils.FiguraIdentifier;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.MathUtils;
import org.figuramc.figura.utils.ui.UIHelper;

public class EntityPreview extends AbstractContainerElement {

    public static final ResourceLocation UNKNOWN = new FiguraIdentifier("textures/gui/unknown_entity.png");
    public static final ResourceLocation OVERLAY = new FiguraIdentifier("textures/gui/entity_overlay.png");

    // properties
    private EntityLivingBase entity;
    private final float pitch, yaw, scale;
    private SwitchButton button;

    // transformation data

    // rot
    private boolean isRotating = false;
    private float anchorX = 0f, anchorY = 0f;
    private float anchorAngleX = 0f, anchorAngleY = 0f;
    private float angleX, angleY;

    // scale
    private float scaledValue = 0f, scaledPrecise = 0f;
    private static final float SCALE_FACTOR = 1.1F;

    // pos
    private boolean isDragging = false;
    private int modelX, modelY;
    private float dragDeltaX, dragDeltaY;
    private float dragAnchorX, dragAnchorY;

    public EntityPreview(int x, int y, int width, int height, float scale, float pitch, float yaw, EntityLivingBase entity, GuiScreen parentScreen) {
        super(x, y, width, height);

        this.scale = scale;
        this.pitch = pitch;
        this.yaw = yaw;
        this.entity = entity;

        modelX = width / 2;
        modelY = height / 2;
        angleX = pitch;
        angleY = yaw;

        // button
        children.add(button = new SwitchButton(
                x + 4, y + 4, 16, 16,
                0, 0, 16,
                new FiguraIdentifier("textures/gui/expand.png"),
                48, 32,
                new FiguraText("gui.expand"),
                bx -> {
                    if (button.isToggled()) {
                        Minecraft.getMinecraft().displayGuiScreen(new AvatarScreen(scale, pitch, yaw, this.entity, parentScreen));
                    } else {
                        Minecraft.getMinecraft().displayGuiScreen(parentScreen);
                    }
                }));
    }

    @Override
    public void draw(Minecraft minecraft, int mouseX, int mouseY, float delta) {
        if (!this.isVisible())
            return;

        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();

        if (!button.isToggled()) {
            // border
            UIHelper.renderSliced(x, y, width, height, UIHelper.OUTLINE_FILL);
            // overlay
            UIHelper.renderTexture(x + 1, y + 1, width - 2, height - 2, OVERLAY);
        }

        // scissors
        UIHelper.setupScissor(x + 1, y + 1, width - 2, height - 2);

        // render entity
        if (entity != null) {
            GlStateManager.pushMatrix();
            scaledValue = (float) MathUtils.lerp((float) (1f - Math.pow(0.5f, delta)), scaledValue, scaledPrecise);
            UIHelper.drawEntity(x + modelX, y + modelY, scale + scaledValue, angleX, angleY, entity, EntityRenderMode.FIGURA_GUI);
            GlStateManager.popMatrix();
        } else {
            // draw
            int s = Math.min(width, height) * 2 / 3;
            UIHelper.setupTexture(UNKNOWN);
            UIHelper.blit(x + (width - s) / 2, y + (height - s) / 2, s, s, 0f, 64 * ((int) (FiguraMod.ticks / 3f) % 8), 64, 64, 64, 512);
        }

        UIHelper.disableScissor();

        super.draw(minecraft, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseButtonClicked(int mouseX, int mouseY, int button) {
        if (!this.isVisible() || !this.mouseOver(mouseX, mouseY))
            return false;

        if (super.mouseButtonClicked(mouseX, mouseY, button))
            return true;

        switch (button) {
            // left click - rotate
            case 0: {
                // set anchor rotation

                // get starter mouse pos
                anchorX = (float) mouseX;
                anchorY = (float) mouseY;

                // get starter rotation angles
                anchorAngleX = angleX;
                anchorAngleY = angleY;

                isRotating = true;
                return true;
            }

            // right click - move
            case 1: {
                // get starter mouse pos
                dragDeltaX = (float) mouseX;
                dragDeltaY = (float) mouseY;

                // also get start node pos
                dragAnchorX = modelX;
                dragAnchorY = modelY;

                isDragging = true;
                return true;
            }

            // middle click - reset pos
            case 2: {
                isRotating = false;
                isDragging = false;
                anchorX = 0f;
                anchorY = 0f;
                anchorAngleX = 0f;
                anchorAngleY = 0f;
                angleX = pitch;
                angleY = yaw;
                scaledValue = 0f;
                scaledPrecise = 0f;
                modelX = getWidth() / 2;
                modelY = getHeight() / 2;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseButtonReleased(int mouseX, int mouseY, int button) {
        // left click - stop rotating
        if (button == 0) {
            isRotating = false;
            return true;
        }

        // right click - stop dragging
        else if (button == 1) {
            isDragging = false;
            return true;
        }

        return super.mouseButtonReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseDragged(Minecraft minecraft, int mouseX, int mouseY, int button, double dragX, double dragY) {
        // left click - rotate
        if (isRotating) {
            // get starter rotation angle then get hot much is moved and divided by a slow factor
            ScaledResolution scaledResolution = new ScaledResolution(minecraft);
            angleX = (float) (anchorAngleX + (anchorY - mouseY) / (3 / scaledResolution.getScaleFactor()));
            angleY = (float) (anchorAngleY - (anchorX - mouseX) / (3 / scaledResolution.getScaleFactor()));

            // cap to 360, so we don't get extremely high unnecessary rotation values
            if (angleX >= 360 || angleX <= -360) {
                anchorY = (float) mouseY;
                anchorAngleX = 0;
                angleX = 0;
            }
            if (angleY >= 360 || angleY <= -360) {
                anchorX = (float) mouseX;
                anchorAngleY = 0;
                angleY = 0;
            }

            return;
        }

        // right click - move
        else if (isDragging) {
            // get how much it should move
            // get actual pos of the mouse, then subtract starter X,Y
            float x = (float) (mouseX - dragDeltaX);
            float y = (float) (mouseY - dragDeltaY);

            // move it
            modelX = (int) (dragAnchorX + x);
            modelY = (int) (dragAnchorY + y);

            return;
        }

        super.mouseDragged(minecraft, mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScroll(double mouseX, double mouseY, double amount) {
        if (!this.isVisible())
            return false;

        if (super.mouseScroll(mouseX, mouseY, amount))
            return true;

        // scroll - scale

        // set scale direction
        float scaleDir = (amount > 0) ? SCALE_FACTOR : 1 / SCALE_FACTOR;

        // determine scale
        scaledPrecise = ((scale + scaledPrecise) * scaleDir) - scale;

        return true;
    }

    public void setEntity(EntityLivingBase entity) {
        this.entity = entity;
    }

    public void setToggled(boolean toggled) {
        this.button.setToggled(toggled);
        this.button.setTooltip(toggled ? new FiguraText("gui.minimise") : new FiguraText("gui.expand"));
    }
}
