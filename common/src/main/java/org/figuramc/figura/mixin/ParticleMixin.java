package org.figuramc.figura.mixin;

import net.minecraft.client.particle.Particle;
import org.figuramc.figura.ducks.extensions.ParticleExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Particle.class)
public class ParticleMixin implements ParticleExtension {
    @Shadow protected double motionX;

    @Shadow protected double motionY;

    @Shadow protected double motionZ;

    @Override
    public void figura$setParticleSpeed(double velocityX, double velocityY, double velocityZ) {
        this.motionX = velocityX;
        this.motionY = velocityY;
        this.motionZ = velocityZ;
    }
}
