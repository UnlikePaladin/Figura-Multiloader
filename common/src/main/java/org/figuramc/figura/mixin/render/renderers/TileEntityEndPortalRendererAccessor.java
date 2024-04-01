package org.figuramc.figura.mixin.render.renderers;

import net.minecraft.client.renderer.tileentity.TileEntityEndPortalRenderer;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TileEntityEndPortalRenderer.class)
public interface TileEntityEndPortalRendererAccessor {
    @Accessor("END_SKY_TEXTURE")
    @Final
    static ResourceLocation getEndSkyTexture() {
        throw new AssertionError();
    }

    @Accessor("END_PORTAL_TEXTURE")
    @Final
    static ResourceLocation getEndPortalTexture() {
        throw new AssertionError();
    }
}
