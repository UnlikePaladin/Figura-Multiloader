package org.figuramc.figura.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.Badges;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.ducks.extensions.StyleExtension;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.math.vector.FiguraVec4;
import org.figuramc.figura.permissions.PermissionManager;
import org.figuramc.figura.permissions.PermissionPack;
import org.figuramc.figura.utils.FiguraIdentifier;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.MathUtils;
import org.figuramc.figura.utils.Pair;
import org.figuramc.figura.utils.ui.UIHelper;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class PopupMenu {
//TODO: implement font in style extension correctly
    private static final FiguraIdentifier BACKGROUND = new FiguraIdentifier("textures/gui/popup.png");
    private static final FiguraIdentifier ICONS = new FiguraIdentifier("textures/gui/popup_icons.png");

    private static final ITextComponent VERSION_WARN = new TextComponentString("")
            .appendSibling(Badges.System.WARNING.badge.createCopy().setStyle(((StyleExtension)new Style()).setFont(Badges.FONT)))
            .appendText(" ")
            .appendSibling(Badges.System.WARNING.desc.createCopy().setStyle(new Style().setColor(TextFormatting.YELLOW)));
    private static final ITextComponent ERROR_WARN = new TextComponentString("")
            .appendSibling(Badges.System.ERROR.badge.createCopy().setStyle(((StyleExtension)new Style()).setFont(Badges.FONT)))
            .appendText(" ")
            .appendSibling(Badges.System.ERROR.desc.createCopy().setStyle(new Style().setColor(TextFormatting.RED)));
    private static final ITextComponent PERMISSION_WARN = new TextComponentString("")
            .appendSibling(Badges.System.PERMISSIONS.badge.createCopy().setStyle(((StyleExtension)new Style()).setFont(Badges.FONT)))
            .appendText(" ")
            .appendSibling(Badges.System.PERMISSIONS.desc.createCopy().setStyle(new Style().setColor(TextFormatting.RED)));

    private static final List<Pair<ITextComponent, Consumer<UUID>>> BUTTONS = Arrays.asList(
            Pair.of(new FiguraText("popup_menu.cancel"), id -> {}),
            Pair.of(new FiguraText("popup_menu.reload"), id -> {
                AvatarManager.reloadAvatar(id);
                FiguraToast.sendToast(new FiguraText("toast.reload"));
            }),
            Pair.of(new FiguraText("popup_menu.increase_permissions"), id -> {
                PermissionPack pack = PermissionManager.get(id);
                if (PermissionManager.increaseCategory(pack))
                    FiguraToast.sendToast(new FiguraText("toast.permission_change"), pack.getCategoryName());
            }),
            Pair.of(new FiguraText("popup_menu.decrease_permissions"), id -> {
                PermissionPack pack = PermissionManager.get(id);
                if (PermissionManager.decreaseCategory(pack))
                    FiguraToast.sendToast(new FiguraText("toast.permission_change"), pack.getCategoryName());
            })
    );
    private static final int LENGTH = BUTTONS.size();

    // runtime data
    private static int index = 0;
    private static boolean enabled = false;
    private static Entity entity;
    private static UUID id;

    public static void render() {
        if (!isEnabled()) return;

        if (entity == null) {
            id = null;
            return;
        }

        id = entity.getUniqueID();
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null || (entity.isInvisibleToPlayer(minecraft.player) && entity != minecraft.player)) {
            entity = null;
            id = null;
            return;
        }

        GlStateManager.disableDepth();
        GlStateManager.pushMatrix();

        // world to screen space
        float partialTicks = minecraft.getRenderPartialTicks();
        double d = MathUtils.lerp(partialTicks, entity.prevPosX, entity.posX);
        double e = MathUtils.lerp(partialTicks, entity.prevPosY, entity.posY);
        double f = MathUtils.lerp(partialTicks, entity.prevPosZ, entity.posZ);

        FiguraVec3 worldPos = FiguraVec3.of(d,e,f);
        worldPos.add(0f, (entity.getCollisionBoundingBox().maxY -  entity.getCollisionBoundingBox().minY )+ 0.1f, 0f);

        FiguraVec4 vec = MathUtils.worldToScreenSpace(worldPos);
        if (vec.z < 1) return; // too close

        ScaledResolution window = new ScaledResolution(minecraft);
        double w = window.getScaledWidth_double();
        double h = window.getScaledHeight_double();
        double s = Configs.POPUP_SCALE.value * Math.max(Math.min(minecraft.displayHeight * 0.035 / vec.w * ((double) 1 / window.getScaleFactor()), Configs.POPUP_MAX_SIZE.value), Configs.POPUP_MIN_SIZE.value);

        GlStateManager.translate((vec.x + 1) / 2 * w, (vec.y + 1) / 2 * h, -100);
        GlStateManager.scale((float) (s * 0.5), (float) (s * 0.5), 1);

        // background
        int width = LENGTH * 18;

        UIHelper.setupTexture(BACKGROUND);
        int frame = Configs.REDUCED_MOTION.value ? 0 : (int) ((FiguraMod.ticks / 5f) % 4);
        UIHelper.blit(width / -2, -24, width, 26, 0, frame * 26, width, 26, width, 104);

        // icons
        GlStateManager.translate(0f, 0f, -2f);
        UIHelper.setupTexture(ICONS);
        for (int i = 0; i < LENGTH; i++)
            UIHelper.blit(width / -2 + (18 * i), -24, 18, 18, 18 * i, i == index ? 18 : 0, 18, 18, width, 36);

        // texts
        FontRenderer font = minecraft.fontRenderer;

        ITextComponent title = BUTTONS.get(index).getFirst();

        PermissionPack tc = PermissionManager.get(id);
        ITextComponent permissionName = tc.getCategoryName().appendText(tc.hasChanges() ? "*" : "");

        ITextComponent name = new TextComponentString(entity.getName());

        boolean error = false;
        boolean version = false;
        boolean noPermissions = false;

        ITextComponent badges = Badges.fetchBadges(id);
        if (!badges.getFormattedText().isEmpty())
            name.appendText(" ").appendSibling(badges);

        Avatar avatar = AvatarManager.getAvatarForPlayer(id);
        if (avatar != null) {
            error = avatar.scriptError;
            version = avatar.versionStatus > 0;
            noPermissions = !avatar.noPermissions.isEmpty();
        }

        // render texts
        UIHelper.renderOutlineText(font, name, -font.getStringWidth(name.getFormattedText()) / 2, -36, 0xFFFFFF, 0x202020);

        GlStateManager.scale(0.5f, 0.5f, 0.5f);
        GlStateManager.translate(0f, 0f, -1f);

        UIHelper.renderOutlineText(font, permissionName, -font.getStringWidth(permissionName.getFormattedText()) / 2, -54, 0xFFFFFF, 0x202020);
        font.drawString(title.getFormattedText(), -width + 4, -12, 0xFFFFFF);

        if (error)
            UIHelper.renderOutlineText(font, ERROR_WARN, -font.getStringWidth(ERROR_WARN.getFormattedText()) / 2, 0, 0xFFFFFF, 0x202020);
        if (version)
            UIHelper.renderOutlineText(font, VERSION_WARN, -font.getStringWidth(VERSION_WARN.getFormattedText()) / 2, error ? font.FONT_HEIGHT : 0, 0xFFFFFF, 0x202020);
        if (noPermissions)
            UIHelper.renderOutlineText(font, PERMISSION_WARN, -font.getStringWidth(PERMISSION_WARN.getFormattedText()) / 2, (error ? font.FONT_HEIGHT : 0) + (version ? font.FONT_HEIGHT : 0), 0xFFFFFF, 0x202020);

        // finish rendering
        GlStateManager.popMatrix();
    }


    public static void scroll(double d) {
        index = (int) (index - d + LENGTH) % LENGTH;
    }

    public static void hotbarKeyPressed(int i) {
        if (i < LENGTH && i >= 0)
            index = i;
    }

    public static void run() {
        if (id != null)
            BUTTONS.get(index).getSecond().accept(id);

        enabled = false;
        entity = null;
        id = null;
        index = 0;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        PopupMenu.enabled = enabled;
    }

    public static boolean hasEntity() {
        return entity != null;
    }

    public static void setEntity(Entity entity) {
        PopupMenu.entity = entity;
    }

    public static UUID getEntityId() {
        return id;
    }
}
