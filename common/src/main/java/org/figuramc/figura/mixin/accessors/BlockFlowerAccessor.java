package org.figuramc.figura.mixin.accessors;

import net.minecraft.block.BlockFlower;
import net.minecraft.block.properties.PropertyEnum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockFlower.class)
public interface BlockFlowerAccessor {
    @Accessor("type")
    static PropertyEnum<BlockFlower.EnumFlowerType> getType() {
        throw new AssertionError();
    }
}
