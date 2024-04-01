package org.figuramc.figura.gui.screens;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import org.figuramc.figura.ducks.extensions.StyleExtension;
import org.figuramc.figura.gui.FiguraToast;
import org.figuramc.figura.gui.widgets.Button;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.TextUtils;

public class ProfileScreen extends AbstractPanelScreen {

    public ProfileScreen(GuiScreen parentScreen) {
        super(parentScreen, new FiguraText("gui.panels.title.profile"));
    }

    @Override
    public void initGui() {
        super.initGui();

        this.addRenderableWidget(new Button(width / 2 - 30, height / 2 - 30, 60, 20, new TextComponentString("meow"),
                new TextComponentString("test").appendText("\n").appendText("one line").appendText("\n\n").appendText("two lines").appendText("\n").appendText("\n").appendText("two lines").appendText("\n\n\n").appendText("three lines").appendText("\n").appendText("\n").appendText("\n").appendText("three lines").appendText("\n"), button -> {
            FiguraToast.sendToast(new TextComponentString("Backend restarting").setStyle(((StyleExtension)new Style()).setRGBColor(0x99BBEE)), "in 10 minutes!", FiguraToast.ToastType.DEFAULT);
        }));

        this.addRenderableWidget(new Button(width / 2 - 30, height / 2 + 10, 60, 20, new TextComponentString("meow"), TextUtils.tryParseJson(
                "{\"text\": \"△❗\n❌\uD83E\uDDC0\n\n☄❤\n\n\n☆★\",\"font\": \"figura:badges\"}"), button -> {
            FiguraToast.sendToast(new TextComponentString("Backend restarting").setStyle(((StyleExtension)new Style()).setRGBColor((0x99BBEE))), "in 10 minutes!", FiguraToast.ToastType.DEFAULT);
        }));
    }

    @Override
    public void renderOverlays(int mouseX, int mouseY, float delta) {
        // UIHelper.highlight(stack, button, TextUtils.tryParseJson("{\"text\":\"🦐🦐🦐🦐\",\"font\":\"figura:emojis\"}"));
        super.renderOverlays(mouseX, mouseY, delta);
    }
}
