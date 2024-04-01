package org.figuramc.figura.mixin.render.layers.elytra;

import net.minecraft.client.model.ModelElytra;
import net.minecraft.client.model.ModelRenderer;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ModelElytra.class)
public interface ModelElytraAccessor {
    @Intrinsic
    @Accessor
    ModelRenderer getLeftWing();

    @Intrinsic
    @Accessor
    ModelRenderer getRightWing();
}
