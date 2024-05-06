package org.figuramc.figura.mixin.render;

import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Frustum.class)
public interface FrustumAccessor {
    @Accessor("x")
    double cameraPosX();
    @Accessor("y")
    double cameraPosY();
    @Accessor("z")
    double cameraPosZ();

}
