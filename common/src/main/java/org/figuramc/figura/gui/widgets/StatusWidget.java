package org.figuramc.figura.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.backend2.NetworkStuff;
import org.figuramc.figura.ducks.extensions.StyleExtension;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.MathUtils;
import org.figuramc.figura.utils.ui.UIHelper;

import java.util.Arrays;
import java.util.List;

public class StatusWidget implements FiguraWidget, FiguraTickable, FiguraGuiEventListener {

    public static final String STATUS_INDICATORS = "-*/+";
    public static final List<String> STATUS_NAMES = Arrays.asList("size", "texture", "script", "backend");
    public static final List<Style> TEXT_COLORS = Arrays.asList(
            new Style().setColor(TextFormatting.WHITE),
            new Style().setColor(TextFormatting.RED),
            new Style().setColor(TextFormatting.YELLOW),
            new Style().setColor(TextFormatting.GREEN)
    );

    private final FontRenderer font;
    protected final int count;
    protected int status = 0;
    private ITextComponent scriptError, disconnectedReason;

    private int x, y;
    private int width, height;
    private boolean visible = true;
    private boolean background = true;
    private boolean hovered;

    public StatusWidget(int x, int y, int width) {
        this(x, y, width, STATUS_NAMES.size());
    }

    protected StatusWidget(int x, int y, int width, int count) {
        this.x = x;
        this.y = y;
        this.font = Minecraft.getMinecraft().fontRenderer;
        this.width = width;
        this.height = font.FONT_HEIGHT + 5;
        this.count = count;
    }

    @Override
    public void tick() {
        if (!isVisible()) return;

        // update status indicators
        Avatar avatar = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());
        boolean empty = avatar == null || avatar.nbt == null;

        status = empty ? 0 : avatar.fileSize > NetworkStuff.getSizeLimit() ? 1 : avatar.fileSize > NetworkStuff.getSizeLimit() * 0.75 ? 2 : 3;

        int texture = empty || !avatar.hasTexture ? 0 : 3;
        status += texture << 2;

        int script = empty ? 0 : avatar.scriptError ? 1 : avatar.luaRuntime == null ? 0 : avatar.versionStatus > 0 ? 2 : 3;
        status += script << 4;
        scriptError = script == 1 ? avatar.errorText.createCopy() : null;

        int backend = NetworkStuff.backendStatus;
        status += backend << 6;

        String dc = NetworkStuff.disconnectedReason;
        disconnectedReason = backend == 1 && dc != null && !dc.trim().isEmpty() ? new TextComponentString(dc) : null;
    }

    @Override
    public void draw(Minecraft minecraft, int mouseX, int mouseY, float delta) {
        if (!isVisible()) return;

        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        boolean background = hasBackground();

        // background
        if (background)
            UIHelper.renderSliced(x, y, width, height, UIHelper.OUTLINE_FILL);

        // hover
        hovered = UIHelper.isMouseOver(getX(), getY(), getWidth(), getHeight(), mouseX, mouseY);

        // text and tooltip
        double spacing = (double) width / count;
        double hSpacing = spacing * 0.5;
        for (int i = 0; i < count; i++) {
            int xx = (int) (x + spacing * i + hSpacing);

            ITextComponent text = getStatusIcon(i);
            UIHelper.drawString(font, text, xx - font.getStringWidth(text.getFormattedText()) / 2, y + (background ? 3 : 0), 0xFFFFFF);

            if (hovered && mouseX >= xx - hSpacing && mouseX < xx + hSpacing && mouseY >= y && mouseY < y + font.FONT_HEIGHT + (background ? 3 : 0))
                UIHelper.setTooltip(getTooltipFor(i));
        }
    }

    public ITextComponent getStatusIcon(int type) {
        return new TextComponentString(String.valueOf(STATUS_INDICATORS.charAt(status >> (type * 2) & 3))).setStyle(((StyleExtension)new Style()).setFont(UIHelper.UI_FONT));
    }

    public ITextComponent getTooltipFor(int i) {
        // get name and color
        int color = status >> (i * 2) & 3;
        String part = "gui.status." + STATUS_NAMES.get(i);

        ITextComponent info;
        if (i == 0) {
            double size = NetworkStuff.getSizeLimit();
            info = new FiguraText(part + "." + color, MathUtils.asFileSize(size));
        } else {
            info = new FiguraText(part + "." + color);
        }

        ITextComponent text = new FiguraText(part).appendText("\n• ").appendSibling(info).setStyle(TEXT_COLORS.get(color));

        // script error
        if (i == 2 && color == 1 && scriptError != null)
            text.appendText("\n\n").appendSibling(new FiguraText("gui.status.reason")).appendText("\n• ").appendSibling(scriptError);

        // get backend disconnect reason
        if (i == 3 && disconnectedReason != null)
            text.appendText("\n\n").appendSibling(new FiguraText("gui.status.reason")).appendText("\n• ").appendSibling(disconnectedReason);

        return text;
    }

    public boolean hasBackground() {
        return this.background;
    }

    public void setBackground(boolean background) {
        this.background = background;
    }

    @Override
    public boolean isVisible() {
        return this.visible;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public void setX(int x) {
        this.x = x;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public void setY(int y) {
        this.y = y;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public int getHeight() {
        return height;
    }
}
