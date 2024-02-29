package org.figuramc.figura.mixin.input;

import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(KeyBinding.class)
public interface KeyBindingAccessor {

    @Intrinsic
    @Accessor("KEYBIND_ARRAY")
    static Map<String, KeyBinding> getAll() {
        throw new AssertionError();
    }
}
