package org.figuramc.figura.ducks.extensions;

import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Quaternion;

public interface Vector3fExtension {
    void figura$transform(Matrix3f matrix);

    void figura$transform(Quaternion rotation);
    Quaternion figura$rotationDegrees(float f);

    Quaternion figura$rotation(float f);
}
