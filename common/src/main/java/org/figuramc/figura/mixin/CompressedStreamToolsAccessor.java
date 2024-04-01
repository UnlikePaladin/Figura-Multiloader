package org.figuramc.figura.mixin;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.io.DataOutput;

@Mixin(CompressedStreamTools.class)
public interface CompressedStreamToolsAccessor {

    @Invoker("writeTag")
    static void figura$invokeWriteUnnamedTag(NBTBase compound, DataOutput output) {
        throw new AssertionError();
    }
}
