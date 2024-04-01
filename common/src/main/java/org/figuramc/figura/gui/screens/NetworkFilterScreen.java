package org.figuramc.figura.gui.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.figuramc.figura.config.ConfigManager;
import org.figuramc.figura.config.ConfigType;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.gui.widgets.Label;
import org.figuramc.figura.gui.widgets.lists.NetworkFilterList;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.TextUtils;

public class NetworkFilterScreen extends AbstractPanelScreen {
    private final ConfigType.NetworkFilterConfig config = Configs.NETWORK_FILTER;
    private final Label titleLabel;
    private NetworkFilterList networkFilterList;
    public NetworkFilterScreen(GuiScreen parentScreen) {
        super(parentScreen, new FiguraText("gui.network_filter"));
        titleLabel = new Label(this.getTitle(), 0, 0, TextUtils.Alignment.CENTER);
    }

    @Override
    public void initGui() {
        titleLabel.setX(width / 2);
        titleLabel.setY(8 + titleLabel.getHeight() / 2 );
        int listWidth = Math.min(420, this.width - 8);
        addRenderableWidget(networkFilterList =
                new NetworkFilterList((this.width - listWidth) / 2, titleLabel.getY() + (titleLabel.getHeight() / 2) + 8,
                        listWidth, height - (titleLabel.getY() + (titleLabel.getHeight() / 2) + 16), config));
        addRenderableOnly(titleLabel);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float delta) {
        super.drawScreen(mouseX, mouseY, delta);
    }

    protected void repositionElements() {
        titleLabel.setX(width / 2);
        titleLabel.setY(8 + titleLabel.getHeight() / 2 );
        int listWidth = Math.min(420, width - 8);
        networkFilterList.setX((width - listWidth) / 2);
        networkFilterList.setY(titleLabel.getY() + (titleLabel.getHeight() / 2) + 8);
        networkFilterList.setWidth(listWidth);
        networkFilterList.setHeight(height - (titleLabel.getY() + (titleLabel.getHeight() / 2) + 16));
    }

    @Override
    public void onResize(Minecraft minecraft, int width, int height) {
        super.onResize(minecraft, width, height);
        repositionElements();
    }

    @Override
    public void onGuiClosed() {
        ConfigManager.applyConfig();
        ConfigManager.saveConfig();
        super.onGuiClosed();
    }
}