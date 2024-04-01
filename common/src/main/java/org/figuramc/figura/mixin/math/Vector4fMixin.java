package org.figuramc.figura.mixin.math;

import net.minecraft.client.renderer.Matrix4f;
import org.figuramc.figura.ducks.extensions.Vector4fExtension;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Vector4f.class)
public abstract class Vector4fMixin implements Vector4fExtension {

    @Shadow public float x;

    @Shadow public float y;

    @Shadow public float z;

    @Shadow public float w;


    @Shadow public abstract void set(float x, float y, float z, float w);

    @Override
    public void figura$transform(Matrix4f matrix) {
        float f = this.x;
        float g = this.y;
        float h = this.z;
        float i = this.w;
        this.x = matrix.m00 * f + matrix.m01 * g + matrix.m02 * h + matrix.m03 * i;
        this.y = matrix.m10 * f + matrix.m11 * g + matrix.m12 * h + matrix.m13 * i;
        this.z = matrix.m20 * f + matrix.m21 * g + matrix.m22 * h + matrix.m23 * i;
        this.w = matrix.m30 * f + matrix.m31 * g + matrix.m32 * h + matrix.m33 * i;
    }

    @Override
    public void figura$transform(Quaternion rotation) {
        Quaternion quaternion = new Quaternion(rotation);
        Quaternion.mul(quaternion, new Quaternion(this.x, this.y, this.z, 0.0f), quaternion);
        Quaternion quaternion2 = new Quaternion(rotation);
        quaternion2.negate();
        Quaternion.mul(quaternion, quaternion2, quaternion);
        this.set(quaternion.getX(), quaternion.getY(), quaternion.getZ(), this.w);
    }

    @Override
    public Quaternion figura$rotationDegrees(float angle) {
        Quaternion quat = new Quaternion((Vector4f)(Object)this);
        angle *= (float)Math.PI / 180;
        float f = (float) Math.sin(angle / 2.0f);
        quat.x = quat.x *f;
        quat.y = quat.y *f;
        quat.z = quat.z *f;
        quat.w = quat.w *f;
        return quat;
    }
}
