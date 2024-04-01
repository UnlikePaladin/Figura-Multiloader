package org.figuramc.figura.gui.widgets.permissions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.figuramc.figura.gui.widgets.lists.PlayerList;
import org.figuramc.figura.permissions.PermissionPack;
import org.figuramc.figura.utils.FiguraIdentifier;
import org.figuramc.figura.utils.ui.UIHelper;

public class CategoryPermPackElement extends AbstractPermPackElement {

    private static final ResourceLocation BACKGROUND = new FiguraIdentifier("textures/gui/group_permissions.png");
    private boolean enabled;

    public CategoryPermPackElement(int width, PermissionPack pack, PlayerList parent) {
        super(width, 20, pack, parent);
        this.enabled = pack.isVisible();
    }

    @Override
    public void drawWidget(Minecraft minecraft, int mouseX, int mouseY, float delta) {
        int width = getWidth();
        int height = getHeight();

        GlStateManager.pushMatrix();
        GlStateManager.translate(x + width / 2f, y + height / 2f, 100);
        GlStateManager.scale(scale, scale, 1f);

        animate(delta, this.mouseOver(mouseX, mouseY));

        // fix x, y
        int x = -width / 2;
        int y = -height / 2;

        // selected overlay
        if (this.parent.selectedEntry == this) {
            UIHelper.fillRounded(x - 1, y - 1, width + 2, height + 2, 0xFFFFFFFF);
        }

        // background
        UIHelper.renderHalfTexture(x, y, width, height, 0f, enabled ? 20f : 0f, 174, 20, 174, 40, BACKGROUND);

        // name
        ITextComponent text = pack.getCategoryName().appendText(pack.hasChanges() ? "*" : "");
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        UIHelper.renderOutlineText(font, text, x + width / 2 - font.getStringWidth(text.getFormattedText()) / 2, y + height / 2 - font.FONT_HEIGHT / 2, 0xFFFFFF, 0);

        GlStateManager.popMatrix();
    }

    @Override
    public boolean mouseButtonClicked(int mouseX, int mouseY, int button) {
        return this.mouseOver(mouseX, mouseY) && super.mouseButtonClicked(mouseX, mouseY, button);
    }

    @Override
    public void widgetPressed(int mouseX, int mouseY) {
        if (parent.selectedEntry == this) {
            enabled = !enabled;
            pack.setVisible(enabled);

            parent.updateScroll();
        }

        super.widgetPressed(mouseX, mouseY);
    }

    @Override
    public boolean isVisible() {
        return true;
    }
}
