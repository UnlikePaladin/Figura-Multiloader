package org.figuramc.figura.mixin.render;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import org.figuramc.figura.ducks.ModelPlayerAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ModelPlayer.class)
public abstract class ModelPlayerMixin extends ModelBiped implements ModelPlayerAccessor {

    // Fake cape ModelPart which we set rotations of.
    // This is because the internal cape renderer uses the matrix stack,
    // instead of setting rotations like every single other ModelPart they render...
    @Unique
    public ModelRenderer fakeCloak = new ModelRenderer(this, 0,0);

    @Final
    @Shadow
    private ModelRenderer bipedCape;

    public ModelPlayerMixin() {
        super(0);
    }

    @Override
    public ModelRenderer figura$getCloak() {
        return bipedCape;
    }

    @Override
    public ModelRenderer figura$getFakeCloak() {
        return fakeCloak;
    }

}
