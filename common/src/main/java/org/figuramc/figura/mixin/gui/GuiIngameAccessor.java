package org.figuramc.figura.mixin.gui;

import net.minecraft.client.gui.GuiIngame;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiIngame.class)
public interface GuiIngameAccessor {
    @Intrinsic
    @Accessor("displayedTitle")
    String getTitle();

    @Intrinsic
    @Accessor("displayedSubTitle")
    String getSubtitle();

    @Intrinsic
    @Accessor("overlayMessage")
    String getActionbar();

    @Intrinsic
    @Accessor("titlesTimer")
    int getTime();

    @Intrinsic
    @Accessor("overlayMessageTime")
    int getActionbarTime();

    @Intrinsic
    @Accessor("titleFadeIn")
    void setTitleFadeInTime(int time);

    @Intrinsic
    @Accessor("titleDisplayTime")
    void setTitleStayTime(int time);

    @Intrinsic
    @Accessor("titleFadeOut")
    void setTitleFadeOutTime(int time);

    @Intrinsic
    @Accessor("titlesTimer")
    void setTitleTime(int time);

    @Intrinsic
    @Accessor("titleFadeIn")
    int getTitleFadeInTime();

    @Intrinsic
    @Accessor("titleDisplayTime")
    int getTitleStayTime();

    @Intrinsic
    @Accessor("titleFadeOut")
    int getTitleFadeOutTime();

    @Intrinsic
    @Accessor("titlesTimer")
    int getTitleTime();

    @Intrinsic
    @Accessor("displayedTitle")
    void setTitle(String title);

    @Intrinsic
    @Accessor("displayedSubTitle")
    void setSubtitle(String subtitle);
}
