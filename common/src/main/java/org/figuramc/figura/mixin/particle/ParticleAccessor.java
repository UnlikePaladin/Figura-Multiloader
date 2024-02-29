package org.figuramc.figura.mixin.particle;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Particle.class)
public interface ParticleAccessor {

    @Intrinsic
    @Accessor("particleGravity")
    void setGravity(float gravity);

    @Intrinsic
    @Accessor("particleGravity")
    float getGravity();

    @Intrinsic
    @Accessor("canCollide")
    void setHasPhysics(boolean physics);

    @Intrinsic
    @Accessor("canCollide")
    boolean getHasPhysics();

    @Intrinsic
    @Accessor("prevPosX")
    void setXo(double xo);

    @Intrinsic
    @Accessor("prevPosY")
    void setYo(double yo);

    @Intrinsic
    @Accessor("prevPosZ")
    void setZo(double zo);

    @Intrinsic
    @Invoker("setAlphaF")
    void setParticleAlpha(float alpha);

    @Intrinsic
    @Accessor("posX")
    double getX();

    @Intrinsic
    @Accessor("posY")
    double getY();

    @Intrinsic
    @Accessor("posZ")
    double getZ();

    @Intrinsic
    @Accessor("width")
    float getBbWidth();

    @Intrinsic
    @Accessor("particleRed")
    float getRCol();

    @Intrinsic
    @Accessor("particleGreen")
    float getGCol();

    @Intrinsic
    @Accessor("particleBlue")
    float getBCol();

    @Intrinsic
    @Accessor("particleAlpha")
    float getAlpha();

    @Intrinsic
    @Accessor("motionX")
    double getXd();

    @Intrinsic
    @Accessor("motionY")
    double getYd();

    @Intrinsic
    @Accessor("motionZ")
    double getZd();

    @Intrinsic
    @Accessor("particleMaxAge")
    int getLifetime();
}
