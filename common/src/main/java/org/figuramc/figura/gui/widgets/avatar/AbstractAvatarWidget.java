package org.figuramc.figura.gui.widgets.avatar;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.local.LocalAvatarFetcher;
import org.figuramc.figura.ducks.extensions.StyleExtension;
import org.figuramc.figura.gui.FiguraToast;
import org.figuramc.figura.gui.widgets.AbstractContainerElement;
import org.figuramc.figura.gui.widgets.Button;
import org.figuramc.figura.gui.widgets.ContextMenu;
import org.figuramc.figura.gui.widgets.lists.AvatarList;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.PlatformUtils;
import org.figuramc.figura.utils.ui.UIHelper;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public abstract class AbstractAvatarWidget extends AbstractContainerElement implements Comparable<AbstractAvatarWidget> {

    protected static final int SPACING = 6;
    protected static final ITextComponent FAVOURITE = new TextComponentString("★").setStyle(((StyleExtension)new Style()).setFont(UIHelper.UI_FONT));
    protected static final ITextComponent ADD_FAVOURITE = new FiguraText("gui.context.favorite.add");
    protected static final ITextComponent REMOVE_FAVOURITE = new FiguraText("gui.context.favorite.remove");

    protected final AvatarList parent;
    protected final int depth;
    protected final ContextMenu context;

    protected LocalAvatarFetcher.AvatarPath avatar;
    protected Button button;
    protected String filter = "";
    protected boolean favourite;

    public AbstractAvatarWidget(int depth, int width, int height, LocalAvatarFetcher.AvatarPath avatar, AvatarList parent) {
        super(0, 0, width, height);
        this.parent = parent;
        this.avatar = avatar;
        this.depth = depth;
        this.context = new ContextMenu(this);
        this.favourite = avatar.isFavourite();
        int id = 0;
        context.addAction(favourite ? REMOVE_FAVOURITE : ADD_FAVOURITE, null, button -> {
            favourite = !favourite;
            avatar.setFavourite(favourite);
            button.setMessage(favourite ? REMOVE_FAVOURITE : ADD_FAVOURITE);
            context.updateDimensions();
        });
        context.addAction(new FiguraText("gui.context.open_folder"), null, button -> {
            try {
                PlatformUtils.openWebLink(avatar.getFSPath().toUri());
            } catch (Exception e) {
                FiguraMod.debug("failed to open avatar folder: ", e.getMessage());
                PlatformUtils.openWebLink(LocalAvatarFetcher.getLocalAvatarDirectory().toUri());
            }
        });
        context.addAction(new FiguraText("gui.context.copy_path"), null, button -> {
            GuiScreen.setClipboardString(avatar.getFSPath().toString());
            FiguraToast.sendToast(new FiguraText("toast.clipboard"));
        });
    }

    @Override
    public void draw(Minecraft minecraft, int mouseX, int mouseY, float delta) {
        if (!isVisible() || !this.button.isVisible())
            return;

        super.draw(minecraft, mouseX, mouseY, delta);

        if (favourite) {
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            int width = font.getStringWidth(FAVOURITE.getFormattedText());
            int x = this.getX() + this.getWidth() - width;
            int y = this.getY() + 2;

            font.drawString(FAVOURITE.getFormattedText(), x, y, 0xFFFFFF);

            if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + font.FONT_HEIGHT)
                UIHelper.setTooltip(new FiguraText("gui.favorited").appendText(" ").appendSibling(FAVOURITE));
        }
    }

    @Override
    public boolean mouseButtonClicked(int mouseX, int mouseY, int button) {
        if (!this.mouseOver(mouseX, mouseY))
            return false;

        if (super.mouseButtonClicked(mouseX, mouseY, button))
            return true;

        // context menu on right click
        if (button == 1) {
            context.setX((int) mouseX);
            context.setY((int) mouseY);
            context.setVisible(true);
            UIHelper.setContext(context);
            return true;
        }
        // hide old context menu
        else if (UIHelper.getContext() == context) {
            context.setVisible(false);
        }

        return false;
    }

    @Override
    public boolean mouseOver(double mouseX, double mouseY) {
        return this.parent.isInsideScissors(mouseX, mouseY) && super.mouseOver(mouseX, mouseY);
    }

    public void update(LocalAvatarFetcher.AvatarPath path, String filter) {
        this.avatar = path;
        this.filter = filter.toLowerCase();
    }

    public ITextComponent getName() {
        return new TextComponentString(avatar.getName());
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        this.button.setX(x);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        this.button.setY(y);
    }

    public boolean filtered() {
        return this.getName().getFormattedText().toLowerCase().contains(filter.toLowerCase());
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible && filtered());
    }

    @Override
    public int compareTo(@NotNull AbstractAvatarWidget other) {
        // compare favourite
        if (this.favourite && !other.favourite)
            return -1;
        else if (other.favourite && !this.favourite)
            return 1;

        // compare types
        if (this instanceof AvatarFolderWidget && other instanceof AvatarWidget)
            return -1;
        else if (this instanceof AvatarWidget && other instanceof AvatarFolderWidget)
            return 1;

        // then compare names
        else return this.getName().getFormattedText().toLowerCase().compareTo(other.getName().getFormattedText().toLowerCase());
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof AbstractAvatarWidget && ((AbstractAvatarWidget) obj).avatar != null && this.avatar != null && this.avatar.getPath().equals(((AbstractAvatarWidget) obj).avatar.getPath());
    }

    public boolean isOf(Path path) {
        return this.avatar != null && this.avatar.getTheActualPathForThis().equals(path);
    }
}
