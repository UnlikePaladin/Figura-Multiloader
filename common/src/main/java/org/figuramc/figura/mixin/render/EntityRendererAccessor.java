package org.figuramc.figura.mixin.render;

import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityRenderer.class)
public interface EntityRendererAccessor {

    @Intrinsic
    @Accessor("SHADERS_TEXTURES")
    static ResourceLocation[] getEffects() {
        throw new AssertionError();
    }
}