package org.figuramc.figura.mixin.render.renderers;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.EnumFacing;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.SkullBlockRendererAccessor;
import org.figuramc.figura.lua.api.entity.EntityAPI;
import org.figuramc.figura.lua.api.world.BlockStateAPI;
import org.figuramc.figura.lua.api.world.ItemStackAPI;
import org.figuramc.figura.model.rendering.texture.RenderTypes;
import org.figuramc.figura.permissions.Permissions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntitySkullRenderer.class)
public abstract class TileEntitySkullRendererMixin extends TileEntitySpecialRenderer<TileEntitySkull> {

    @Unique
    private static Avatar avatar;
    @Unique
    private static TileEntitySkull block;


    @Inject(at = @At("HEAD"), method = "renderSkull", cancellable = true)
    private static void figura$renderSkull(float posX, float posY, float posZ, EnumFacing direction, float yaw, int j, GameProfile gameProfile, int skullType, float animateTicks, CallbackInfo ci) {
       // Replicate vanilla skull transforms
        GlStateManager.pushMatrix();
        
        if (direction == EnumFacing.UP) {
            GlStateManager.translate(posX + 0.5F, posY, posZ + 0.5F);
        } else {
            switch (direction) {
                case NORTH:
                    GlStateManager.translate(posX + 0.5F, posY + 0.25F, posZ + 0.74F);
                    break;
                case SOUTH:
                    GlStateManager.translate(posX + 0.5F, posY + 0.25F, posZ + 0.26F);
                    yaw = 180.0F;
                    break;
                case WEST:
                    GlStateManager.translate(posX + 0.74F, posY + 0.25F, posZ + 0.5F);
                    yaw = 270.0F;
                    break;
                case EAST:
                default:
                    GlStateManager.translate(posX + 0.26F, posY + 0.25F, posZ + 0.5F);
                    yaw = 90.0F;
            }
        }
        
        // parse block and items first, so we can yeet them in case of a missed event
        TileEntitySkull localBlock = block;
        block = null;

        ItemStack localItem = SkullBlockRendererAccessor.getItem();
        SkullBlockRendererAccessor.setItem(null);

        Entity localEntity = SkullBlockRendererAccessor.getEntity();
        SkullBlockRendererAccessor.setEntity(null);

        SkullBlockRendererAccessor.SkullRenderMode localMode = SkullBlockRendererAccessor.getRenderMode();
        SkullBlockRendererAccessor.setRenderMode(SkullBlockRendererAccessor.SkullRenderMode.OTHER);

        // avatar pointer incase avatar variable is set during render. (unlikely)
        Avatar localAvatar = avatar;
        avatar = null;

        if (localAvatar == null || localAvatar.permissions.get(Permissions.CUSTOM_SKULL) == 0)
            return;

        FiguraMod.pushProfiler(FiguraMod.MOD_ID);
        FiguraMod.pushProfiler(localAvatar);
        FiguraMod.pushProfiler("skullRender");

        // event
        BlockStateAPI b = localBlock == null ? null : new BlockStateAPI(localBlock.getWorld().getBlockState(localBlock.getPos()), localBlock.getPos());
        ItemStackAPI i = localItem != null ? ItemStackAPI.verify(localItem) : null;
        EntityAPI<?> e = localEntity != null ? EntityAPI.wrap(localEntity) : null;
        String m = localMode.name();

        FiguraMod.pushProfiler(localBlock != null ? localBlock.getPos().toString() : String.valueOf(i));

        FiguraMod.pushProfiler("event");
        boolean bool = localAvatar.skullRenderEvent(Minecraft.getMinecraft().getRenderPartialTicks(), b, i, e, m);

        int light = block.getWorld().getCombinedLight(block.getPos(), 0);

        // render skull :3
        FiguraMod.popPushProfiler("render");
        if (bool || localAvatar.skullRender(RenderTypes.FiguraBufferSource.INSTANCE, light, direction, yaw))
            ci.cancel();

        FiguraMod.popProfiler(5);
        GlStateManager.popMatrix();
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/tileentity/TileEntitySkullRenderer;renderSkull(FFFLnet/minecraft/util/EnumFacing;FILcom/mojang/authlib/GameProfile;IF)V"), method = "render(Lnet/minecraft/tileentity/TileEntitySkull;DDDFIF)V")
    public void figura$render(TileEntitySkull skullBlockEntity, double d, double e, double f, float g, int destroyStage, float h, CallbackInfo ci) {
        block = skullBlockEntity;
        SkullBlockRendererAccessor.setRenderMode(SkullBlockRendererAccessor.SkullRenderMode.BLOCK);
    }

    @Inject(at = @At("HEAD"), method = "render(Lnet/minecraft/tileentity/TileEntitySkull;DDDFIF)V")
    private static void figura$getRenderType(TileEntitySkull tileEntitySkull, double d, double e, double f, float g, int destroyStage, float h, CallbackInfo ci) {
        GameProfile profile = tileEntitySkull.getPlayerProfile();
        avatar = (profile != null && profile.getId() != null) ? AvatarManager.getAvatarForPlayer(profile.getId()) : null;
    }
}
