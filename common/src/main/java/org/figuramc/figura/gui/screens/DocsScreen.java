package org.figuramc.figura.gui.screens;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.TextComponentString;
import org.figuramc.figura.gui.widgets.Label;
import org.figuramc.figura.utils.ColorUtils;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.TextUtils;

public class DocsScreen extends AbstractPanelScreen {

    private final GuiScreen sourcePanel;

    public DocsScreen(AbstractPanelScreen parentScreen) {
        super(parentScreen.parentScreen, new FiguraText("gui.panels.title.docs"));
        sourcePanel = parentScreen;
    }

    @Override
    public Class<? extends GuiScreen> getSelectedPanel() {
        return sourcePanel.getClass();
    }

    @Override
    public void initGui() {
        super.initGui();
        this.addRenderableWidget(new Label(new TextComponentString("").appendText("Still not finished :s"), width / 2, height / 2, 3f, 200, true, TextUtils.Alignment.CENTER, ColorUtils.Colors.AWESOME_BLUE.hex));
    }
}
