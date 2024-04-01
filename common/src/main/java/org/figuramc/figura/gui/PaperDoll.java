package org.figuramc.figura.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.model.rendering.EntityRenderMode;
import org.figuramc.figura.utils.ui.UIHelper;

public class PaperDoll {

    private static Long lastActivityTime = 0L;

    public static void render(boolean force) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityLivingBase entity = minecraft.getRenderViewEntity() instanceof EntityLivingBase ? (EntityLivingBase) minecraft.getRenderViewEntity() : null;
        Avatar avatar;

        if ((!Configs.HAS_PAPERDOLL.value && !force) ||
                entity == null ||
                Minecraft.getMinecraft().gameSettings.hideGUI ||
                minecraft.gameSettings.showDebugInfo ||
                (Configs.FIRST_PERSON_PAPERDOLL.value && minecraft.gameSettings.thirdPersonView != 0 && !force) ||
                entity.isPlayerSleeping())
            return;

        // check if it should stay always on
        if (!Configs.PAPERDOLL_ALWAYS_ON.value && !force && (avatar = AvatarManager.getAvatar(entity)) != null && avatar.luaRuntime != null && !avatar.luaRuntime.renderer.forcePaperdoll) {
            // if action - reset activity time and enable can draw
            if (entity.isSprinting() ||
                    entity.isSneaking() ||
                    entity.isElytraFlying() ||
                    entity.isActiveItemStackBlocking() ||
                    entity.isOnLadder() ||
                    (entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.isFlying))
                lastActivityTime = System.currentTimeMillis();

                // if activity time is greater than duration - return
            else if (System.currentTimeMillis() - lastActivityTime > 1000L)
                return;
        }

        // draw
        float screenWidth = minecraft.displayWidth;
        float screenHeight = minecraft.displayHeight;
        float guiScale = (float) new ScaledResolution(minecraft).getScaleFactor();

        float scale = Configs.PAPERDOLL_SCALE.tempValue;
        float x = scale * 25f;
        float y = scale * 45f;
        x += (Configs.PAPERDOLL_X.tempValue / 100f) * screenWidth / guiScale;
        y += (Configs.PAPERDOLL_Y.tempValue / 100f) * screenHeight / guiScale;

        UIHelper.drawEntity(
                x, y,
                scale * 30f,
                Configs.PAPERDOLL_PITCH.tempValue, Configs.PAPERDOLL_YAW.tempValue,
                entity, EntityRenderMode.PAPERDOLL
        );
    }
}
