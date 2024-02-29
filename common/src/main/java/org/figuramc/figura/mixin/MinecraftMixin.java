package org.figuramc.figura.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MouseHelper;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.backend2.NetworkStuff;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.gui.ActionWheel;
import org.figuramc.figura.gui.FiguraToast;
import org.figuramc.figura.gui.PopupMenu;
import org.figuramc.figura.gui.screens.WardrobeScreen;
import org.figuramc.figura.lua.FiguraLuaPrinter;
import org.figuramc.figura.utils.FiguraText;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow
    public MouseHelper mouseHelper;
    @Shadow
    public GameSettings gameSettings;
    @Shadow public EntityPlayerSP player;
    @Shadow
    private Entity renderViewEntity;

    @Shadow public abstract void displayGuiScreen(@Nullable GuiScreen screen);

    @Unique
    private boolean scriptMouseUnlock = false;

    @Inject(at = @At("RETURN"), method = "processKeyBinds")
    private void handleKeybinds(CallbackInfo ci) {
        // don't handle keybinds on panic
        if (AvatarManager.panic)
            return;

        // reload avatar button
        if (Configs.RELOAD_BUTTON.keyBind.isPressed()) {
            AvatarManager.reloadAvatar(FiguraMod.getLocalPlayerUUID());
            FiguraToast.sendToast(new FiguraText("toast.reload"));
        }

        // reload avatar button
        if (Configs.WARDROBE_BUTTON.keyBind.isPressed())
            this.displayGuiScreen(new WardrobeScreen(null));

        // action wheel button
        Boolean wheel = null;
        if (Configs.ACTION_WHEEL_MODE.value % 2 == 1) {
            if (Configs.ACTION_WHEEL_BUTTON.keyBind.isPressed())
                wheel = !ActionWheel.isEnabled();
        } else if (Configs.ACTION_WHEEL_BUTTON.keyBind.isKeyDown()) {
            wheel = true;
        } else if (ActionWheel.isEnabled()) {
            wheel = false;
        }
        if (wheel != null) {
            if (wheel) {
                ActionWheel.setEnabled(true);
                this.mouseHelper.ungrabMouseCursor();
            } else {
                if (Configs.ACTION_WHEEL_MODE.value >= 2)
                    ActionWheel.execute(ActionWheel.getSelected(), true);
                ActionWheel.setEnabled(false);
                this.mouseHelper.grabMouseCursor();
            }
        }

        // popup menu button
        if (Configs.POPUP_BUTTON.keyBind.isKeyDown()) {
            PopupMenu.setEnabled(true);

            if (!PopupMenu.hasEntity()) {
                Entity target = FiguraMod.extendedPickEntity;
                if (this.player != null && target instanceof EntityPlayer && !target.isInvisibleToPlayer(this.player)) {
                    PopupMenu.setEntity(target);
                } else if (this.gameSettings.thirdPersonView != 0) {
                    PopupMenu.setEntity(this.renderViewEntity);
                }
            }
        } else if (PopupMenu.isEnabled()) {
            PopupMenu.run();
        }

        // unlock cursor :p
        Avatar avatar = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());
        if (avatar != null && avatar.luaRuntime != null && avatar.luaRuntime.host.unlockCursor) {
            this.mouseHelper.ungrabMouseCursor();
            scriptMouseUnlock = true;
        } else if (scriptMouseUnlock) {
            this.mouseHelper.grabMouseCursor();
            scriptMouseUnlock = false;
        }
    }

    @ModifyVariable(at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/InventoryPlayer;currentItem:I"), method = "processKeyBinds")
    private int handleHotbarSlots(int value) {
        if (PopupMenu.isEnabled())
            PopupMenu.hotbarKeyPressed(value);
        if (ActionWheel.isEnabled())
            ActionWheel.hotbarKeyPressed(value);
        return value;
    }

    @Inject(at = @At("HEAD"), method = "displayGuiScreen")
    private void setScreen(GuiScreen screen, CallbackInfo ci) {
        if (ActionWheel.isEnabled())
            ActionWheel.setEnabled(false);

        if (PopupMenu.isEnabled())
            PopupMenu.run();
    }

    @Inject(at = @At("RETURN"), method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V")
    private void clearLevel(WorldClient worldClient, String loadingMessage, CallbackInfo ci) {
        if (worldClient == null) {
            AvatarManager.clearAllAvatars();
            FiguraLuaPrinter.clearPrintQueue();
            NetworkStuff.unsubscribeAll();
        }
    }

    @Inject(at = @At("RETURN"), method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V")
    private void setLevel(WorldClient worldClient, String loadingMessage, CallbackInfo ci) {
        if (worldClient != null) {
            NetworkStuff.auth();
        }
    }

    @Inject(at = @At("HEAD"), method = "runGameLoop")
    private void preTick(CallbackInfo ci) {
        AvatarManager.executeAll("applyBBAnimations", Avatar::applyAnimations);
    }

    @Inject(at = @At("RETURN"), method = "runGameLoop")
    private void afterTick(CallbackInfo ci) {
        AvatarManager.executeAll("clearBBAnimations", Avatar::clearAnimations);
    }

    @Inject(at = @At("HEAD"), method = "runTick")
    private void startTick(CallbackInfo ci) {
        FiguraMod.pushProfiler(FiguraMod.MOD_ID);
        FiguraMod.tick();
        FiguraMod.popProfiler();
    }
}
