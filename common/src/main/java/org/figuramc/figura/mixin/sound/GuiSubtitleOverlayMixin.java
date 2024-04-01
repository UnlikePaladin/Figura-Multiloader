package org.figuramc.figura.mixin.sound;

import net.minecraft.client.gui.GuiSubtitleOverlay;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.math.Vec3d;
import org.figuramc.figura.ducks.SubtitleOverlayAccessor;
import org.figuramc.figura.lua.api.sound.LuaSound;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(GuiSubtitleOverlay.class)
public class GuiSubtitleOverlayMixin implements SubtitleOverlayAccessor {

    @Shadow @Final private List<GuiSubtitleOverlay.Subtitle> subtitles;

    @Unique
    @Override
    public void figura$PlaySound(LuaSound sound) {
        ITextComponent text = sound.getSubtitleText();
        if (text == null)
            return;

        Vec3d pos = sound.getPos().asVec3();

        for (GuiSubtitleOverlay.Subtitle subtitle : this.subtitles) {
            if (subtitle.getString().equals(text.getFormattedText())) {
                subtitle.refresh(pos);
                return;
            }
        }
        GuiSubtitleOverlay.Subtitle subtitle = ((GuiSubtitleOverlay)(Object)this).new Subtitle(text.getFormattedText(), pos);
        this.subtitles.add(subtitle);
    }
}
