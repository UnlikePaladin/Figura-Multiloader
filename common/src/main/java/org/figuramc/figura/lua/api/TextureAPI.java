package org.figuramc.figura.lua.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.LuaNotNil;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaMethodOverload;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.model.rendering.texture.FiguraTexture;
import org.figuramc.figura.utils.ColorUtils;
import org.figuramc.figura.utils.LuaUtils;
import org.figuramc.figura.utils.RenderUtils;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@LuaWhitelist
@LuaTypeDoc(
        name = "TextureAPI",
        value = "textures"
)
public class TextureAPI {
    public static final int TEXTURE_LIMIT = 128;

    private final Avatar owner;

    public TextureAPI(Avatar owner) {
        this.owner = owner;
    }

    private void check() {
        if (owner.renderer == null)
            throw new LuaError("Avatar have no active renderer!");
    }

    public FiguraTexture register(String name, BufferedImage image, boolean ignoreSize) {
        return owner.registerTexture(name, image, ignoreSize);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = {String.class, Integer.class, Integer.class},
                    argumentNames = {"name", "width", "height"}
            ),
            value = "textures.new_texture")
    public FiguraTexture newTexture(@LuaNotNil String name, int width, int height) {
        BufferedImage image;
        try {
            image = new BufferedImage(width, height, 1);
        } catch (Exception e) {
            throw new LuaError(e.getMessage());
        }

        FiguraTexture texture = register(name, image, false);
        texture.fill(0, 0, width, height, ColorUtils.Colors.AWESOME_BLUE.vec.augmented(1d), null, null, null);
        return texture;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = {String.class, String.class},
                            argumentNames = {"name", "base64Text"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {String.class, LuaTable.class},
                            argumentNames = {"name", "byteArray"}
                    )
            },
            value = "textures.read")
    public FiguraTexture read(@LuaNotNil String name, @LuaNotNil Object object) {
        BufferedImage image;
        byte[] bytes;

        if (object instanceof LuaTable) {
            LuaTable table = (LuaTable) object;
            bytes = new byte[table.length()];
            for(int i = 0; i < bytes.length; i++)
                bytes[i] = (byte) table.get(i + 1).checkint();
        } else if (object instanceof String) {
            String s = (String) object;
            bytes = Base64.getDecoder().decode(s);
        } else {
            throw new LuaError("Invalid type for read \"" + object.getClass().getSimpleName() + "\"");
        }

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            image = TextureUtil.readBufferedImage(bais);
            bais.close();
        } catch (Exception e) {
            throw new LuaError(e.getMessage());
        }

        return register(name, image, false);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = {String.class, FiguraTexture.class},
                    argumentNames = {"name", "texture"}
            ),
            value = "textures.copy")
    public FiguraTexture copy(@LuaNotNil String name, @LuaNotNil FiguraTexture texture) {
        BufferedImage image = texture.copy();
        return register(name, image, false);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "name"
            ),
            value = "textures.get")
    public FiguraTexture get(@LuaNotNil String name) {
        check();
        return owner.renderer.customTextures.get(name);
    }

    @LuaWhitelist
    @LuaMethodDoc("textures.get_textures")
    public List<FiguraTexture> getTextures() {
        check();
        return new ArrayList<>(owner.renderer.textures.values());
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = {String.class, String.class},
                    argumentNames = {"name", "path"}
            ),
            value = "textures.from_vanilla"
    )
    public FiguraTexture fromVanilla(@LuaNotNil String name, @LuaNotNil String path) {
        check();
        ResourceLocation resourcePath = LuaUtils.parsePath(path);
        try {
            BufferedImage image;
            try {
                image = TextureUtil.readBufferedImage(Minecraft.getMinecraft().getResourceManager().getResource(resourcePath).getInputStream());
            } catch (Exception ignored) {
                // if the string is a valid resourceLocation but does not point to a valid resource, missingno
                image = RenderUtils.getMissingTexture();
            }
            return register(name, image, false);
        } catch (Exception e) {
            // spit an error if the player inputs a resource location that does point to a thing, but not to an image
            throw new LuaError(e.getMessage());
        }
    }

    @LuaWhitelist
    public FiguraTexture __index(@LuaNotNil String name) {
        check();
        return owner.renderer.getTexture(name);
    }

    @Override
    public String toString() {
        return "TextureAPI";
    }
}
