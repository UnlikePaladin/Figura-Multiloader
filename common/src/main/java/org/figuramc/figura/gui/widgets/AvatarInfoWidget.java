package org.figuramc.figura.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.font.Emojis;
import org.figuramc.figura.utils.ColorUtils;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.MathUtils;
import org.figuramc.figura.utils.TextUtils;
import org.figuramc.figura.utils.ui.UIHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AvatarInfoWidget implements FiguraWidget, FiguraTickable, FiguraGuiEventListener {

    private static final ITextComponent UNKNOWN = new TextComponentString("?").setStyle(ColorUtils.Colors.AWESOME_BLUE.style);
    private static final ITextComponent ELLIPSIS = TextUtils.ELLIPSIS.createCopy().setStyle(ColorUtils.Colors.AWESOME_BLUE.style);
    private static final List<ITextComponent> TITLES = Arrays.asList(
            new FiguraText("gui.name").setStyle(new Style().setUnderlined(true)),
            new FiguraText("gui.authors").setStyle(new Style().setUnderlined(true)),
            new FiguraText("gui.size").setStyle(new Style().setUnderlined(true)),
            new FiguraText("gui.complexity").setStyle(new Style().setUnderlined(true))
    );

    private int x, y;
    private int width, height;
    private boolean visible = true;
    private final int maxSize;

    private final FontRenderer font;
    private final List<ITextComponent> values = new ArrayList<ITextComponent>() {{
        for (ITextComponent ignored : TITLES)
            this.add(UNKNOWN);
    }};

    public AvatarInfoWidget(int x, int y, int width, int maxSize) {
        this.x = x;
        this.y = y;
        this.font = Minecraft.getMinecraft().fontRenderer;

        this.width = width;
        this.height = (font.FONT_HEIGHT + 4) * TITLES.size() * 2 + 4; // font + spacing + border
        this.maxSize = maxSize;
    }

    @Override
    public void tick() {
        if (!visible) return;

        Style accent = FiguraMod.getAccentColor();
        ELLIPSIS.setStyle(accent);
        UNKNOWN.setStyle(accent);

        // update values
        Avatar avatar = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());
        if (avatar != null && avatar.nbt != null) {
            values.set(0, avatar.name == null || avatar.name.trim().isEmpty() ? UNKNOWN : Emojis.applyEmojis(new TextComponentString(avatar.name).setStyle(accent))); // name
            values.set(1, avatar.authors == null || avatar.authors.trim().isEmpty() ? UNKNOWN : Emojis.applyEmojis(new TextComponentString(avatar.authors).setStyle(accent))); // authors
            values.set(2, new TextComponentString(MathUtils.asFileSize(avatar.fileSize)).setStyle(accent)); // size
            values.set(3, new TextComponentString(String.valueOf(avatar.complexity.pre)).setStyle(accent)); // complexity
        } else {
            for (int i = 0; i < TITLES.size(); i++) {
                values.set(i, UNKNOWN);
            }
        }
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        // prepare vars
        int x = this.x + width / 2;
        int y = this.y + 4;
        int height = font.FONT_HEIGHT + 4;
        int maxLines = (maxSize - 8) / height;

        // special author stuff
        int authorFreeLines = maxLines - 7;
        ITextComponent authors = values.get(1);
        List<ITextComponent> authorLines = authors == null ? Collections.emptyList() : TextUtils.splitText(authors, "\n");
        int authorUsedLines = Math.min(authorLines.size(), authorFreeLines);

        // set new widget height
        int newHeight = height * TITLES.size() * 2 + 4 + height * (authorUsedLines - 1);
        this.height = Math.min(newHeight + height, maxSize);
        y += (this.height - newHeight) / 2;

        //render background
        UIHelper.renderSliced(this.x, this.y, this.width, this.height, UIHelper.OUTLINE_FILL);

        // render texts
        for (int i = 0; i < TITLES.size(); i++) {
            // -- title -- //

            ITextComponent title = TITLES.get(i);
            if (title != null)
                UIHelper.drawCenteredString(font, title, x, y, 0xFFFFFF);
            y += height;

            // -- value -- //

            ITextComponent value = values.get(i);
            if (value == null) {
                y += height;
                continue;
            }

            // default rendering
            if (i != 1) {
                ITextComponent toRender = TextUtils.trimToWidthEllipsis(font, value, width - 10, ELLIPSIS);
                UIHelper.drawCenteredString(font, toRender, x, y, 0xFFFFFF);

                // tooltip
                if (value != toRender && UIHelper.isMouseOver(this.x, y - height, width, height * 2 - 4, mouseX, mouseY))
                    UIHelper.setTooltip(value);

                y += height;
                continue;
            }

            // author special rendering
            for (int j = 0; j < authorUsedLines; j++) {
                ITextComponent text = authorLines.get(j);
                ITextComponent newText = TextUtils.trimToWidthEllipsis(font, text, width - 10, ELLIPSIS);

                if (j == authorUsedLines - 1 && authorLines.size() > authorUsedLines) {
                    text = value;
                    newText = ELLIPSIS;
                }

                if (text != newText && UIHelper.isMouseOver(this.x, y, width, height, mouseX, mouseY))
                    UIHelper.setTooltip(text);

                UIHelper.drawCenteredString(font, newText, x, y, 0xFFFFFF);
                y += height;
            }
        }
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public void setX(int x) {
        this.x = x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public void setY(int y) {
        this.y = y;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }
}
