package org.figuramc.figura.font;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.figuramc.figura.model.rendering.texture.RenderTypes;

import java.io.IOException;

public class FiguraFontTexture extends AbstractTexture {
    private final ResourceLocation name;
    private final RenderTypes.FiguraRenderType normalType;
    private final RenderTypes.FiguraRenderType seeThroughType;
    private final boolean colored;
    private final Node root;

    public FontTexture(ResourceLocation resourceLocation, boolean hasColor) {
        this.name = resourceLocation;
        this.colored = bl;
        this.root = new Node(0, 0, 256, 256);
        TextureUtil.prepareImage(hasColor ? NativeImage.InternalGlFormat.RGBA : NativeImage.InternalGlFormat.INTENSITY, this.getGlTextureId(), 256, 256);
        this.normalType = RenderTypes.FiguraRenderType.TEXT.apply(resourceLocation);
        this.seeThroughType = RenderTypes.FiguraRenderType.TEXT_SEETHROUGH.apply(resourceLocation);
    }

    @Override
    public void loadTexture(IResourceManager resourceManager) throws IOException {

    }
}
