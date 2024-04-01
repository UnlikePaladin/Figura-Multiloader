package org.figuramc.figura.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.ducks.TextureMapAccessor;
import org.figuramc.figura.model.rendering.texture.FiguraTexture;
import org.figuramc.figura.model.rendering.texture.FiguraTextureSet;
import org.figuramc.figura.utils.RenderUtils;
import org.luaj.vm2.LuaError;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.IntBuffer;
import java.util.Optional;

public class TextureCustomization {

    private final FiguraTextureSet.OverrideType first;
    private final Object second;

    public TextureCustomization(FiguraTextureSet.OverrideType first, Object second) {
        this.first = first;
        this.second = second;
    }

    public FiguraTextureSet.OverrideType getOverrideType() {
        return first;
    }

    public Object getValue() {
        return second;
    }

    public FiguraTexture getTexture(Avatar avatar, FiguraTextureSet textureSet) {
        if (avatar.render == null) return null;

        ResourceLocation resourceLocation = textureSet.getOverrideTexture(avatar.owner, this);
        String name = resourceLocation.toString();
        if (avatar.renderer.customTextures.containsKey(name)) {
            return avatar.renderer.customTextures.get(name);
        }

        // is there a way to check if an atlas exists without getAtlas? cause that is the only thing that will cause an error, and try catch blocks can be pricy
        try {
            TextureMap atlas = Minecraft.getMinecraft().getTextureMapBlocks();
            Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            TextureMapAccessor atlasAccessor = (TextureMapAccessor) atlas;
            int width = atlasAccessor.getWidth();
            int height = atlasAccessor.getHeight();
            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            IntBuffer buffer;
            int[] pixelValues;
            int k = width * height;
            buffer = BufferUtils.createIntBuffer(k);
            pixelValues = new int[k];
            GlStateManager.glReadPixels(0, 0, width, height, 32993, 33639, buffer);
            buffer.get(pixelValues);
            TextureUtil.processPixelValues(pixelValues, width, height);
            bufferedImage.setRGB(0, 0, width, height, pixelValues, 0, width);
            return avatar.registerTexture(name, bufferedImage, false);
        } catch (Exception ignored) {}
        try {
            Optional<IResource> resource = Optional.of(Minecraft.getMinecraft().getResourceManager().getResource(resourceLocation));
            // if the string is a valid resourceLocation but does not point to a valid resource, missingno

            BufferedImage image;
            if (resource.isPresent() && resource != null) {
                image = ImageIO.read(resource.get().getInputStream());
            } else {
                image = RenderUtils.getMissingTexture();
            }
            return avatar.registerTexture(name, image, false);
        } catch (Exception e) {
            // spit an error if the player inputs a resource location that does point to a thing, but not to an image
            throw new LuaError(e.getMessage());
        }
    }
}
