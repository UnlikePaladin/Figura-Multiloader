package org.figuramc.figura.mixin;

import net.minecraft.command.CommandReplaceItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(CommandReplaceItem.class)
public interface CommandReplaceItemAccessor {

    @Final
    @Accessor("SHORTCUTS")
    static Map<String, Integer> figura$getShortcuts() {
        throw new AssertionError();
    }
}
