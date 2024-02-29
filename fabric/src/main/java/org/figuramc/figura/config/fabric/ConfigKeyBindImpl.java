package org.figuramc.figura.config.fabric;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import org.figuramc.figura.config.ConfigKeyBinding;

public class ConfigKeyBindImpl {
    public static void addKeyBind(ConfigKeyBinding keyBind) {
        KeyBindingHelper.registerKeyBinding(keyBind);
    }
}
