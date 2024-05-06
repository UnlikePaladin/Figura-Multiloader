package org.figuramc.figura.mixin.render.layers.items;

import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;

/**
 * This class only exists because of spyglass jank.
 * Has literally the exact same code as ItemInHandLayerMixin, just for the spyglass specifically.
 * For now, at least. Once spyglass category part exists, it may be different.
 *     The spyglass didn't exist in 1.12 ;)
 */
public abstract class PlayerItemInHandLayerMixin extends LayerHeldItem {

    public PlayerItemInHandLayerMixin(RenderLivingBase renderLayerParent) {
        super(renderLayerParent);
    }
}
