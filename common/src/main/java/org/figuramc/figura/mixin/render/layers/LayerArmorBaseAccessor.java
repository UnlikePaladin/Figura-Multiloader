package org.figuramc.figura.mixin.render.layers;

import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LayerArmorBase.class)
public interface LayerArmorBaseAccessor {
    @Accessor("ENCHANTED_ITEM_GLINT_RES")
    static ResourceLocation getItemGlint() {
        throw new AssertionError();
    }
}
