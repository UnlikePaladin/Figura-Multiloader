package org.figuramc.figura.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public class FileTexture extends DynamicTexture {

    private final ResourceLocation id;

    private FileTexture(BufferedImage image, ResourceLocation id) {
        super(image);
        this.id = id;

        Minecraft.getMinecraft().getTextureManager().loadTexture(id, this);
    }

    public static FileTexture of(Path path) throws IOException {
        String s = path.toString();
        ResourceLocation resourceLocation = new FiguraIdentifier("file/" + FiguraIdentifier.formatPath(s));
        return new FileTexture(readImage(path), resourceLocation);
    }

    public static BufferedImage readImage(Path path) throws IOException {
        byte[] bytes = IOUtils.readFileBytes(path);
        //TODO : Could cause issues, test well and if not reverse this array
        InputStream is = new ByteArrayInputStream(bytes);
        return ImageIO.read(is);
    }

    public ResourceLocation getLocation() {
        return id;
    }
}
