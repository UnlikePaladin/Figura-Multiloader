package org.figuramc.figura.mixin.particle;

import net.minecraft.client.particle.Particle;
import org.figuramc.figura.ducks.ParticleAccessor;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Particle.class)
public abstract class ParticleMixin implements ParticleAccessor {

    @Shadow protected float particleScale;

    @Override
    @Intrinsic
    public void figura$fixQuadSize() {
        this.particleScale = 0.2f;
    }

    @Override
    @Intrinsic
    public void figura$setParticleSize(float size) {
        this.particleScale = size;
    }
}
