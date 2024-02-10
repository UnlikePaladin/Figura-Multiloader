package org.figuramc.figura.mixin.render;

import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.nio.FloatBuffer;

@Mixin(ActiveRenderInfo.class)
public interface ActiveRenderInfoAccessor {
    @Accessor("position")
    static Vec3d getPos() {
        throw new AssertionError();
    }

    @Accessor("PROJECTION")
    static FloatBuffer getProjectionBuf() {
        throw new AssertionError();
    }
}