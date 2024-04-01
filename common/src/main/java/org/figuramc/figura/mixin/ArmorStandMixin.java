package org.figuramc.figura.mixin;

import net.minecraft.entity.item.EntityArmorStand;
import org.figuramc.figura.ducks.ArmorStandAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityArmorStand.class)
public abstract class ArmorStandMixin implements ArmorStandAccessor {
    @Shadow protected abstract void setMarker(boolean bl);

    @Override
    public void figura$setMarker(boolean isMarker) {
        setMarker(isMarker);
    }
}
