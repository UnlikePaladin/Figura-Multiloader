package org.figuramc.figura.gui.screens;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.gui.FiguraToast;
import org.figuramc.figura.gui.widgets.Button;
import org.figuramc.figura.gui.widgets.Label;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.TextUtils;
import org.lwjgl.input.Keyboard;

import java.util.function.Function;

public class FiguraConfirmScreen extends AbstractPanelScreen {

    private final Function<Boolean, Void> callback;
    private final ITextComponent message;

    public FiguraConfirmScreen(Function<Boolean, Void> callback, Object title, Object message, GuiScreen parentScreen) {
        super(parentScreen, title instanceof ITextComponent ? (ITextComponent) title : new TextComponentString(title.toString()));
        this.callback = callback;
        this.message = message instanceof ITextComponent ? (ITextComponent) message : new TextComponentString(message.toString()).setStyle(FiguraMod.getAccentColor());
    }

    @Override
    public void initGui() {
        super.initGui();
        removeWidget(panels);

        // labels
        int center = this.width / 2;
        Label title = new Label(this.getTitle(), center, 0, width - 8, true, TextUtils.Alignment.CENTER);
        Label message = new Label(this.message, center, 0, width - 8, true, TextUtils.Alignment.CENTER);

        int titleY = (this.height - message.getHeight()) / 2;
        titleY = Math.min(Math.max(titleY - 29, 4), 80);
        int messageY = titleY + 20;

        title.setY(titleY);
        message.setY(messageY);

        addRenderableWidget(title);
        addRenderableWidget(message);

        // buttons
        addButtons(center, Math.min(Math.max(messageY + message.getHeight() + 20, this.height / 6 + 96), this.height - 24));
    }

    protected void addButtons(int x, int y) {
        this.addRenderableWidget(new Button(x - 130, y, 128, 20, new TextComponentTranslation("gui.yes"), null, button -> run(true)));
        this.addRenderableWidget(new Button(x + 2, y, 128, 20, new TextComponentTranslation("gui.no"), null, button -> run(false)));
    }

    @Override
    public void keyTyped(char c, int scanCode) {
        if (scanCode == Keyboard.KEY_ESCAPE) {
            run(false);
        } else {
            super.keyTyped(c, scanCode);
        }
    }

    protected void run(boolean bool) {
        this.callback.apply(bool);
        onGuiClosed();
    }

    public static class FiguraConfirmLinkScreen extends FiguraConfirmScreen {

        private final String url;

        public FiguraConfirmLinkScreen(Function<Boolean, Void> callback, String link, GuiScreen parentScreen) {
            super(callback, new TextComponentTranslation("chat.link.confirmTrusted"), link, parentScreen);
            this.url = link;
        }

        @Override
        protected void addButtons(int x, int y) {
            this.addRenderableWidget(new Button(x - 148, y, 96, 20, new TextComponentTranslation("chat.link.open"), null, button -> run(true)));
            this.addRenderableWidget(new Button(x - 48, y, 96, 20, new TextComponentTranslation("chat.copy"), null, button -> {
                GuiScreen.setClipboardString(this.url);
                FiguraToast.sendToast(new FiguraText("toast.clipboard"));
                run(false);
            }));
            this.addRenderableWidget(new Button(x + 52, y, 96, 20, new TextComponentTranslation("gui.cancel"), null, button -> run(false)));
        }
    }
}
