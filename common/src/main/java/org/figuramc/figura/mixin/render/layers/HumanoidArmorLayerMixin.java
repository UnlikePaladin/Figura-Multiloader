package org.figuramc.figura.mixin.render.layers;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.compat.GeckoLibCompat;
import org.figuramc.figura.lua.api.vanilla_model.VanillaPart;
import org.figuramc.figura.model.ParentType;
import org.figuramc.figura.model.rendering.texture.RenderTypes;
import org.figuramc.figura.permissions.Permissions;
import org.figuramc.figura.utils.FiguraArmorPartRenderer;
import org.figuramc.figura.utils.PlatformUtils;
import org.figuramc.figura.utils.RenderUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = LayerArmorBase.class, priority = 900)
public abstract class HumanoidArmorLayerMixin<T extends ModelBase> implements LayerRenderer<EntityLivingBase>, HumanoidArmorLayerAccessor {

    @Shadow
    public abstract T getModelFromSlot(EntityEquipmentSlot slot);

    @Shadow protected abstract void renderArmorLayer(EntityLivingBase entityLivingBase, float f, float g, float h, float i, float j, float k, float l, EntityEquipmentSlot slotIn);

    @Shadow protected T modelArmor;

    @Shadow protected abstract ResourceLocation getArmorResource(ItemArmor armor, boolean bl);

    @Shadow private float colorR;
    @Shadow private float colorG;
    @Shadow private float colorB;
    @Shadow private float alpha;
    @Shadow @Final private RenderLivingBase<?> renderer;

    @Shadow protected abstract ResourceLocation getArmorResource(ItemArmor armor, boolean bl, String string);
    @Shadow @Final protected static ResourceLocation ENCHANTED_ITEM_GLINT_RES;
    @Unique
    private boolean figura$renderingVanillaArmor;

    @Unique
    private Avatar figura$avatar;

    @Inject(at = @At(value = "HEAD"), method = "doRenderLayer") 
    public void setAvatar(EntityLivingBase livingEntity, float limbSwing, float limbSwingAmount, float tickDelta, float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
        figura$avatar = AvatarManager.getAvatar(livingEntity);
    }

    @Inject(at = @At(value = "INVOKE", shift = At.Shift.AFTER, ordinal = 3, target = "Lnet/minecraft/client/renderer/entity/layers/LayerArmorBase;renderArmorLayer(Lnet/minecraft/entity/EntityLivingBase;FFFFFFFLnet/minecraft/inventory/EntityEquipmentSlot;)V"), method = "doRenderLayer")
    public void onRenderEnd(EntityLivingBase livingEntity, float limbSwing, float limbSwingAmount, float tickDelta, float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
        if (figura$avatar == null) return;

        figura$tryRenderArmorPart(EntityEquipmentSlot.HEAD, limbSwing, limbSwingAmount, tickDelta, ageInTicks, netHeadYaw, headPitch, scale, this::figura$helmetRenderer, livingEntity, ParentType.HelmetPivot);
        figura$tryRenderArmorPart(EntityEquipmentSlot.CHEST, limbSwing, limbSwingAmount, tickDelta, ageInTicks, netHeadYaw, headPitch, scale, this::figura$chestplateRenderer, livingEntity, ParentType.LeftShoulderPivot, ParentType.ChestplatePivot, ParentType.RightShoulderPivot);
        figura$tryRenderArmorPart(EntityEquipmentSlot.LEGS,  limbSwing, limbSwingAmount, tickDelta, ageInTicks, netHeadYaw, headPitch, scale, this::figura$leggingsRenderer, livingEntity, ParentType.LeftLeggingPivot, ParentType.RightLeggingPivot, ParentType.LeggingsPivot);
        figura$tryRenderArmorPart(EntityEquipmentSlot.FEET,  limbSwing, limbSwingAmount, tickDelta, ageInTicks, netHeadYaw, headPitch, scale, this::figura$bootsRenderer, livingEntity, ParentType.LeftBootPivot, ParentType.RightBootPivot);
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/LayerArmorBase;isLegSlot(Lnet/minecraft/inventory/EntityEquipmentSlot;)Z"), method = "renderArmorLayer", locals = LocalCapture.CAPTURE_FAILHARD)
    public void onRenderArmorPiece(EntityLivingBase entityLivingBase, float f, float g, float h, float i, float j, float k, float l, EntityEquipmentSlot equipmentSlot, CallbackInfo ci, ItemStack b, ItemArmor a, T humanoidModel) {
        if (figura$avatar == null) return;

        VanillaPart part = RenderUtils.partFromSlot(figura$avatar, equipmentSlot);
        if (part != null) {
            part.save(humanoidModel);
            part.preTransform(humanoidModel);
            part.posTransform(humanoidModel);
        }
    }


    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/LayerArmorBase;setModelSlotVisible(Lnet/minecraft/client/model/ModelBase;Lnet/minecraft/inventory/EntityEquipmentSlot;)V", shift = At.Shift.AFTER), method = "renderArmorLayer", cancellable = true)
    public void renderArmorPieceHijack(EntityLivingBase entityLivingBase, float f, float g, float h, float i, float j, float k, float l, EntityEquipmentSlot slotIn, CallbackInfo ci) {
        if (figura$avatar == null) return;

        if (!figura$renderingVanillaArmor) {
            ci.cancel();
        }
    }


    @Inject(at = @At("RETURN"), method = "renderArmorLayer")
    public void postRenderArmorPiece(EntityLivingBase entityLivingBase, float f, float g, float h, float i, float j, float k, float l, EntityEquipmentSlot slotIn, CallbackInfo ci) {
        if (figura$avatar == null) return;

        VanillaPart part = RenderUtils.partFromSlot(figura$avatar, slotIn);
        if (part != null)
            part.restore(getModelFromSlot(slotIn));
    }

    @Unique
    private void figura$tryRenderArmorPart(EntityEquipmentSlot slot, float limbSwing, float limbSwingAmount, float tickDelta, float ageInTicks, float netHeadYaw, float headPitch, float scale, FiguraArmorPartRenderer<T> renderer, EntityLivingBase entity, ParentType... parentTypes) {
        if (slot == null) return; // ?
        VanillaPart part = RenderUtils.partFromSlot(figura$avatar, slot);
        if (figura$avatar.permissions.get(Permissions.VANILLA_MODEL_EDIT) == 1 && part != null && !part.checkVisible()) return;

        ItemStack itemStack = entity.getItemStackFromSlot(slot);
        T model = getModelFromSlot(slot);

        // Make sure the item in the equipment slot is actually a piece of armor
        if ((itemStack.getItem() instanceof ItemArmor && ((ItemArmor) itemStack.getItem()).getEquipmentSlot() == slot) && model instanceof ModelBiped) {
            ItemArmor armorItem = (ItemArmor) itemStack.getItem();
            ModelBiped armorModel = (ModelBiped) model;
            // Bones have to be their defaults to prevent issues with clipping
            armorModel.bipedBody.rotateAngleX = 0.0f;
            armorModel.bipedRightLeg.offsetZ = 0.0f;
            armorModel.bipedLeftLeg.offsetZ = 0.0f;
            armorModel.bipedRightLeg.offsetY = 12.0f;
            armorModel.bipedLeftLeg.offsetY = 12.0f;
            armorModel.bipedHead.offsetY = 0.0f;
            armorModel.bipedBody.offsetY = 0.0f;
            armorModel.bipedLeftArm.offsetY = 2.0f;
            armorModel.bipedRightArm.offsetY = 2.0f;
            armorModel.bipedLeftArm.offsetX = 5.0f;
            armorModel.bipedRightArm.offsetX = -5.0f;
            armorModel.bipedLeftArm.offsetZ = 0.0f;
            armorModel.bipedRightArm.offsetZ = 0.0f;

            boolean allFailed = true;

            // Don't render armor if GeckoLib is already doing the rendering
            if (!GeckoLibCompat.armorHasCustomModel(itemStack)) {
                // Go through each parent type needed to render the current piece of armor
                for (ParentType parentType : parentTypes) {
                            // Try to render the pivot part
                        GlStateManager.pushMatrix();
                        boolean renderedPivot = figura$avatar.pivotPartRender(parentType, stack -> {
                                figura$prepareArmorRender(); // TODO: check if needed probably no
                                renderer.renderArmorPart(model, entity, itemStack, slot, armorItem, parentType);
                            });

                            if (renderedPivot) {
                                allFailed = false;
                            }
                        }
                        GlStateManager.popMatrix();
            }
            // As a fallback, render armor the vanilla way, but avoid rendering if it's a geckolib armor as it would render twice on fabric, funky
            if (allFailed && (!GeckoLibCompat.armorHasCustomModel(itemStack) || PlatformUtils.getModLoader().equals(PlatformUtils.ModLoader.FORGE))) {
                figura$renderingVanillaArmor = true;
                renderArmorLayer(entity,limbSwing,limbSwingAmount, tickDelta, ageInTicks, netHeadYaw, headPitch, scale, slot);
                figura$renderingVanillaArmor = false;
            }
        }

    }

    // Prepare the transformations for rendering armor on the avatar
    @Unique
    private void figura$prepareArmorRender() { // TODO: check if this is necessary because we use the global pose stack
        GlStateManager.scale(16, 16, 16);
        GlStateManager.rotate(180.0f, 1.0f, 1.0f, 0.0f);
        //stack.mulPose(Vector3f.XP.rotationDegrees(180f)); stack.mulPose(Vector3f.YP.rotationDegrees(180f));
    }

    @Unique
    private void figura$helmetRenderer(T model, EntityLivingBase entity, ItemStack itemStack, EntityEquipmentSlot armorSlot, ItemArmor armorItem, ParentType parentType) {
        if (parentType == ParentType.HelmetPivot && model instanceof ModelBiped) {
            figura$renderArmorPart(((ModelBiped)model).bipedHead, entity, itemStack, armorSlot, armorItem);
            figura$renderArmorPart(((ModelBiped)model).bipedHeadwear, entity, itemStack, armorSlot, armorItem);
        }
    }

    @Unique
    private void figura$chestplateRenderer(T model, EntityLivingBase entity, ItemStack itemStack, EntityEquipmentSlot armorSlot, ItemArmor armorItem, ParentType parentType) {
        if (parentType == ParentType.ChestplatePivot && model instanceof ModelBiped) {
            figura$renderArmorPart(((ModelBiped)model).bipedBody, entity, itemStack, armorSlot, armorItem);
        }

        if (parentType == ParentType.LeftShoulderPivot && model instanceof ModelBiped) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(-6 / 16f, 0f, 0f);
            figura$renderArmorPart(((ModelBiped)model).bipedLeftArm, entity, itemStack, armorSlot, armorItem);
            GlStateManager.popMatrix();
        }

        if (parentType == ParentType.RightShoulderPivot && model instanceof ModelBiped) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(6 / 16f, 0f, 0f);
            figura$renderArmorPart(((ModelBiped)model).bipedRightArm, entity, itemStack, armorSlot, armorItem);
            GlStateManager.popMatrix();
        }
    }

    @Unique
    private void figura$leggingsRenderer(T model, EntityLivingBase entity, ItemStack itemStack, EntityEquipmentSlot armorSlot, ItemArmor armorItem, ParentType parentType) {
        if (parentType == ParentType.LeggingsPivot && model instanceof ModelBiped) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0, -12 / 16f, 0);
            figura$renderArmorPart(((ModelBiped)model).bipedBody, entity, itemStack, armorSlot, armorItem);
            GlStateManager.popMatrix();
        }

        if (parentType == ParentType.LeftLeggingPivot && model instanceof ModelBiped) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(-2 / 16f, -12 / 16f, 0);
            figura$renderArmorPart(((ModelBiped)model).bipedLeftArm, entity, itemStack, armorSlot, armorItem);
            GlStateManager.popMatrix();
        }

        if (parentType == ParentType.RightLeggingPivot && model instanceof ModelBiped) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(2 / 16f, -12 / 16f, 0);
            figura$renderArmorPart(((ModelBiped)model).bipedRightArm, entity, itemStack, armorSlot, armorItem);
            GlStateManager.popMatrix();
        }
    }

    @Unique
    private void figura$bootsRenderer(T model, EntityLivingBase entity, ItemStack itemStack, EntityEquipmentSlot armorSlot, ItemArmor armorItem, ParentType parentType) {
        if (parentType == ParentType.LeftBootPivot && model instanceof ModelBiped) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(-2 / 16f, -24 / 16f, 0);
            figura$renderArmorPart(((ModelBiped)model).bipedLeftLeg, entity, itemStack, armorSlot, armorItem);
            GlStateManager.popMatrix();
        }

        if (parentType == ParentType.RightBootPivot && model instanceof ModelBiped) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(2 / 16f, -24 / 16f, 0);
            figura$renderArmorPart(((ModelBiped)model).bipedRightLeg, entity, itemStack, armorSlot, armorItem);
            GlStateManager.popMatrix();
        }
    }


    // Similar to vanilla's renderArmorModel, but it renders each part individually, instead of the whole model at once.
    // Could be optimized by calculating the tint, overlays, and trims beforehand instead of re-calculating for each ModelPart, but it's not super important.
    @Unique
    private void figura$renderArmorPart(ModelRenderer modelPart, EntityLivingBase entity, ItemStack itemStack, EntityEquipmentSlot armorSlot, ItemArmor armorItem) {
        boolean bl = this.usesInnerModel(armorSlot);
        boolean hasOverlay = false;
        boolean hasGlint = itemStack.isItemEnchanted();

        modelPart.showModel = true;
        modelPart.rotateAngleX = 0;
        modelPart.rotateAngleY = 0;
        modelPart.rotateAngleZ = 0;

        float tintR = 1;
        float tintG = 1;
        float tintB = 1;

        if (armorItem.getArmorMaterial() == ItemArmor.ArmorMaterial.LEATHER) {
            int i = armorItem.getColor(itemStack);
            tintR = (float) (i >> 16 & 255) / 255.0F;
            tintG = (float) (i >> 8 & 255) / 255.0F;
            tintB = (float) (i & 255) / 255.0F;
            hasOverlay = true;
        }

        ResourceLocation normalArmorResource = getArmorResource(armorItem, bl);
        this.renderer.bindTexture(normalArmorResource);
        GlStateManager.color(this.colorR * tintR, this.colorG * tintG, this.colorB * tintB, this.alpha);
        modelPart.render(1f);

        if (hasOverlay) {
            GlStateManager.color(this.colorR, this.colorG, this.colorB, this.alpha);
            this.renderer.bindTexture(getArmorResource(armorItem, bl, "overlay"));
            modelPart.render(1f);
        }

        if (hasGlint) {
            RenderTypes.FiguraRenderType renderType = RenderTypes.GLINT.get(ENCHANTED_ITEM_GLINT_RES);
            renderType.setupState.run();
            modelPart.render(1.0f);
            renderType.clearState.run();
        }
    }
}
