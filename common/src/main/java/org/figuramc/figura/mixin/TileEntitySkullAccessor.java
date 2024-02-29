package org.figuramc.figura.mixin;

import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.tileentity.TileEntitySkull;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TileEntitySkull.class)
public interface TileEntitySkullAccessor {
    @Intrinsic
    @Accessor("profileCache")
    static PlayerProfileCache getProfileCache() {
        throw new AssertionError();
    }
}
