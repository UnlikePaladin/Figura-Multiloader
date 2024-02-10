package org.figuramc.figura.mixin.math;

import org.figuramc.figura.ducks.extensions.QuaternionExtension;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Quaternion.class)
public class QuaternionMixin implements QuaternionExtension {

}
