package org.figuramc.figura.mixin.render.layers;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.ModelPlayerAccessor;
import org.figuramc.figura.lua.api.vanilla_model.VanillaPart;
import org.figuramc.figura.model.rendering.texture.RenderTypes;
import org.figuramc.figura.permissions.Permissions;
import org.figuramc.figura.utils.MathUtils;
import org.figuramc.figura.utils.RenderUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LayerCape.class)
public abstract class LayerCapeMixin implements LayerRenderer<AbstractClientPlayer> {
    @Shadow @Final private RenderPlayer playerRenderer;
    @Unique
    private Avatar avatar;

    @Inject(method = "doRenderLayer(Lnet/minecraft/client/entity/AbstractClientPlayer;FFFFFFF)V", at = @At("HEAD"))
    private void preRender(AbstractClientPlayer entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch, float scale, CallbackInfo ci) {
        ItemStack itemStack = entity.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
        if (entity.isInvisible() || itemStack.getItem() == Items.ELYTRA)
            return;

        avatar = AvatarManager.getAvatar(entity);
        if (avatar == null)
            return;

        // Acquire reference to fake cloak
        ModelRenderer fakeCloak = ((ModelPlayerAccessor) playerRenderer.getMainModel()).figura$getFakeCloak();
        ModelRenderer realCloak = ((ModelPlayerAccessor) playerRenderer.getMainModel()).figura$getCloak();

        // Do math for fake cloak
        fakeCloak.rotateAngleX = realCloak.rotateAngleX;
        fakeCloak.rotateAngleY = realCloak.rotateAngleY;
        fakeCloak.rotateAngleZ = realCloak.rotateAngleZ;
        fakeCloak.rotationPointX = realCloak.rotationPointX;
        fakeCloak.rotationPointY = realCloak.rotationPointY;
        fakeCloak.rotationPointZ = realCloak.rotationPointZ;
        fakeCloak.offsetX = realCloak.offsetX; // Before MC used the PoseStack it stored offset transformations for models in these
        fakeCloak.offsetY = realCloak.offsetY;
        fakeCloak.offsetZ = realCloak.offsetZ;

        // REFERENCED FROM CODE IN CapeLayer (CapeFeatureRenderer for Yarn)
        double d = MathUtils.lerp(tickDelta, entity.prevChasingPosX, entity.chasingPosX) - MathUtils.lerp(tickDelta, entity.prevPosX, entity.posX);
        double e = MathUtils.lerp(tickDelta, entity.prevChasingPosY, entity.chasingPosY) - MathUtils.lerp(tickDelta, entity.prevPosY, entity.posY);
        double m = MathUtils.lerp(tickDelta, entity.prevChasingPosZ, entity.chasingPosZ) - MathUtils.lerp(tickDelta, entity.prevPosZ, entity.posZ);
        float n = entity.prevRenderYawOffset + tickDelta * MathHelper.wrapDegrees(entity.prevRenderYawOffset - entity.renderYawOffset); // eq to rotLerp
        n = (float) Math.toRadians(n);
        double o = MathHelper.sin(n);
        double p = -MathHelper.cos(n);
        float q = (float) e * 10f;
        q = MathHelper.clamp(q, -6f, 32f);
        float r = (float) (d * o + m * p) * 100f;
        r = MathHelper.clamp(r, 0f, 150f);
        float s = (float) (d * p - m * o) * 100f;
        s = MathHelper.clamp(s, -20f, 20f);
        r = Math.max(r, 0f);
        float t = (float) MathUtils.lerp(tickDelta, entity.prevCameraYaw, entity.cameraYaw); // cameraYaw = bob
        q += MathHelper.sin((float) (MathUtils.lerp(tickDelta, entity.prevDistanceWalkedModified, entity.distanceWalkedModified) * 6f)) * 32f * t;

        // Just going to ignore the fact that vanilla uses XZY rotation order for capes...
        // As a result, the cape rotation is slightly off.
        // Another inaccuracy results from the fact that the cape also moves its position without moving its pivot point,
        // I'm pretty sure. This is due to it using the matrix stack instead of setting x,y,z,xRot,yRot,zRot on the parts.
        // The cape functions completely differently than all other model parts of the player. Quite frankly,
        // I don't want to deal with it any more than I already have, and I'm just going to leave this alone now and call it
        // close enough.

        // If someone wants to spend the time to correct these inaccuracies for us, feel free to make a pull request.

        // pos
        if (itemStack.isEmpty() || (avatar.luaRuntime != null && !avatar.luaRuntime.vanilla_model.CHESTPLATE_BODY.checkVisible())) {
            if (entity.isSneaking()) {
                q += 25f;
                fakeCloak.rotationPointY = 2.25f;
                fakeCloak.rotationPointZ = -0.25f;
            } else {
                fakeCloak.rotationPointY = 0f;
                fakeCloak.rotationPointZ = 0f;
            }
        } else if (entity.isSneaking()) {
            q += 25f;
            fakeCloak.rotationPointY = 0.85f;
            fakeCloak.rotationPointZ = 0.15f;
        } else {
            fakeCloak.rotationPointY = -1f;
            fakeCloak.rotationPointZ = 1f;
        }

        // rot
        fakeCloak.rotateAngleX = (float) Math.toRadians(6f + r / 2f + q);
        fakeCloak.rotateAngleY = (float) -Math.toRadians(s / 2f);
        fakeCloak.rotateAngleZ = (float) Math.toRadians(s / 2f);

        // Copy rotations from fake cloak
        if (avatar.luaRuntime != null) {
            VanillaPart part = avatar.luaRuntime.vanilla_model.CAPE;
            ModelPlayer model = playerRenderer.getMainModel();
            part.save(model);
            if (avatar.permissions.get(Permissions.VANILLA_MODEL_EDIT) == 1)
                part.preTransform(model);
        }

        avatar.capeRender(entity, RenderTypes.FiguraBufferSource.INSTANCE, entity.getBrightnessForRender(), tickDelta, fakeCloak);

        // Setup visibility for real cloak
        if (RenderUtils.vanillaModelAndScript(avatar))
            avatar.luaRuntime.vanilla_model.CAPE.posTransform(playerRenderer.getMainModel());
    }

    @Inject(method = "doRenderLayer(Lnet/minecraft/client/entity/AbstractClientPlayer;FFFFFFF)V", at = @At("RETURN"))
    private void postRender(AbstractClientPlayer abstractClientPlayer, float f, float g, float h, float i, float j, float k, float scale, CallbackInfo ci) {
        if (avatar == null)
            return;

        if (avatar.luaRuntime != null)
            avatar.luaRuntime.vanilla_model.CAPE.restore(playerRenderer.getMainModel());

        avatar = null;
    }
}
