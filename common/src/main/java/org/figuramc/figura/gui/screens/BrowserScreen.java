package org.figuramc.figura.gui.screens;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.TextComponentString;
import org.figuramc.figura.gui.FiguraToast;
import org.figuramc.figura.gui.widgets.Button;
import org.figuramc.figura.utils.FiguraText;

public class BrowserScreen extends AbstractPanelScreen {

    public BrowserScreen(GuiScreen parentScreen) {
        super(parentScreen, new FiguraText("gui.panels.title.browser"));
    }

    @Override
    public void initGui() {
        super.initGui();

        int y = -84;
        this.addRenderableWidget(new Button(width / 2 - 50, height / 2 + (y += 24), 100, 20, new TextComponentString("default toast"), new FiguraText("backend.error"), button -> {
            FiguraToast.sendToast("default", "test", FiguraToast.ToastType.DEFAULT);
        }));
        this.addRenderableWidget(new Button(width / 2 - 50, height / 2 + (y += 24), 100, 20, new TextComponentString("error toast"), new TextComponentString("test2"), button -> {
            FiguraToast.sendToast("error", "test", FiguraToast.ToastType.ERROR);
        }));
        this.addRenderableWidget(new Button(width / 2 - 50, height / 2 + (y += 24), 100, 20, new TextComponentString("warning toast"), new TextComponentString("test3\novo"), button -> {
            FiguraToast.sendToast("warning", "test", FiguraToast.ToastType.WARNING);
        }));
        this.addRenderableWidget(new Button(width / 2 - 50, height / 2 + (y += 24), 100, 20, new TextComponentString("cheese toast"), new TextComponentString("test4\n\nhehe"), button -> {
            FiguraToast.sendToast("cheese", "test", FiguraToast.ToastType.CHEESE);
        }));
    }
}
