package org.figuramc.figura.mixin.render.layers;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.BlockSkull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.EnumFacing;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.SkullBlockRendererAccessor;
import org.figuramc.figura.model.ParentType;
import org.figuramc.figura.utils.NbtType;
import org.figuramc.figura.utils.RenderUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LayerCustomHead.class)
public abstract class LayerCustomHeadMixin implements LayerRenderer<EntityLivingBase> {

    public LayerCustomHeadMixin(ModelRenderer renderer) {
        super();
    }

    @Inject(at = @At("HEAD"), method = "doRenderLayer", cancellable = true)
    private void render(EntityLivingBase livingEntity, float f, float g, float h, float i, float j, float k, float scale, CallbackInfo ci) {
        ItemStack itemStack = livingEntity.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        if (itemStack.getItem() instanceof ItemArmor && ((ItemArmor) itemStack.getItem()).getEquipmentSlot() == EntityEquipmentSlot.HEAD) {
            ItemArmor armorItem = (ItemArmor) itemStack.getItem();
            return;
        }

        Avatar avatar = AvatarManager.getAvatar(livingEntity);
        if (!RenderUtils.vanillaModel(avatar))
            return;

        // script hide
        if (avatar.luaRuntime != null && !avatar.luaRuntime.vanilla_model.HELMET_ITEM.checkVisible()) {
            ci.cancel();
            return;
        }

        // pivot part
        if (itemStack.getItem() instanceof ItemBlock && ((ItemBlock) itemStack.getItem()).getBlock() instanceof BlockSkull) {
            // fetch skull data
            GameProfile gameProfile;
            if (itemStack.hasTagCompound()) {
                NBTTagCompound tag = itemStack.getTagCompound();
                if (tag != null && tag.hasKey("SkullOwner", NbtType.COMPOUND.getValue()))
                    gameProfile = NBTUtil.readGameProfileFromNBT(itemStack.getTagCompound().getCompoundTag("SkullOwner"));
                else {
                    gameProfile = null;
                }
            } else {
                gameProfile = null;
            }

            int type = itemStack.getMetadata();

            // render!!
            if (avatar.pivotPartRender(ParentType.HelmetItemPivot, stack -> {
                GlStateManager.pushMatrix();
                float s = 19f;
                GlStateManager.scale(s, s, s);
                GlStateManager.translate(-0.5d, 0d, -0.5d);

                // set item context
                SkullBlockRendererAccessor.setItem(itemStack);
                SkullBlockRendererAccessor.setEntity(livingEntity);
                SkullBlockRendererAccessor.setRenderMode(SkullBlockRendererAccessor.SkullRenderMode.HEAD);
                // note 1.12 gave it the -.5 transform instead of 0, should have the same effect if we transform first though
                TileEntitySkullRenderer.instance.renderSkull(0.0f, 0.0F, 0.0f, EnumFacing.UP, 180.0F, type, gameProfile, -1, f);
                GlStateManager.popMatrix();
            })) {
                ci.cancel();
            }
        } else if (avatar.pivotPartRender(ParentType.HelmetItemPivot, stack -> {
            float s = 10f;
            GlStateManager.translate(0d, 4d, 0d);
            GlStateManager.scale(s, s, s);
            Minecraft.getMinecraft().getItemRenderer().renderItem(livingEntity, itemStack, ItemCameraTransforms.TransformType.HEAD);
        })) {
            ci.cancel();
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/tileentity/TileEntitySkullRenderer;renderSkull(FFFLnet/minecraft/util/EnumFacing;FILcom/mojang/authlib/GameProfile;IF)V"), method = "doRenderLayer")
    private void renderSkull(EntityLivingBase livingEntity, float f, float g, float h, float i, float j, float k, float scale, CallbackInfo ci) {
        SkullBlockRendererAccessor.setItem(livingEntity.getItemStackFromSlot(EntityEquipmentSlot.HEAD));
        SkullBlockRendererAccessor.setEntity(livingEntity);
        SkullBlockRendererAccessor.setRenderMode(SkullBlockRendererAccessor.SkullRenderMode.HEAD);
    }
}
