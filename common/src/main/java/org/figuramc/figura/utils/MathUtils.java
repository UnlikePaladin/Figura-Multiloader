package org.figuramc.figura.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.util.math.Vec3d;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.ducks.GameRendererAccessor;
import org.figuramc.figura.ducks.extensions.Vector3fExtension;
import org.figuramc.figura.ducks.extensions.Vector4fExtension;
import org.figuramc.figura.math.matrix.FiguraMat2;
import org.figuramc.figura.math.matrix.FiguraMat3;
import org.figuramc.figura.math.matrix.FiguraMat4;
import org.figuramc.figura.math.matrix.FiguraMatrix;
import org.figuramc.figura.math.vector.FiguraVec2;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.math.vector.FiguraVec4;
import org.figuramc.figura.math.vector.FiguraVector;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.mixin.render.ActiveRenderInfoAccessor;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.lang.Math;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.function.IntPredicate;

public class MathUtils {

    public static final double DEG_TO_RAD = Math.PI / 180;
    public static final double RAD_TO_DEG = 180 / Math.PI;

    public static FiguraVector<?, ?> sizedVector(double... vals) {
        switch (vals.length) {
            case 2:
                return FiguraVec2.of(vals[0], vals[1]);
            case 3:
                return FiguraVec3.of(vals[0], vals[1], vals[2]);
            case 4:
                return FiguraVec4.of(vals[0], vals[1], vals[2], vals[3]);
            default:
                throw new IllegalStateException("Cannot create vector of size: " + vals.length);
        }
    }

    public static FiguraMatrix<?, ?> sizedMat(FiguraVector<?, ?>... vectors) {
        switch (vectors.length) {
            case 4:
                return FiguraMat4.of(
                        vectors[0].index(0), vectors[0].index(1), vectors[0].index(2), vectors[0].index(3),
                        vectors[1].index(0), vectors[1].index(1), vectors[1].index(2), vectors[1].index(3),
                        vectors[2].index(0), vectors[2].index(1), vectors[2].index(2), vectors[2].index(3),
                        vectors[3].index(0), vectors[3].index(1), vectors[3].index(2), vectors[3].index(3)
                );
            case 3:
                return FiguraMat3.of(
                        vectors[0].index(0), vectors[0].index(1), vectors[0].index(2),
                        vectors[1].index(0), vectors[1].index(1), vectors[1].index(2),
                        vectors[2].index(0), vectors[2].index(1), vectors[2].index(2)
                );
            case 2:
                return FiguraMat2.of(
                        vectors[0].index(0), vectors[0].index(1),
                        vectors[1].index(0), vectors[1].index(1)
                );
            default:
                throw new IllegalStateException("Cannot create matrix of size: " + vectors.length);
        }
    }

    // maya pls check those // ty <3 <3
    public static FiguraVec3 rotateAroundAxis(FiguraVec3 vec, FiguraVec3 axis, double degrees) {
        FiguraVec3 normalizedAxis = axis.normalized();
        Quaternion vectorQuat = new Quaternion((float) vec.x, (float) vec.y, (float) vec.z, 0);
        Quaternion rotatorQuat = new Quaternion((float) normalizedAxis.x, (float) normalizedAxis.y, (float) normalizedAxis.z, (float) degrees);
        Quaternion rotatorQuatConj = new Quaternion(rotatorQuat);
        rotatorQuatConj.negate();

        Quaternion.mul(rotatorQuat, vectorQuat, rotatorQuat);
        Quaternion.mul(rotatorQuat, rotatorQuatConj, rotatorQuat);

        return FiguraVec3.of(rotatorQuat.getX(), rotatorQuat.getY(), rotatorQuat.getZ());
    }

    public static FiguraVec3 toCameraSpace(FiguraVec3 vec) {
        ActiveRenderInfo camera = Minecraft.getMinecraft().renderGlobal.getMainCamera();

        FiguraMat3 transformMatrix = FiguraMat3.of().set(new Matrix3f(camera.rotation()));
        Vec3d pos = ActiveRenderInfoAccessor.getPos();
        transformMatrix.invert();

        FiguraVec3 ret = vec.copy();
        ret.subtract(pos.x, pos.y, pos.z);
        ret.transform(transformMatrix);
        // ret.multiply(-1, 1, 1);

        return ret;
    }

    public static FiguraVec4 worldToScreenSpace(FiguraVec3 worldSpace) {
        Minecraft minecraft = Minecraft.getMinecraft();
        ActiveRenderInfo camera = minecraft.gameRenderer.getMainCamera();
        Matrix3f transformMatrix = new Matrix3f(camera.rotation());
        transformMatrix.invert();

        Vec3d camPos = ActiveRenderInfoAccessor.getPos();
        FiguraVec3 posDiff = worldSpace.copy().subtract(camPos.x, camPos.y, camPos.z);
        Vector3f camSpace = posDiff.asVec3f();
        ((Vector3fExtension)camSpace).figura$transform(transformMatrix);

        Vector4f projectiveCamSpace = new Vector4f(camSpace.x, camSpace.y, camSpace.z, 0);
        Matrix4f projMat = new Matrix4f();
        projMat.load(ActiveRenderInfoAccessor.getProjectionBuf());
        ((Vector4fExtension)projectiveCamSpace).figura$transform(projMat);
        float w = projectiveCamSpace.w;

        return FiguraVec4.of(projectiveCamSpace.x / w, projectiveCamSpace.y / w, projectiveCamSpace.z / w, Math.sqrt(posDiff.dot(posDiff)));
    }

    private static final String[] SIZE_UNITS = {"b", "kb", "mb", "gb"};

    public static String asFileSize(double size) {
        int i = 0;
        while (i < SIZE_UNITS.length) {
            if (size < 1000) break;
            size /= 1000;
            i++;
        }

        DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(size) + SIZE_UNITS[i];
    }

    public static float magicDelta(float speed, float delta) {
        return Configs.REDUCED_MOTION.value ? 1f : (float) (1f - Math.pow(speed, delta));
    }

    public static FiguraVec3 min(FiguraVec3 val, double min) {
        return FiguraVec3.of(
                Math.min(val.x, min),
                Math.min(val.y, min),
                Math.min(val.z, min)
        );
    }

    public static FiguraVec3 max(FiguraVec3 val, double max) {
        return FiguraVec3.of(
                Math.max(val.x, max),
                Math.max(val.y, max),
                Math.max(val.z, max)
        );
    }

    public static FiguraVec3 clamp(FiguraVec3 val, double min, double max) {
        return FiguraVec3.of(
                Math.min(Math.max(val.x, min), max),
                Math.min(Math.max(val.y, min), max),
                Math.min(Math.max(val.z, min), max)
        );
    }

    public static FiguraVec3 lerp(double delta, FiguraVec3 prev, FiguraVec3 next) {
        return FiguraVec3.of(
                lerp(delta, prev.x, next.x),
                lerp(delta, prev.y, next.y),
                lerp(delta, prev.z, next.z)
        );
    }

    public static double lerp(double delta, double prev, double next) {
        return delta * (next - prev) + prev;
    }

    public static FiguraVec3 catmullrom(double delta, FiguraVec3 prevA, FiguraVec3 prevB, FiguraVec3 nextA, FiguraVec3 nextB) {
        return FiguraVec3.of(
                catmullrom(delta, prevA.x, prevB.x, nextA.x, nextB.x),
                catmullrom(delta, prevA.y, prevB.y, nextA.y, nextB.y),
                catmullrom(delta, prevA.z, prevB.z, nextA.z, nextB.z)
        );
    }

    // no idea how it works
    // it is the same function from minecraft, but using doubles instead
    public static double catmullrom(double delta, double prevA, double prevB, double nextA, double nextB) {
        return 0.5 * (2 * prevB + (nextA - prevA) * delta + (2 * prevA - 5 * prevB + 4 * nextA - nextB) * delta * delta + (3 * prevB - prevA - 3 * nextA + nextB) * delta * delta * delta);
    }

    // bezier function generated by ChatGPT
    public static double bezier(double t, double p0, double p1, double p2, double p3) {
        double d = 1 - t;
        return p0 * d * d * d + 3 * p1 * d * d * t + 3 * p2 * d * t * t + p3 * t * t * t;
    }

    // secant method for finding bezier t based on x, also provided by ChatGPT
    public static double bezierFindT(double x, double p0, double p1, double p2, double p3) {
        double x0 = 0.4;
        double x1 = 0.6;
        double tolerance = 0.001;
        int iterations = 100;

        for (int i = 0; i < iterations; i++) {
            double fx1 = bezier(x1, p0, p1, p2, p3) - x;
            double fx0 = bezier(x0, p0, p1, p2, p3) - x;
            double xNext = x1 - fx1 * (x1 - x0) / (fx1 - fx0);
            if (Math.abs(xNext - x1) < tolerance)
                return xNext;
            x0 = x1;
            x1 = xNext;
        }

        return x1;
    }

    //same as minecraft too, but with doubles and fixing the NaN in the Math.asin
    public static FiguraVec3 quaternionToYXZ(Quaternion quaternion) {
        double r, i, j, k;
        r = quaternion.getZ();
        i = quaternion.getX();
        j = quaternion.getY();
        k = quaternion.getZ();

        double f = r * r;
        double g = i * i;
        double h = j * j;
        double m = k * k;
        double n = f + g + h + m;
        double o = 2 * r * i - 2 * j * k;
        double l = Math.asin(Math.max(Math.min(1, o / n), -1));
        return Math.abs(o) > 0.999d * n ?
                FiguraVec3.of(l, 2 * Math.atan2(j, r), 0) :
                FiguraVec3.of(l, Math.atan2(2 * i * k + 2 * j * r, f - g - h + m), Math.atan2(2 * i * j + 2 * r * k, f - g + h - m));
    }

    public static int binarySearch(int start, int end, IntPredicate leftPredicate) {
        int i = end - start;
        while (i > 0) {
            int j = i / 2;
            int k = start + j;
            if (leftPredicate.test(k)) {
                i = j;
                continue;
            }
            start = k + 1;
            i -= j + 1;
        }
        return start;
    }

    public static Quaternion Quaternion(Vector3f vector3f, float f, boolean isDegrees) {
        Quaternion newQ = new Quaternion();
        if (isDegrees) {
            f *= 0.017453292F;
        }

        float g = (float) Math.sin(f / 2.0F);
        newQ.x = vector3f.x * g;
        newQ.y = vector3f.y * g;
        newQ.z = vector3f.z * g;
        newQ.w = (float) Math.cos(f / 2.0F);
        return newQ;
    }
}
