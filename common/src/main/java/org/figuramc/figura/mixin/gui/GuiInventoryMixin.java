package org.figuramc.figura.mixin.gui;

import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.EntityLivingBase;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.model.rendering.EntityRenderMode;
import org.figuramc.figura.utils.ui.UIHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiInventory.class, priority = 999)
public class GuiInventoryMixin {

    @Inject(method = "drawEntityOnScreen", at = @At("HEAD"), cancellable = true)
    private static void renderEntityInInventoryFollowsMouse(int x, int y, int size, float mouseX, float mouseY, EntityLivingBase entity, CallbackInfo ci) {
        if (!Configs.FIGURA_INVENTORY.value || AvatarManager.panic)
            return;

        UIHelper.drawEntity(x, y, size, (float) Math.atan(mouseY / 40f) * 20f, (float) -Math.atan(mouseX / 40f) * 20f, entity, EntityRenderMode.MINECRAFT_GUI);
        ci.cancel();
    }
}
