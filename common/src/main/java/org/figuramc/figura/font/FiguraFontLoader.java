package org.figuramc.figura.font;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.figuramc.figura.FiguraMod;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

public class FiguraFontLoader implements IResourceManagerReloadListener {
    static final Gson GSON = new Gson();

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        for (String namespace : resourceManager.getResourceDomains()) {
            try {
                resourceManager.getAllResources(new ResourceLocation(namespace, "font")).forEach(iResource -> {
                    ResourceLocation locationForResource = iResource.getResourceLocation();
                    if (locationForResource.getResourcePath().endsWith(".json")) {
                        Reader reader = new InputStreamReader(iResource.getInputStream());
                        Font font = GSON.fromJson(reader, Font.class);
                        if (font != null && !font.providers.isEmpty()) {
                            for (BitMapProvider provider : font.providers) {
                                if (Objects.equals(provider.type, "bitmap")) { // If other font types are supported, we should abstract this
                                   try {
                                       List<int[]> codepoints = Lists.newArrayList();
                                       for (String codepointString : provider.chars) {
                                           int[] currentList = codepointString.codePoints().toArray();
                                           if (!codepoints.isEmpty() && (currentList.length != codepoints.get(0).length)) throw new JsonParseException("The number of chars in each row must the same, pad with space or \\u0000. Found: " + currentList.length + ", Expected: " + codepoints.get(0).length);
                                           codepoints.add(currentList);
                                       }
                                       if (codepoints.isEmpty() || codepoints.get(0).length == 0) {
                                           throw new JsonParseException("There were no chars defined.");
                                       }

                                       // This is where we actually load the texture and create a font object
                                       ResourceLocation texturePath = new ResourceLocation(provider.file);
                                       texturePath = new ResourceLocation(texturePath.getResourceDomain(), "textures/"+texturePath.getResourcePath());

                                       IResource texture = resourceManager.getResource(texturePath);
                                       if (texture instanceof IResource) {
                                           InputStream textureData = texture.getInputStream();
                                           if (textureData != null) {
                                               BufferedImage image = TextureUtil.readBufferedImage(textureData);
                                               int width = image.getWidth();
                                               int height = image.getHeight();
                                               int glyphWidth = width / codepoints.get(0).length;
                                               int glyphHeight = height / codepoints.size();
                                               float glyphScale = (float) glyphHeight / provider.height;

                                             //  SimpleTexture simpleTexture = new SimpleTexture(texturePath);
                                              // simpleTexture.setBlurMipmap(false, false);
                                               //Minecraft.getMinecraft().getTextureManager().loadTexture(texturePath, simpleTexture); // register the texture


                                               Int2ObjectOpenHashMap<FiguraGlyph> figuraGlyphMap = new Int2ObjectOpenHashMap<>();
                                               // TODO : map from the image coords to the actual glyphs and construct the font
                                               for (int rowIndex = 0; rowIndex < codepoints.size(); rowIndex++) {
                                                   int[] codepointList = codepoints.get(rowIndex);
                                                   for (int columnIndex = 0; columnIndex < codepointList.length; columnIndex++) {
                                                       int codePoint = codepointList[columnIndex];

                                                       if (codePoint == 0 || codePoint == 32) {
                                                           columnIndex++;
                                                           continue; // Skip empty characters or space
                                                       }

                                                       // Emoji advance is hardcoded to be 8.
                                                       int advance = isFiguraFont(locationForResource) ? 8 : findGlyphWidth(image, glyphWidth, glyphHeight, columnIndex, rowIndex);
                                                       FiguraGlyph glyph = new FiguraGlyph(glyphScale, texturePath, glyphWidth * columnIndex, glyphHeight * rowIndex, glyphWidth, glyphHeight, advance, provider.ascent);
                                                       if (figuraGlyphMap.put(codePoint, glyph) != null)  {
                                                           FiguraMod.LOGGER.warn("Duplicate codepoint '{}' in {}", Integer.toHexString(codePoint), provider.file);
                                                           continue;
                                                       }
                                                       if (isFiguraFont(locationForResource)) {
                                                           Emojis.getCategoryByFont(locationForResource);
                                                       }
                                                   }
                                               }

                                           }
                                           IOUtils.closeQuietly(texture);
                                       }



                                   } catch (Exception exception) {
                                       FiguraMod.LOGGER.warn("Found a problem while loading {} : {}", locationForResource.toString(), exception.getMessage());
                                   }
                                } else {
                                    FiguraMod.LOGGER.warn("Ignoring provider of type {}, only bitmap fonts are currently supported! {}", provider.type, iResource.getResourceLocation().toString());
                                }
                            }
                        } else {
                            FiguraMod.LOGGER.warn("Unable to load a font in namespace {}, as font was null or had no providers", namespace);
                        }
                    }
                });
            } catch (IOException e) {
                FiguraMod.LOGGER.warn("Unable to load a font in namespace {}, font {}", namespace, e.getMessage());
            }
        }
    }

    private static boolean isFiguraFont(ResourceLocation location) {
        return location.getResourceDomain().equals("figura");
    }

    private static int findGlyphWidth(BufferedImage image, int charWidth, int charHeight, int column, int row) {
        int actualWidth;
        for (actualWidth = charWidth - 1; actualWidth >= 0; --actualWidth) {
            int tileX = column * charWidth + actualWidth;
            for (int k = 0; k < charHeight; ++k) {
                int tileY = row * charHeight + k;
                // Check the alpha component
                if (((image.getRGB(tileX, tileY) >> 24) & 0xFF) != 0) continue;
                return actualWidth + 1;
            }
        }
        return actualWidth + 1;
    }


    static final class Font {
        public List<BitMapProvider> providers;

        public Font() {
             providers = Collections.emptyList(); // just in case it's empty
        }
    }

    // If we ever support other font types such as ttf we'd have to abstract this away. Provider -> (TTFProvider, BitMapProvider)
    // Having no records doesn't mean we can't get them to do our work for us
    static final class BitMapProvider {
        public String type;
        public String file;
        public Integer ascent;
        public List<String> chars;
        public Integer height;

        // Default values
        public BitMapProvider() {
            this.height = 8;
            this.ascent = 8;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            BitMapProvider that = (BitMapProvider) obj;
            return Objects.equals(this.type, that.type) &&
                    Objects.equals(this.file, that.file) &&
                    Objects.equals(this.ascent, that.ascent) &&
                    Objects.equals(this.height, that.height) &&
                    Objects.equals(this.chars, that.chars);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, file, ascent, chars, height);
        }

        @Override
        public String toString() {
            return "BitMapProvider[" +
                    "type=" + type + ", " +
                    "file=" + file + ", " +
                    "ascent=" + ascent + ", " +
                    "chars=" + chars + ", " +
                    "height=" + height + ']';
        }
    }
}
