package org.figuramc.figura.mixin.gui;

import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Badges;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.ducks.extensions.StyleExtension;
import org.figuramc.figura.utils.ColorUtils;
import org.figuramc.figura.utils.ui.UIHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.*;

@Mixin(GuiMainMenu.class)
public class SplashManagerMixin {

    @Shadow @Final private static Random RANDOM;

    @Unique
    private static final List<ITextComponent> FIGURA_SPLASHES = Collections.singletonList(
            new TextComponentString("Also try ears ")
                    .appendSibling(new TextComponentString("\uD83D\uDC3E").setStyle(((StyleExtension)new Style()).setFont(UIHelper.SPECIAL_FONT).setColor(TextFormatting.WHITE)))
                    .appendText("!")
    );

    @Inject(at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;"), method = "<init>", locals = LocalCapture.CAPTURE_FAILHARD)
    private void init(CallbackInfo ci, IResource e, List<String> list) {
        FiguraMod.splashText = null;
        if (!Configs.EASTER_EGGS.value)
            return;

        Calendar calendar = FiguraMod.CALENDAR;
        calendar.setTime(new Date());

        String who = null;
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        switch (calendar.get(Calendar.MONTH)) {
            case Calendar.MARCH:
                if (day == 24) who = FiguraMod.MOD_NAME;
                break;
            case Calendar.JULY:
                if (day == 4) who = "Skylar";
                break;
        }

        if (who != null) {
            FiguraMod.splashText = new TextComponentString("Happy birthday " + who + " ")
                    .appendSibling(Badges.System.DEFAULT.badge.createCopy().setStyle(((StyleExtension)((StyleExtension)new Style()).setFont(Badges.FONT)).setRGBColor(ColorUtils.Colors.DEFAULT.hex)))
                    .appendText("!");
        } else {
            int size = list.size();
            int random = RANDOM.nextInt(size + FIGURA_SPLASHES.size());
            if (random >= size)
                FiguraMod.splashText = FIGURA_SPLASHES.get(random - size);
        }
    }
}
