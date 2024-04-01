package org.figuramc.figura.mixin.render.renderers;

import net.minecraft.client.renderer.tileentity.TileEntitySignRenderer;
import net.minecraft.util.text.TextComponentString;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.font.Emojis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(TileEntitySignRenderer.class)
public class TileEntitySignRendererMixin {

    // method_3583 corresponds to fabric intermediary, lambda$render$0 is the unmapped OF name, func_243502_a is the SRG name for Forge
    @ModifyArg(method = {"render(Lnet/minecraft/tileentity/TileEntitySign;DDDFIF)V"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I"))
    private static String modifyText(String string) {
        return Configs.EMOJIS.value > 0 ? Emojis.applyEmojis(new TextComponentString(string)).getFormattedText() : string;
    }
}
