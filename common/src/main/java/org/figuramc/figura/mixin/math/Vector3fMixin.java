package org.figuramc.figura.mixin.math;

import org.figuramc.figura.ducks.extensions.Vector3fExtension;
import org.figuramc.figura.utils.MathUtils;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Vector3f.class)
public abstract class Vector3fMixin implements Vector3fExtension {

    @Shadow public float x;

    @Shadow public float y;

    @Shadow public float z;

    @Shadow public abstract void set(float x, float y, float z);

    @Override
    public void figura$transform(Matrix3f matrix) {
        float f = this.x;
        float f1 = this.y;
        float f2 = this.z;
        this.x = matrix.m00 * f + matrix.m01 * f1 + matrix.m02 * f2;
        this.y = matrix.m10 * f + matrix.m11 * f1 + matrix.m12 * f2;
        this.z = matrix.m20 * f + matrix.m21 * f1 + matrix.m22 * f2;
    }

    @Override
    public void figura$transform(Quaternion rotation) {
        Quaternion quaternion = new Quaternion(rotation);
        Quaternion.mul(quaternion, new Quaternion(this.x, this.y, this.z, 0.0f), quaternion);
        Quaternion quaternion1 = new Quaternion(rotation);
        quaternion1.negate();
        Quaternion.mul(quaternion, quaternion1, quaternion);
        this.set(quaternion.getX(), quaternion.getY(), quaternion.getZ());
    }

    @Override
    public Quaternion figura$rotationDegrees(float f) {
        return MathUtils.Quaternion(((Vector3f)(Object)this), f, true);
    }
}