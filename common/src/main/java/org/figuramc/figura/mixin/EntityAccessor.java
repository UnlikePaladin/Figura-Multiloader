package org.figuramc.figura.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityAccessor {

    @Intrinsic
    @Accessor
    World getWorld();

    @Intrinsic
    @Invoker("getPermissionLevel")
    int getPermissionLevel();
}
