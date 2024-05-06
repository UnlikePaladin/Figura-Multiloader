package org.figuramc.figura.mixin.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.Badges;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.lua.api.nameplate.NameplateCustomization;
import org.figuramc.figura.permissions.Permissions;
import org.figuramc.figura.utils.EntityUtils;
import org.figuramc.figura.utils.TextUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;
import java.util.regex.Pattern;

@Mixin(GuiPlayerTabOverlay.class)
public class PlayerTabOverlayMixin {

    @Shadow @Final private Minecraft mc;
    @Unique private UUID uuid;

    @Inject(at = @At("RETURN"), method = "getPlayerName", cancellable = true)
    private void getPlayerName(NetworkPlayerInfo playerInfo, CallbackInfoReturnable<String> cir) {
        // get config
        int config = Configs.LIST_NAMEPLATE.value;
        if (config == 0 || AvatarManager.panic)
            return;

        // apply customization
        ITextComponent text = new TextComponentString(cir.getReturnValue());
        ITextComponent name = new TextComponentString(playerInfo.getGameProfile().getName());

        UUID uuid = playerInfo.getGameProfile().getId();
        Avatar avatar = AvatarManager.getAvatarForPlayer(uuid);
        NameplateCustomization custom = avatar == null || avatar.luaRuntime == null ? null : avatar.luaRuntime.nameplate.LIST;

        ITextComponent replacement = custom != null && custom.getJson() != null && avatar.permissions.get(Permissions.NAMEPLATE_EDIT) == 1 ?
                TextUtils.replaceInText(custom.getJson().createCopy(), "\n|\\\\n", " ") : name;

        // name
        replacement = TextUtils.replaceInText(replacement, "\\$\\{name\\}", name);

        // badges
        replacement = Badges.appendBadges(replacement, uuid, config > 1);

        // trim
        replacement = TextUtils.trim(replacement);

        text = TextUtils.replaceInText(text, "\\b" + Pattern.quote(playerInfo.getGameProfile().getName()) + "\\b", replacement);

        cir.setReturnValue(text.getUnformattedText());
    }

    @ModifyArg(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/WorldClient;getPlayerEntityByUUID(Ljava/util/UUID;)Lnet/minecraft/entity/player/EntityPlayer;"), method = "renderPlayerlist")
    private UUID getPlayerByUUID(UUID id) {
        uuid = id;
        return id;
    }


    @ModifyArg(method = "renderPlayerlist", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;drawScaledCustomSizeModalRect(IIFFIIIIFF)V"), index = 0)
    private int doNotDrawFace(int i, int j, float f, float g, int k, int l, int m, int n, float h, float tileHeight) {
        if (uuid != null) {
            Avatar avatar = AvatarManager.getAvatarForPlayer(uuid);

            EntityPlayer player = this.mc.world.getPlayerEntityByUUID(uuid);
            boolean upsideDown = player != null && EntityUtils.isEntityUpsideDown(player);

            if (avatar != null && avatar.renderPortrait(i, j, m, 16, upsideDown))
                return 0;
        }
        return i;
    }
}
