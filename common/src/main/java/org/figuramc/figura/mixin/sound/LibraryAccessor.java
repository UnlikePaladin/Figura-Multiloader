package org.figuramc.figura.mixin.sound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import paulscode.sound.Channel;
import paulscode.sound.Library;

import java.util.List;

@Mixin(Library.class)
public interface LibraryAccessor {
    @Accessor("streamingChannels")
    List<Channel> figura$getStreamingChannels();

    @Accessor("normalChannels")
    List<Channel> figura$getNormalChannels();
}
