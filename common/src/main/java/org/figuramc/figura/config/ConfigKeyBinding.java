package org.figuramc.figura.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.figuramc.figura.FiguraMod;

public class ConfigKeyBinding extends KeyBinding {

    private final ConfigType.KeybindConfig config;

    public ConfigKeyBinding(String translationKey, int key, ConfigType.KeybindConfig config) {
        super(translationKey, key, FiguraMod.MOD_ID);
        this.config = config;

        if (FiguraMod.debugModeEnabled() || !config.disabled)
           addKeyBind(this);
    }

    @Override
    public void setKeyCode(int boundKey) {
        super.setKeyCode(boundKey);

        config.value = config.tempValue = this.getKeyDescription();
        ConfigManager.saveConfig();
    }

    // Moved from setKey as it caused issues on Forge because the vanilla config overriden before it was loaded properly
    public void saveConfigChanges() {
        GameSettings options = Minecraft.getMinecraft().gameSettings;
        if (options != null) options.saveOptions();

        KeyBinding.resetKeyBindingArrayAndHash();
    }

    @ExpectPlatform
    // TODO : add a service for keybinds
    public static void addKeyBind(ConfigKeyBinding keyBind) {}
}