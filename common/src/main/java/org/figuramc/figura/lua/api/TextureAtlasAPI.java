package org.figuramc.figura.lua.api;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import org.figuramc.figura.ducks.TextureMapAccessor;
import org.figuramc.figura.lua.LuaNotNil;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaMethodOverload;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.math.vector.FiguraVec4;
import org.figuramc.figura.utils.LuaUtils;

import java.util.ArrayList;
import java.util.List;

@LuaWhitelist
@LuaTypeDoc(
        name = "TextureAtlas",
        value = "texture_atlas"
)
public class TextureAtlasAPI {

    private final TextureMap atlas;

    public TextureAtlasAPI(TextureMap atlas) {
        this.atlas = atlas;
    }

    @LuaWhitelist
    @LuaMethodDoc("texture_atlas.list_sprites")
    public List<String> listSprites() {
        return new ArrayList<>(((TextureMapAccessor) atlas).getTexturesByName().keySet());
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "path"
            ),
            value = "texture_atlas.get_sprite_uv"
    )
    public FiguraVec4 getSpriteUV(@LuaNotNil String sprite) {
        ResourceLocation spriteLocation = LuaUtils.parsePath(sprite);
        TextureAtlasSprite s = atlas.getAtlasSprite(spriteLocation.toString());
        return FiguraVec4.of(s.getMinU(), s.getMinV(), s.getMaxU(), s.getMaxV());
    }


    @LuaWhitelist
    @LuaMethodDoc("texture_atlas.get_width")
    public int getWidth() {
        return 64;
    }

    @LuaWhitelist
    @LuaMethodDoc("texture_atlas.get_height")
    public int getHeight() {
        return 64;
    }

    @Override
    public String toString() {
        return "TextureAtlas (" + ((TextureMapAccessor)atlas).getAtlasName() + ")";
    }
}
