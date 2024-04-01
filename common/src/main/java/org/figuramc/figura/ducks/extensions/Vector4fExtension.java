package org.figuramc.figura.ducks.extensions;

import net.minecraft.client.renderer.Matrix4f;
import org.lwjgl.util.vector.Quaternion;

public interface Vector4fExtension {
    void figura$transform(Matrix4f matrix);

    void figura$transform(Quaternion rotation);

    Quaternion figura$rotationDegrees(float value);

}
