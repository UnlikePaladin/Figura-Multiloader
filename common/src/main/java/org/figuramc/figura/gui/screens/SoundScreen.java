package org.figuramc.figura.gui.screens;

import net.minecraft.client.gui.GuiScreen;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.gui.widgets.Button;
import org.figuramc.figura.gui.widgets.PianoWidget;
import org.figuramc.figura.gui.widgets.lists.SoundsList;
import org.figuramc.figura.utils.FiguraText;

public class SoundScreen extends AbstractPanelScreen {

    private final GuiScreen sourcePanel;
    private PianoWidget piano;

    public SoundScreen(AbstractPanelScreen parentScreen) {
        super(parentScreen.parentScreen, new FiguraText("gui.panels.title.sound"));
        sourcePanel = parentScreen;
    }

    @Override
    public Class<? extends GuiScreen> getSelectedPanel() {
        return sourcePanel.getClass();
    }

    @Override
    public void initGui() {
        super.initGui();

        Avatar owner = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());

        // list
        int listWidth = Math.min(this.width - 8, 420);
        int listX = (this.width - listWidth) / 2;
        SoundsList list;
        addRenderableWidget(list = new SoundsList(listX, 28, listWidth, height - 120, owner));

        // keys
        addRenderableWidget(piano = new PianoWidget(listX, height - 88, listWidth, 60, list::getSound));

        // back
        addRenderableWidget(new Button(width / 2 - 60, height - 24, 120, 20, new FiguraText("gui.done"), null, bx -> onGuiClosed()));
    }

    @Override
    public void onGuiClosed() {
        this.mc.displayGuiScreen(sourcePanel);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        piano.pressed = button == 0;
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int button) {
        piano.pressed = false;
        super.mouseReleased(mouseX, mouseY, button);
    }
}
