package org.figuramc.figura.config.forge;

import org.figuramc.figura.config.ConfigKeyBinding;
import org.figuramc.figura.forge.FiguraModClientForge;

public class ConfigKeyBindImpl {
    public static void addKeyBind(ConfigKeyBinding keyBind) {
        FiguraModClientForge.KEYBINDS.add(keyBind);
    }
}
