package org.figuramc.figura.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import org.figuramc.figura.ducks.ModelPlayerAccessor;
import org.figuramc.figura.mixin.render.layers.elytra.ModelElytraAccessor;

import java.util.function.Function;

public enum VanillaModelProvider {
    HEAD(model -> model instanceof ModelBiped ? ((ModelBiped) model).bipedHead : null),
    BODY(model -> model instanceof ModelBiped ? ((ModelBiped) model).bipedBody : null),
    LEFT_ARM(model -> model instanceof ModelBiped ? ((ModelBiped) model).bipedLeftArm : null),
    RIGHT_ARM(model -> model instanceof ModelBiped ? ((ModelBiped) model).bipedRightArm : null),
    LEFT_LEG(model -> model instanceof ModelBiped ? ((ModelBiped) model).bipedLeftLeg : null),
    RIGHT_LEG(model -> model instanceof ModelBiped ? ((ModelBiped) model).bipedRightLeg : null),
    HAT(model -> model instanceof ModelBiped ? ((ModelBiped) model).bipedHeadwear : null),

    JACKET(model -> model instanceof ModelPlayer ? ((ModelPlayer) model).bipedBodyWear : null),
    LEFT_SLEEVE(model -> model instanceof ModelPlayer ? ((ModelPlayer) model).bipedLeftArmwear : null),
    RIGHT_SLEEVE(model -> model instanceof ModelPlayer ? ((ModelPlayer) model).bipedRightArmwear : null),
    LEFT_PANTS(model -> model instanceof ModelPlayer ? ((ModelPlayer) model).bipedLeftLegwear : null),
    RIGHT_PANTS(model -> model instanceof ModelPlayer ? ((ModelPlayer) model).bipedRightLegwear : null),

    CAPE(model -> model instanceof ModelPlayer ? ((ModelPlayerAccessor) model).figura$getCloak() : null),
    FAKE_CAPE(model -> model instanceof ModelPlayer ? ((ModelPlayerAccessor) model).figura$getFakeCloak() : null),

    LEFT_ELYTRON(model -> model instanceof ModelElytraAccessor ? ((ModelElytraAccessor) model).getLeftWing() : null),
    RIGHT_ELYTRON(model -> model instanceof ModelElytraAccessor ? ((ModelElytraAccessor) model).getRightWing() : null);

    public final Function<ModelBase, ModelRenderer> func;

    VanillaModelProvider(Function<ModelBase, ModelRenderer> func) {
        this.func = func;
    }
}