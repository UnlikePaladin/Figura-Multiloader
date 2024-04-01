package org.figuramc.figura.gui.widgets;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.local.LocalAvatarLoader;
import org.figuramc.figura.ducks.extensions.StyleExtension;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.ui.UIHelper;

public class LoadingErrorWidget extends StatusWidget {

    private static final ITextComponent ICON = new TextComponentString("=").setStyle(((StyleExtension)new Style()).setFont(UIHelper.UI_FONT).setColor(TextFormatting.WHITE));

    private String string;

    public LoadingErrorWidget(int x, int y, int width) {
        super(x, y, width, 1);
    }

    @Override
    public void tick() {
        string = LocalAvatarLoader.getLoadError();
        this.setVisible(!AvatarManager.localUploaded && string != null);
    }

    @Override
    public ITextComponent getStatusIcon(int type) {
        return ICON;
    }

    @Override
    public ITextComponent getTooltipFor(int i) {
        return string == null ? new TextComponentString("") : new FiguraText("gui.load_error").setStyle(new Style().setColor(TextFormatting.RED))
                .appendText("\n\n")
                .appendSibling(new FiguraText("gui.status.reason"))
                .appendText("\n• ")
                .appendSibling(new FiguraText("gui.load_error." + LocalAvatarLoader.getLoadState()))
                .appendText("\n• ")
                .appendSibling(new TextComponentString(string));
    }
}
