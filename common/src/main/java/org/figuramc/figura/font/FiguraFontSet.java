package org.figuramc.figura.font;

import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;

public class FiguraFontSet implements AutoCloseable {
    private final TextureManager textureManager;
    private final ResourceLocation name;

    public FiguraFontSet(TextureManager textureManager, ResourceLocation name) {
        this.textureManager = textureManager;
        this.name = name;
    }

    @Override
    public void close() throws Exception {

    }



}
