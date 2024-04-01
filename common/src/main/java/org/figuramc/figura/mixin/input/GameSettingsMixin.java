package org.figuramc.figura.mixin.input;

import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.figuramc.figura.FiguraMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameSettings.class)
public class GameSettingsMixin {
    // Moved from KeyMappingMixin -> KeyBindingMixin -> GameSettingsMixin
    @Inject(method = "isKeyDown", at = @At("HEAD"), cancellable = true)
    private static void matches(KeyBinding key, CallbackInfoReturnable<Boolean> cir) {
        if (FiguraMod.processingKeybind)
            cir.setReturnValue(false);
    }
}
