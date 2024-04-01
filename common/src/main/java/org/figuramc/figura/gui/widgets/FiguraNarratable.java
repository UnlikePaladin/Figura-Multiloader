package org.figuramc.figura.gui.widgets;

import com.mojang.text2speech.Narrator;
import net.minecraft.util.text.ITextComponent;

public interface FiguraNarratable {
    Narrator NARRATOR_INSTANCE = Narrator.getNarrator();

    void narrate();

    ITextComponent getNarrationMessage();
    void queueNarration(long time);
}
