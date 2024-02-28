package org.figuramc.figura.mixin;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(WorldClient.class)
public interface ClientWorldInvoker {
    @Intrinsic
    @Accessor("entityList")
    Set<Entity> getEntityList();

}
