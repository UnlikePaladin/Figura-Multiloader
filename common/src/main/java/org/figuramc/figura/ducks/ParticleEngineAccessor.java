package org.figuramc.figura.ducks;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;

import java.util.UUID;

public interface ParticleEngineAccessor {

    Particle figura$makeParticle(EnumParticleTypes type, double x, double y, double z, double velocityX, double velocityY, double velocityZ);
    void figura$spawnParticle(Particle particle, UUID owner);
    void figura$clearParticles(UUID owner);
    ITextureObject figura$getParticleSprite(ResourceLocation particleID);
}
