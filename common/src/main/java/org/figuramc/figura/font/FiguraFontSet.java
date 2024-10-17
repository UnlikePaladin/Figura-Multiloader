package org.figuramc.figura.font;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;

import java.util.HashSet;
import java.util.List;

public class FiguraFontSet implements AutoCloseable {
    private final TextureManager textureManager;
    private final ResourceLocation name;
    private final List<FontTexture> textures = Lists.newArrayList();
    private final List<FiguraGlyphProvider> providers = Lists.newArrayList();

    public FiguraFontSet(TextureManager textureManager, ResourceLocation name) {
        this.textureManager = textureManager;
        this.name = name;
    }

    @Override
    public void close() {
        this.closeProviders();
        this.closeTextures();
    }

    private void closeProviders() {
        for (FiguraGlyphProvider glyphProvider : this.providers) {
            glyphProvider.close();
        }
        this.providers.clear();
    }

    private void closeTextures() {
        for (FontTexture fontTexture : this.textures) {
            fontTexture.close();
        }
        this.textures.clear();
    }

    public void reload(List<FiguraGlyphProvider> glyphProviders) {
        this.closeProviders();
        this.closeTextures();
        this.glyphs.clear();
        this.glyphInfos.clear();
        this.glyphsByWidth.clear();
        this.missingGlyph = this.stitch(MissingGlyph.INSTANCE);
        this.whiteGlyph = this.stitch(WhiteGlyph.INSTANCE);
        IntOpenHashSet intSet = new IntOpenHashSet();
        for (GlyphProvider glyphProvider : glyphProviders) {
            intSet.addAll(glyphProvider.getSupportedGlyphs());
        }
        HashSet set = Sets.newHashSet();
        intSet.forEach(i2 -> {
            for (GlyphProvider glyphProvider : glyphProviders) {
                GlyphInfo glyphInfo = i2 == 32 ? SPACE_INFO : glyphProvider.getGlyph(i2);
                if (glyphInfo == null) continue;
                set.add(glyphProvider);
                if (glyphInfo == MissingGlyph.INSTANCE) break;
                this.glyphsByWidth.computeIfAbsent(Mth.ceil(glyphInfo.getAdvance(false)), i -> new IntArrayList()).add(i2);
                break;
            }
        });
        glyphProviders.stream().filter(set::contains).forEach(this.providers::add);
    }



}
