package org.figuramc.figura.mixin.render.layers.elytra;

import net.minecraft.client.renderer.entity.layers.LayerElytra;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LayerElytra.class)
public interface LayerElytraAccessor {

    @Intrinsic
    @Accessor("TEXTURE_ELYTRA")
    static ResourceLocation getWingsLocation() {
        throw new AssertionError();
    }
}
