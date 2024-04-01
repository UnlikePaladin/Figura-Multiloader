package org.figuramc.figura.gui.widgets.permissions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.Badges;
import org.figuramc.figura.gui.FiguraToast;
import org.figuramc.figura.gui.widgets.ContextMenu;
import org.figuramc.figura.gui.widgets.Label;
import org.figuramc.figura.gui.widgets.lists.PlayerList;
import org.figuramc.figura.lua.api.nameplate.NameplateCustomization;
import org.figuramc.figura.permissions.PermissionManager;
import org.figuramc.figura.permissions.PermissionPack;
import org.figuramc.figura.permissions.Permissions;
import org.figuramc.figura.utils.EntityUtils;
import org.figuramc.figura.utils.FiguraIdentifier;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.TextUtils;
import org.figuramc.figura.utils.ui.UIHelper;

import java.util.ArrayList;
import java.util.UUID;

public class PlayerPermPackElement extends AbstractPermPackElement {

    public static final ResourceLocation UNKNOWN = new FiguraIdentifier("textures/gui/unknown_portrait.png");
    private static final ResourceLocation BACKGROUND = new FiguraIdentifier("textures/gui/player_permissions.png");
    private static final ITextComponent DC_TEXT = new FiguraText("gui.permissions.disconnected").setStyle(new Style().setColor(TextFormatting.RED));

    private final String name;
    private final ResourceLocation skin;
    private final UUID owner;
    private final ContextMenu context;
    private final Label nameLabel;
    private final PlayerStatusWidget status;

    public boolean disconnected = false;

    // drag
    public boolean dragged = false;
    public int anchorX, anchorY, initialY;
    public int index;

    public PlayerPermPackElement(int width, String name, PermissionPack pack, ResourceLocation skin, UUID owner, PlayerList parent) {
        super(width, 40, pack, parent);
        this.name = name;
        this.skin = skin;
        this.owner = owner;
        this.context = new ContextMenu(this);

        this.nameLabel = new Label(name, 0, 0, 0);
        this.status = new PlayerStatusWidget(0, 0, 70, owner);

        generateContext();
    }

    private void generateContext() {
        // name uuid
        context.addAction(new FiguraText("gui.context.copy_name"), null, button -> {
            GuiScreen.setClipboardString(this.getName());
            FiguraToast.sendToast(new FiguraText("toast.clipboard"));
        });
        context.addAction(new FiguraText("gui.context.copy_uuid"), null, button -> {
            GuiScreen.setClipboardString(this.getOwner().toString());
            FiguraToast.sendToast(new FiguraText("toast.clipboard"));
        });

        // reload
        context.addAction(new FiguraText("gui.context.reload"), null, button -> {
            AvatarManager.reloadAvatar(owner);
            FiguraToast.sendToast(new FiguraText("toast.reload"));
        });

        // permissions
        ContextMenu permissionsContext = new ContextMenu();
        for (Permissions.Category category : Permissions.Category.values()) {
            PermissionPack.CategoryPermissionPack categoryPack = PermissionManager.CATEGORIES.get(category);
            permissionsContext.addAction(categoryPack.getCategoryName(), null, button -> {
                pack.setCategory(categoryPack);
                if (parent.selectedEntry == this)
                    parent.parent.updatePermissions(pack);
            });
        }
        context.addTab(new FiguraText("gui.context.set_permissions"), null, permissionsContext);
    }

    @Override
    public void draw(Minecraft minecraft, int mouseX, int mouseY, float delta) {
        if (dragged)
            UIHelper.fillRounded(getX() - 1, getY() - 1, getWidth() + 2, getHeight() + 2, 0x40FFFFFF);
        else
            super.draw(minecraft, mouseX, mouseY, delta);
    }

    public void renderDragged(Minecraft mc, int mouseX, int mouseY, float delta) {
        int oX = x;
        int oY = y;
        x = mouseX - (anchorX - x);
        y = mouseY - (anchorY - y) + (initialY - oY);
        super.draw(mc, mouseX, mouseY, delta);
        x = oX;
        y = oY;
    }

    @Override
    public void drawWidget(Minecraft minecraft, int mouseX, int mouseY, float delta) {
        int width = getWidth();
        int height = getHeight();

        GlStateManager.pushMatrix();

        float tx = x + width / 2f;
        float ty = y + height / 2f;

        GlStateManager.translate(tx, ty, 100);
        GlStateManager.scale(scale, scale, 1f);

        animate(delta, (UIHelper.getContext() == this.context && this.context.isVisible()) || this.mouseOver(mouseX, mouseY));

        // fix x, y, mouse
        int x = -width / 2;
        int y = -height / 2;
        mouseX = (int) ((mouseX - tx) / scale);
        mouseY = (int) ((mouseY - ty) / scale);

        // selected overlay
        if (this.parent.selectedEntry == this) {
            ArrayList<PermissionPack> list = new ArrayList<>(PermissionManager.CATEGORIES.values());
            int color = (dragged ? list.get(Math.min(index, list.size() - 1)) : pack).getColor();
            UIHelper.fillRounded(x - 1, y - 1, width + 2, height + 2, color + (0xFF << 24));
        }

        // background
        UIHelper.renderHalfTexture(x, y, width, height, 174, BACKGROUND);

        // head
        ITextComponent name = null;
        boolean head = false;

        Avatar avatar = AvatarManager.getAvatarForPlayer(owner);
        if (avatar != null) {
            NameplateCustomization custom = avatar.luaRuntime == null ? null : avatar.luaRuntime.nameplate.LIST;
            if (custom != null && custom.getJson() != null && avatar.permissions.get(Permissions.NAMEPLATE_EDIT) == 1)
                name = custom.getJson().createCopy();

            Entity e = EntityUtils.getEntityByUUID(owner);
            boolean upsideDown = e instanceof EntityLivingBase && EntityUtils.isEntityUpsideDown((EntityLivingBase) e);
            head = avatar.renderPortrait(x + 4, y + 4, Math.round(32f * scale), 64, upsideDown);
        }

        if (!head) {
            if (this.skin != null) {
                // head

                UIHelper.setupTexture(this.skin);
                UIHelper.blit(x + 4, y + 4, 32, 32, 8f, 8f, 8, 8, 64, 64);

                // hat
                GlStateManager.enableBlend();
                UIHelper.blit(x + 4, y + 4, 32, 32, 40f, 8f, 8, 8, 64, 64);
                GlStateManager.disableBlend();
            } else {
                UIHelper.renderTexture(x + 4, y + 4, 32, 32, UNKNOWN);
            }
        }

        // name
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        ITextComponent ogName = new TextComponentString(this.name);

        if (name == null)
            name = ogName;

        name = TextUtils.replaceInText(name, "\\$\\{name\\}", ogName);
        name = TextUtils.splitText(name, "\n").get(0);
        name = new TextComponentString("").appendSibling(name.createCopy().setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(this.name + "\n" + this.owner)))));

        // badges
        name = Badges.appendBadges(name, owner, false);
        ITextComponent badges = Badges.fetchBadges(owner);
        if (!badges.getUnformattedText().isEmpty())
            badges = new TextComponentString(" ").appendSibling(badges);

        nameLabel.setText(TextUtils.trimToWidthEllipsis(font, name, width - 44 - font.getStringWidth(badges.getUnformattedText()), TextUtils.ELLIPSIS).createCopy().appendSibling(badges));
        nameLabel.setX(x + 40);
        nameLabel.setY(y + 4);
        // nameLabel.setOutlineColor(ColorUtils.rgbToInt(ColorUtils.rainbow(2, 1, 0.5)) + ((int) (0.5f * 0xFF) << 24));
        nameLabel.draw(minecraft, mouseX, mouseY, delta);

        // status
        if (avatar != null && avatar.nbt != null) {
            status.tick(); // yes I know
            status.setX(x + 40);
            status.setY(y + 6 + font.FONT_HEIGHT);
            status.draw(minecraft, mouseX, mouseY, delta);
        }

        // category
        int textY = y + height - font.FONT_HEIGHT - 4;
        drawString(font, pack.getCategoryName().appendText(pack.hasChanges() ? "*" : "").getFormattedText(), x + 40, textY, 0xFFFFFF);

        // disconnected
        if (disconnected)
            drawString(font, DC_TEXT.getFormattedText(), x + width - font.getStringWidth(DC_TEXT.getFormattedText()) - 4, textY, 0xFFFFFF);

        GlStateManager.popMatrix();
    }

    @Override
    public boolean mouseButtonClicked(int mouseX, int mouseY, int button) {
        if (!this.mouseOver(mouseX, mouseY))
            return false;

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

        return super.mouseButtonClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseOver(double mouseX, double mouseY) {
        return !dragged && super.mouseOver(mouseX, mouseY);
    }

    public String getName() {
        return name;
    }

    public UUID getOwner() {
        return owner;
    }

    @Override
    public boolean isVisible() {
        return super.isVisible() && this.pack.isVisible();
    }
}
