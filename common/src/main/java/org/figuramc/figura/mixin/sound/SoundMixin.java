package org.figuramc.figura.mixin.sound;

import net.minecraft.client.audio.Sound;
import org.figuramc.figura.ducks.SoundAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

@Mixin(Sound.class)
public class SoundMixin implements SoundAccessor {

    @Unique private UUID figura$owner;
    @Unique private String figura$name;

    @Override
    public UUID getFigura$owner() {
        return this.figura$owner;
    }

    @Override
    public void setFigura$owner(UUID uuid) {
        this.figura$owner = uuid;
    }

    @Override
    public String getFigura$name() {
        return figura$name;
    }

    @Override
    public void setFigura$name(String figura$name) {
        this.figura$name = figura$name;
    }
}
