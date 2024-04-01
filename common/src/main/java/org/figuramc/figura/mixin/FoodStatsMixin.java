package org.figuramc.figura.mixin;

import net.minecraft.util.FoodStats;
import org.figuramc.figura.ducks.FoodStatsAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FoodStats.class)
public abstract class FoodStatsMixin implements FoodStatsAccessor {
    @Override
    @Accessor("foodExhaustionLevel")
    public abstract float figura$getExhaustionLevel();
}
