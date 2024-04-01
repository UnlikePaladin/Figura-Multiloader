package org.figuramc.figura.gui.screens;

import net.minecraft.client.gui.GuiScreen;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.gui.widgets.Button;
import org.figuramc.figura.gui.widgets.lists.KeybindList;
import org.figuramc.figura.lua.api.keybind.FiguraKeybind;
import org.figuramc.figura.utils.FiguraText;
import org.lwjgl.input.Keyboard;

public class KeybindScreen extends AbstractPanelScreen {

    private final GuiScreen sourcePanel;

    private KeybindList list;

    public KeybindScreen(AbstractPanelScreen parentScreen) {
        super(parentScreen.parentScreen, new FiguraText("gui.panels.title.keybind"));
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

        // -- bottom buttons -- //

        // reset
        Button reset;
        this.addRenderableWidget(reset = new Button(width / 2 - 122, height - 24, 120, 20, new FiguraText("gui.reset_all"), null, button -> {
            if (owner == null || owner.luaRuntime == null)
                return;

            for (FiguraKeybind keybind : owner.luaRuntime.keybinds.keyBindings)
                keybind.resetDefaultKey();
            list.updateBindings();
        }));
        reset.setActive(false);

        // back
        addRenderableWidget(new Button(width / 2 + 4, height - 24, 120, 20, new FiguraText("gui.done"), null, bx -> onGuiClosed()));

        // -- list -- //

        int listWidth = Math.min(this.width - 8, 420);
        this.addRenderableWidget(list = new KeybindList((this.width - listWidth) / 2, 28, listWidth, height - 56, owner, reset));
    }

    @Override
    public void onGuiClosed() {
        this.mc.displayGuiScreen(sourcePanel);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        if(list.updateKey(-100+button))
            return;

        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void keyTyped(char c, int scanCode) {
        if (list.updateKey(scanCode == Keyboard.KEY_ESCAPE ? 0 : scanCode))
            return;

        super.keyTyped(c, scanCode);
    }
}
