package org.figuramc.figura.mixin.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import org.figuramc.figura.ducks.ParticleEngineAccessor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Mixin(ParticleManager.class)
public abstract class ParticleEngineMixin implements ParticleEngineAccessor {



    @Shadow public abstract void addEffect(Particle particle);

    @Shadow public abstract @Nullable Particle spawnEffectParticle(int i, double d, double e, double f, double xSpeed, double h, double j, int... is);

    @Shadow @Final private TextureManager renderer;
    @Unique private final HashMap<Particle, UUID> particleMap = new HashMap<>();

    // This fixes a conflict with Optifine having slightly different args + it should be more stable in general, capturing Locals is bad practice
    @ModifyVariable(method = "tickParticleList", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;remove()V", ordinal = 0))
    private Particle tickParticleList(Particle particle) {
        particleMap.remove(particle);
        return particle;
    }

    @Override @Intrinsic
    public Particle figura$makeParticle(EnumParticleTypes type, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
        return spawnEffectParticle(type.getParticleID(), x, y, z, velocityX, velocityY, velocityZ);
    }

    @Override @Intrinsic
    public void figura$spawnParticle(Particle particle, UUID owner) {
        particleMap.put(particle, owner);
        this.addEffect(particle);
    }

    @Override @Intrinsic
    public void figura$clearParticles(UUID owner) {
        Iterator<Map.Entry<Particle, UUID>> iterator = particleMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Particle, UUID> entry = iterator.next();
            if ((owner == null || entry.getValue().equals(owner))) {
                if (entry.getKey() != null)
                    entry.getKey().setExpired();
                iterator.remove();
            }
        }
    }

    @Override @Intrinsic
    public TextureAtlasSprite figura$getParticleSprite(ResourceLocation particleID) {
        return renderer.getTexture(particleID);
    }
}
