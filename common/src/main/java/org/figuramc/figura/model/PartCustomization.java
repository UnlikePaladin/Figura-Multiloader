package org.figuramc.figura.model;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Matrix4f;
import org.figuramc.figura.math.matrix.FiguraMat3;
import org.figuramc.figura.math.matrix.FiguraMat4;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.model.rendering.texture.RenderTypes;
import org.figuramc.figura.utils.MathUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix3f;

import java.nio.FloatBuffer;
import java.util.Stack;

public class PartCustomization {

    // -- Matrix thingies --// 
    /**
     * Boolean exists because blockbench sucks and uses a different rotation
     * formula for meshes than other types of items. Meshes rotate in XYZ order,
     * while literally everything else is in ZYX order.
     */
    public PartType partType = PartType.GROUP;

    public final FiguraMat4 positionMatrix = FiguraMat4.of();
    public final FiguraMat3 uvMatrix = FiguraMat3.of();
    public final FiguraMat3 normalMatrix = FiguraMat3.of();

    public boolean needsMatrixRecalculation = false;
    public Boolean visible = null;
    public Boolean vanillaVisible = null;

    private final FiguraVec3 position = FiguraVec3.of();
    private final FiguraVec3 rotation = FiguraVec3.of();
    private final FiguraVec3 scale = FiguraVec3.of(1, 1, 1);
    private final FiguraVec3 pivot = FiguraVec3.of();

    // The "offset" values are for vanilla part scaling. The offset pivot and rot can be get and set from script.
    private final FiguraVec3 offsetPivot = FiguraVec3.of();
    private final FiguraVec3 offsetPos = FiguraVec3.of();
    private final FiguraVec3 offsetRot = FiguraVec3.of();
    private final FiguraVec3 offsetScale = FiguraVec3.of(1, 1, 1);

    // These values are set by animation players. They can be queried, though not set, by script.
    private final FiguraVec3 animPos = FiguraVec3.of();
    private final FiguraVec3 animRot = FiguraVec3.of();
    private final FiguraVec3 animScale = FiguraVec3.of(1, 1, 1);

    public final FiguraVec3 stackScale = FiguraVec3.of(1, 1, 1);
    public final FiguraVec3 color = FiguraVec3.of(1, 1, 1);
    public final FiguraVec3 color2 = FiguraVec3.of(1, 1, 1);
    public Float alpha = null;
    public Integer light = null;
    public Integer overlay = null;

    private RenderTypes primaryRenderType, secondaryRenderType;
    public TextureCustomization primaryTexture, secondaryTexture;

    /**
     * Recalculates the matrix if necessary.
     */
    public void recalculate() {
        if (!needsMatrixRecalculation)
            return;

        positionMatrix.reset();

        // Position the pivot point at 0, 0, 0, and translate the part
        positionMatrix.translate(
                offsetPos.x - pivot.x - offsetPivot.x,
                offsetPos.y - pivot.y - offsetPivot.y,
                offsetPos.z - pivot.z - offsetPivot.z
        );

        // Scale the model part around the pivot
        stackScale.set(
                offsetScale.x * scale.x * animScale.x,
                offsetScale.y * scale.y * animScale.y,
                offsetScale.z * scale.z * animScale.z
        );
        positionMatrix.scale(stackScale);

        // Rotate the model part around the pivot
        if (partType == PartType.MESH) {
            positionMatrix.rotateZ(rotation.z + offsetRot.z + animRot.z);
            positionMatrix.rotateY(rotation.y + offsetRot.y + animRot.y);
            positionMatrix.rotateX(rotation.x + offsetRot.x + animRot.x);
        } else {
            positionMatrix.rotateZYX(
                    rotation.x + offsetRot.x + animRot.x,
                    rotation.y + offsetRot.y + animRot.y,
                    rotation.z + offsetRot.z + animRot.z
            );
        }

        // Undo the effects of the pivot translation
        positionMatrix.translate(
                position.x + animPos.x + pivot.x + offsetPivot.x,
                position.y + animPos.y + pivot.y + offsetPivot.y,
                position.z + animPos.z + pivot.z + offsetPivot.z
        );

        // Set up the normal matrix as well
        normalMatrix.reset();
        double x = scale.x * animScale.x;
        double y = scale.y * animScale.y;
        double z = scale.z * animScale.z;
        double c = Math.cbrt(x * y * z);
        normalMatrix.scale(
                c == 0 && x == 0 ? 1 : c / x,
                c == 0 && y == 0 ? 1 : c / y,
                c == 0 && z == 0 ? 1 : c / z
        );

        // Perform rotation of normals
        if (partType == PartType.MESH) {
            normalMatrix.rotateZ(rotation.z + offsetRot.z + animRot.z);
            normalMatrix.rotateY(rotation.y + offsetRot.y + animRot.y);
            normalMatrix.rotateX(rotation.x + offsetRot.x + animRot.x);
        } else {
            normalMatrix.rotateZYX(
                    rotation.x + offsetRot.x + animRot.x,
                    rotation.y + offsetRot.y + animRot.y,
                    rotation.z + offsetRot.z + animRot.z
            );
        }

        needsMatrixRecalculation = false;
    }

    public void setPos(FiguraVec3 pos) {
        setPos(pos.x, pos.y, pos.z);
    }
    public void setPos(double x, double y, double z) {
        position.set(x, y, z);
        needsMatrixRecalculation = true;
    }
    public FiguraVec3 getPos() {
        return position.copy();
    }

    public void setRot(FiguraVec3 rot) {
        setRot(rot.x, rot.y, rot.z);
    }
    public void setRot(double x, double y, double z) {
        rotation.set(x, y, z);
        needsMatrixRecalculation = true;
    }
    public FiguraVec3 getRot() {
        return rotation.copy();
    }

    public void setScale(FiguraVec3 scale) {
        setScale(scale.x, scale.y, scale.z);
    }
    public void setScale(double x, double y, double z) {
        scale.set(x, y, z);
        needsMatrixRecalculation = true;
    }
    public FiguraVec3 getScale() {
        return scale.copy();
    }

    public void setPivot(FiguraVec3 pivot) {
        setPivot(pivot.x, pivot.y, pivot.z);
    }
    public void setPivot(double x, double y, double z) {
        pivot.set(x, y, z);
        needsMatrixRecalculation = true;
    }
    public FiguraVec3 getPivot() {
        return pivot.copy();
    }

    public void offsetPivot(FiguraVec3 pivot) {
        offsetPivot(pivot.x, pivot.y, pivot.z);
    }
    public void offsetPivot(double x, double y, double z) {
        offsetPivot.set(x, y, z);
        needsMatrixRecalculation = true;
    }
    public void addOffsetPivot(FiguraVec3 pivot) {
        addOffsetPivot(pivot.x, pivot.y, pivot.z);
    }
    public void addOffsetPivot(double x, double y, double z) {
        offsetPivot.add(x, y, z);
        needsMatrixRecalculation = true;
    }
    public FiguraVec3 getOffsetPivot() {
        return offsetPivot.copy();
    }

    public void offsetPos(FiguraVec3 pos) {
        offsetPos(pos.x, pos.y, pos.z);
    }
    public void offsetPos(double x, double y, double z) {
        offsetPos.set(x, y, z);
        needsMatrixRecalculation = true;
    }
    public void addOffsetPos(FiguraVec3 pos) {
        addOffsetPos(pos.x, pos.y, pos.z);
    }
    public void addOffsetPos(double x, double y, double z) {
        offsetPos.add(x, y, z);
        needsMatrixRecalculation = true;
    }
    public FiguraVec3 getOffsetPos() {
        return offsetPos.copy();
    }

    public void offsetRot(FiguraVec3 rot) {
        offsetRot(rot.x, rot.y, rot.z);
    }
    public void offsetRot(double x, double y, double z) {
        offsetRot.set(x, y, z);
        needsMatrixRecalculation = true;
    }
    public void addOffsetRot(FiguraVec3 rot) {
        addOffsetRot(rot.x, rot.y, rot.z);
    }
    public void addOffsetRot(double x, double y, double z) {
        offsetRot.add(x, y, z);
        needsMatrixRecalculation = true;
    }
    public FiguraVec3 getOffsetRot() {
        return offsetRot.copy();
    }

    public void offsetScale(FiguraVec3 scale) {
        offsetScale(scale.x, scale.y, scale.z);
    }
    public void offsetScale(double x, double y, double z) {
        offsetScale.set(x, y, z);
        needsMatrixRecalculation = true;
    }
    public void addOffsetScale(FiguraVec3 scale) {
        addOffsetScale(scale.x, scale.y, scale.z);
    }
    public void addOffsetScale(double x, double y, double z) {
        offsetScale.add(x, y, z);
        needsMatrixRecalculation = true;
    }
    public FiguraVec3 getOffsetScale() {
        return offsetScale.copy();
    }

    public void setAnimPos(double x, double y, double z) {
        animPos.set(x, y, z);
        needsMatrixRecalculation = true;
    }
    public FiguraVec3 getAnimPos() {
        return animPos.copy();
    }
    public void setAnimRot(double x, double y, double z) {
        animRot.set(x, y, z);
        needsMatrixRecalculation = true;
    }
    public FiguraVec3 getAnimRot() {
        return animRot.copy();
    }
    public void setAnimScale(double x, double y, double z) {
        animScale.set(x, y, z);
        needsMatrixRecalculation = true;
    }
    public FiguraVec3 getAnimScale() {
        return animScale.copy();
    }

    public void setMatrix(FiguraMat4 matrix) {
        positionMatrix.set(matrix);
        FiguraMat3 temp = matrix.deaugmented();
        temp.invert();
        temp.transpose();
        normalMatrix.set(temp);
        needsMatrixRecalculation = false;
    }

    public void setPositionMatrix(FiguraMat4 matrix) {
        positionMatrix.set(matrix);
    }

    public void setNormalMatrix(FiguraMat3 matrix) {
        normalMatrix.set(matrix);
    }

    public FiguraMat4 getPositionMatrix() {
        FiguraMat4 result = FiguraMat4.of();
        result.set(positionMatrix);
        return result;
    }
    public FiguraMat3 getNormalMatrix() {
        FiguraMat3 result = new FiguraMat3();
        result.set(normalMatrix);
        return result;
    }

    // -- Render type thingies --// 

    public void setPrimaryRenderType(RenderTypes type) {
        primaryRenderType = type;
    }
    public RenderTypes getPrimaryRenderType() {
        return primaryRenderType;
    }
    public void setSecondaryRenderType(RenderTypes type) {
        secondaryRenderType = type;
    }
    public RenderTypes getSecondaryRenderType() {
        return secondaryRenderType;
    }

    public void copyTo(PartCustomization target) {
        target.partType = partType;
        target.positionMatrix.set(positionMatrix);
        target.uvMatrix.set(uvMatrix);
        target.normalMatrix.set(normalMatrix);
        target.setPos(position);
        target.setRot(rotation);
        target.setScale(scale);
        target.setPivot(pivot);
        target.offsetPivot(offsetPivot);
        target.offsetPos(offsetPos);
        target.offsetRot(offsetRot);
        target.offsetScale(offsetScale);
        target.stackScale.set(stackScale);
        target.color.set(color);
        target.color2.set(color2);
        target.alpha = alpha;
        target.light = light;
        target.overlay = overlay;
        target.needsMatrixRecalculation = needsMatrixRecalculation;
        target.visible = visible;
        target.vanillaVisible = vanillaVisible;
        target.setPrimaryRenderType(primaryRenderType);
        target.setSecondaryRenderType(secondaryRenderType);
        target.primaryTexture = primaryTexture;
        target.secondaryTexture = secondaryTexture;
    }

    // Modify this object using the information contained in the other object
    public void modify(PartCustomization other) {
        positionMatrix.rightMultiply(other.positionMatrix);
        uvMatrix.rightMultiply(other.uvMatrix);
        normalMatrix.rightMultiply(other.normalMatrix);

        if (other.primaryRenderType != null)
            setPrimaryRenderType(other.primaryRenderType);
        if (other.secondaryRenderType != null)
            setSecondaryRenderType(other.secondaryRenderType);

        if (other.visible != null)
            visible = other.visible;

        if (other.vanillaVisible != null)
            vanillaVisible = other.vanillaVisible;

        if (other.light != null)
            light = other.light;

        if (other.overlay != null)
            overlay = other.overlay;

        if (other.alpha != null) {
            if (alpha != null)
                alpha *= other.alpha;
            else
                alpha = other.alpha;
        }

        stackScale.multiply(other.stackScale);
        color.multiply(other.color);
        color2.multiply(other.color2);

        if (other.primaryTexture != null)
            primaryTexture = other.primaryTexture;
        if (other.secondaryTexture != null)
            secondaryTexture = other.secondaryTexture;

        needsMatrixRecalculation = false;
    }


    public static FloatBuffer posBuf = BufferUtils.createFloatBuffer(16);
    public Matrix4f copyIntoGlobalPoseStack() {
        recalculate();
        positionMatrix.toMatrix4f().store(posBuf);
        GlStateManager.multMatrix(posBuf);
        //positionMatrix.copyDataTo(GLOBAL_CUSTOMIZATION_POSE_STACK.position);
        //normalMatrix.copyDataTo(GLOBAL_CUSTOMIZATION_POSE_STACK.normal);
        GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, posBuf);
        Matrix4f transformedPos = new Matrix4f();
        transformedPos.load(posBuf);
        return transformedPos;
    }

    public static class RenderContainer {
        public Matrix4f position = new Matrix4f();
        public Matrix3f normal = new Matrix3f();

        public void scale(float x, float y, float z) {
            Matrix4f.mul(position, createScaleMatrix4f(x, y, z), position);
            if (x == y && y == z) {
                if (x > 0.0f) {
                    return;
                }
                normal.negate();
            }
            float f = 1.0f / x;
            float g = 1.0f / y;
            float h = 1.0f / z;
            float i = MathUtils.fastInvCubeRoot(f * g * h);
            Matrix3f.mul(normal, createScaleMatrix3f(i * f, i * g, i * h), normal);
        }

        private static Matrix4f createScaleMatrix4f(float f, float g, float h) {
            Matrix4f matrix4f = new Matrix4f();
            matrix4f.m00 = f;
            matrix4f.m11 = g;
            matrix4f.m22 = h;
            matrix4f.m33 = 1.0f;
            return matrix4f;
        }

        private static Matrix3f createScaleMatrix3f(float m00, float m11, float m22) {
            Matrix3f matrix3f = new Matrix3f();
            matrix3f.m00 = m00;
            matrix3f.m11 = m11;
            matrix3f.m22 = m22;
            return matrix3f;
        }
    }

    public enum PartType {
        MESH,
        CUBE,
        GROUP
    }

    public static class PartCustomizationStack {

        private final Stack<PartCustomization> stack = new Stack<PartCustomization>() {{
            add(new PartCustomization());
        }};

        public void push(PartCustomization customization) {
            // copy stack
            PartCustomization newCustomization = new PartCustomization();
            stack.peek().copyTo(newCustomization);

            // modify
            newCustomization.modify(customization);

            // add
            stack.push(newCustomization);
        }

        public void pop() {
            stack.pop();
        }

        public PartCustomization peek() {
            return stack.peek();
        }

        public boolean isEmpty() {
            return stack.size() == 1;
        }
    }
}
