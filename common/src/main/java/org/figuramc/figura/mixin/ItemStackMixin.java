package org.figuramc.figura.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.font.Emojis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin { // TODO : More component checks

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void getHoverName(CallbackInfoReturnable<String> cir) {
        if (Configs.EMOJIS.value > 0)
            cir.setReturnValue(Emojis.applyEmojis(new TextComponentString(cir.getReturnValue())).getUnformattedText());
    }
}
