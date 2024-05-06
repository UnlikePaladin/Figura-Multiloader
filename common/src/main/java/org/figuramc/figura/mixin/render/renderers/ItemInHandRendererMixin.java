package org.figuramc.figura.mixin.render.renderers;

import net.minecraft.block.BlockSkull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.SkullBlockRendererAccessor;
import org.figuramc.figura.lua.api.vanilla_model.VanillaModelPart;
import org.figuramc.figura.math.matrix.FiguraMat4;
import org.figuramc.figura.model.rendering.EntityRenderMode;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.FloatBuffer;

@Mixin(ItemRenderer.class)
public abstract class ItemInHandRendererMixin {


    @Shadow private ItemStack itemStackMainHand;

    @Shadow @Final private Minecraft mc;

    @Shadow protected abstract void renderArmFirstPerson(float f, float g, EnumHandSide enumHandSide);

    @Unique Avatar avatar;

    @Unique
    private final FloatBuffer modelBuff = BufferUtils.createFloatBuffer(16);

    @Inject(method = "renderItemInFirstPerson(F)V", at = @At("HEAD"))
    private void onRenderHandsWithItems(float tickDelta, CallbackInfo ci) {
        AbstractClientPlayer player = this.mc.player;
        avatar = AvatarManager.getAvatarForPlayer(player.getUniqueID());
        if (avatar == null)
            return;

        FiguraMod.pushProfiler(FiguraMod.MOD_ID);
        FiguraMod.pushProfiler(avatar);
        FiguraMod.pushProfiler("renderEvent");
        avatar.renderMode = EntityRenderMode.FIRST_PERSON;
        GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, modelBuff);
        Matrix4f matrix4f = new Matrix4f();
        matrix4f.load(modelBuff);
        avatar.renderEvent(tickDelta, new FiguraMat4().set(matrix4f));
        FiguraMod.popProfiler(3);
    }

    @Inject(method = "renderItemInFirstPerson(F)V", at = @At("RETURN"))
    private void afterRenderHandsWithItems(float tickDelta, CallbackInfo ci) {
        if (avatar == null)
            return;

        FiguraMod.pushProfiler(FiguraMod.MOD_ID);
        FiguraMod.pushProfiler(avatar);
        FiguraMod.pushProfiler("postRenderEvent");
        GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, modelBuff);
        Matrix4f matrix4f = new Matrix4f();
        matrix4f.load(modelBuff);
        avatar.postRenderEvent(tickDelta, new FiguraMat4().set(matrix4f));
        avatar = null;
        FiguraMod.popProfiler(3);
    }

    @Inject(method = "renderItemInFirstPerson(Lnet/minecraft/client/entity/AbstractClientPlayer;FFLnet/minecraft/util/EnumHand;FLnet/minecraft/item/ItemStack;F)V", at = @At("HEAD"), cancellable = true)
    private void renderArmWithItem(AbstractClientPlayer player, float tickDelta, float pitch, EnumHand hand, float swingProgress, ItemStack item, float equipProgress, CallbackInfo ci) {
        if (avatar == null || avatar.luaRuntime == null)
            return;

        boolean main = hand == EnumHand.MAIN_HAND;
        EnumHandSide arm = main ? player.getPrimaryHand() : player.getPrimaryHand().opposite();
        Boolean armVisible = arm == EnumHandSide.LEFT ? avatar.luaRuntime.renderer.renderLeftArm : avatar.luaRuntime.renderer.renderRightArm;

        boolean willRenderItem = !item.isEmpty();
        boolean willRenderArm = (!willRenderItem && main) || item.getItem() == Items.FILLED_MAP || (!willRenderItem && this.itemStackMainHand.getItem() == Items.FILLED_MAP);

        // hide arm
        if (willRenderArm && !willRenderItem && armVisible != null && !armVisible) {
            ci.cancel();
            return;
        }
        // render arm
        if (!willRenderArm && !player.isInvisible() && armVisible != null && armVisible) {
            GlStateManager.pushMatrix();
            this.renderArmFirstPerson(equipProgress, swingProgress, arm);
            GlStateManager.popMatrix();
        }

        // hide item
        VanillaModelPart part = arm == EnumHandSide.LEFT ? avatar.luaRuntime.vanilla_model.LEFT_ITEM : avatar.luaRuntime.vanilla_model.RIGHT_ITEM;
        if (willRenderItem && !part.checkVisible()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderItemSide", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderItem;renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;Z)V"))
    private void renderItem(EntityLivingBase entity, ItemStack stack, ItemCameraTransforms.TransformType itemDisplayContext, boolean leftHanded, CallbackInfo ci) {
        if (stack.getItem() instanceof ItemBlock && ((ItemBlock) stack.getItem()).getBlock() instanceof BlockSkull) {
            ItemBlock bl = (ItemBlock) stack.getItem();
            SkullBlockRendererAccessor.setEntity(entity);
            switch (itemDisplayContext) {
                case FIRST_PERSON_LEFT_HAND:
                    SkullBlockRendererAccessor.setRenderMode(SkullBlockRendererAccessor.SkullRenderMode.FIRST_PERSON_LEFT_HAND);
                    break;
                case FIRST_PERSON_RIGHT_HAND:
                    SkullBlockRendererAccessor.setRenderMode(SkullBlockRendererAccessor.SkullRenderMode.FIRST_PERSON_RIGHT_HAND);
                    break;
                case THIRD_PERSON_LEFT_HAND:
                    SkullBlockRendererAccessor.setRenderMode(SkullBlockRendererAccessor.SkullRenderMode.THIRD_PERSON_LEFT_HAND);
                    break;
                case THIRD_PERSON_RIGHT_HAND:
                    SkullBlockRendererAccessor.setRenderMode(SkullBlockRendererAccessor.SkullRenderMode.THIRD_PERSON_RIGHT_HAND);
                    break;
                default:
                    if (leftHanded) {
                        SkullBlockRendererAccessor.setRenderMode(SkullBlockRendererAccessor.SkullRenderMode.THIRD_PERSON_LEFT_HAND);
                    } else {
                        SkullBlockRendererAccessor.setRenderMode(SkullBlockRendererAccessor.SkullRenderMode.THIRD_PERSON_RIGHT_HAND);
                    } // should never happen
                    break;
            }
        }
    }
}
