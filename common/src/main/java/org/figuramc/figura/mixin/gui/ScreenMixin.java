package org.figuramc.figura.mixin.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.event.ClickEvent;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.utils.TextUtils;
import org.luaj.vm2.LuaValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiScreen.class)
public class ScreenMixin {

    @Inject(at = @At("HEAD"), method = "handleComponentClick", cancellable = true)
    private void handleComponentClicked(ITextComponent component, CallbackInfoReturnable<Boolean> cir) {
        if (component == null)
            return;

        ClickEvent event = component.getStyle().getClickEvent();
        if (event == null)
            return;

        if (event instanceof TextUtils.FiguraClickEvent) {
            TextUtils.FiguraClickEvent figuraEvent = (TextUtils.FiguraClickEvent) event;
            figuraEvent.onClick.run();
            cir.setReturnValue(true);
        } else if (event.getAction() == ClickEvent.Action.getValueByCanonicalName("figura_function")) {
            cir.setReturnValue(true);

            Avatar avatar = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());
            if (avatar == null)
                return;

            LuaValue value = avatar.loadScript("figura_function", event.getValue());
            if (value != null)
                avatar.run(value, avatar.tick);
        }
    }
}
