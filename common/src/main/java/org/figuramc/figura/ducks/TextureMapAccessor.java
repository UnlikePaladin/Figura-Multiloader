package org.figuramc.figura.ducks;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;

import java.util.Map;

public interface TextureMapAccessor {
    Map<String, TextureAtlasSprite> getTexturesByName();

    int getWidth();

    int getHeight();

    ResourceLocation invokeGetResourceLocation(TextureAtlasSprite textureAtlasSprite);

    String getAtlasName();
}

