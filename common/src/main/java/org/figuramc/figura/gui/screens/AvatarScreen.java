package org.figuramc.figura.gui.screens;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.EntityLivingBase;
import org.figuramc.figura.gui.widgets.EntityPreview;
import org.figuramc.figura.utils.FiguraText;

public class AvatarScreen extends AbstractPanelScreen {

    private final float scale;
    private final float pitch;
    private final float yaw;
    private final EntityLivingBase entity;

    public AvatarScreen(float scale, float pitch, float yaw, EntityLivingBase entity, GuiScreen parentScreen) {
        super(parentScreen, new FiguraText("gui.panels.title.avatar"));
        this.scale = scale;
        this.pitch = pitch;
        this.yaw = yaw;
        this.entity = entity;
    }

    @Override
    public Class<? extends GuiScreen> getSelectedPanel() {
        return parentScreen.getClass();
    }

    @Override
    public void initGui() {
        super.initGui();
        removeWidget(panels); // no panels :p

        // entity
        EntityPreview widget = new EntityPreview(0, 0, width, height, scale, pitch, yaw, entity, parentScreen);
        widget.setToggled(true);
        addRenderableWidget(widget);
    }
}
