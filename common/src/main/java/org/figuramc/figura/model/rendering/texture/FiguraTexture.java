package org.figuramc.figura.model.rendering.texture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.LuaNotNil;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaMethodOverload;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.math.matrix.FiguraMat4;
import org.figuramc.figura.math.vector.FiguraVec2;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.math.vector.FiguraVec4;
import org.figuramc.figura.mixin.render.TextureManagerAccessor;
import org.figuramc.figura.utils.ColorUtils;
import org.figuramc.figura.utils.FiguraIdentifier;
import org.figuramc.figura.utils.LuaUtils;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;

@LuaWhitelist
@LuaTypeDoc(
        name = "Texture",
        value = "texture"
)
public class FiguraTexture extends SimpleTexture {

    /**
     * The ID of the texture, used to register to Minecraft.
     */
    private boolean registered = false;
    private boolean dirty = true;
    private boolean modified = false;
    private final String name;
    private final Avatar owner;

    /**
     * Native image holding the texture data for this texture.
     */
    private BufferedImage texture;
    private BufferedImage backup;
    private boolean isClosed = false;

    public FiguraTexture(Avatar owner, String name, byte[] data) {
        super(new FiguraIdentifier("avatar_tex/" + owner.owner + "/" + UUID.randomUUID()));

        // Read image from wrapper
        BufferedImage image;
        try {
            ByteBuffer wrapper = BufferUtils.createByteBuffer(data.length);
            wrapper.put(data);
            wrapper.rewind();
            // TODO: Check if this is correct and check the image formtat
            image = TextureUtil.readBufferedImage(new ByteArrayInputStream(data));
        } catch (IOException e) {
            FiguraMod.LOGGER.error("", e);
            image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        }

        this.texture = image;
        this.name = name;
        this.owner = owner;
    }

    public FiguraTexture(Avatar owner, String name, int width, int height) {
        super(new FiguraIdentifier("avatar_tex/" + owner.owner + "/" + UUID.randomUUID()));
        this.texture = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        this.name = name;
        this.owner = owner;
    }

    public FiguraTexture(Avatar owner, String name, BufferedImage image) {
        super(new FiguraIdentifier("avatar_tex/" + owner.owner + "/custom/" + UUID.randomUUID()));
        this.texture = image;
        this.name = name;
        this.owner = owner;
    }

    @Override
    public void loadTexture(IResourceManager resourceManager) throws IOException {}

    @Override
    public void deleteGlTexture() {
        // Make sure it doesn't close twice (minecraft tries to close the texture when reloading textures
        if (isClosed) return;

        isClosed = true;

        // Close native images
        texture.flush();
        if (backup != null)
            backup.flush();

        super.deleteGlTexture();
        ((TextureManagerAccessor) Minecraft.getMinecraft().getTextureManager()).getByPath().remove(this.textureLocation);
    }

    public void uploadIfDirty() {
        if (!registered) {
            Minecraft.getMinecraft().getTextureManager().loadTexture(this.textureLocation, this);
            registered = true;
        }

        if (dirty && !isClosed) {
            dirty = false;

            TextureUtil.uploadTextureImageAllocate(getGlTextureId(), texture, false, false);
        }
    }

    public void writeTexture(Path dest) throws IOException {
        ImageIO.write(texture, "png", dest.toFile());
    }

    private void backupImage() {
        this.modified = true;
        if (this.backup == null)
            backup = copy();
    }

    public BufferedImage copy() {
        return new BufferedImage(texture.getColorModel(), texture.copyData(null), texture.isAlphaPremultiplied(), null);
    }

    public int getWidth() {
        return texture.getWidth();
    }

    public int getHeight() {
        return texture.getHeight();
    }

    public ResourceLocation getLocation() {
        return this.textureLocation;
    }


    // -- lua stuff -- // 


    private FiguraVec4 parseColor(String method, Object r, Double g, Double b, Double a) {
        return LuaUtils.parseVec4(method, r, g, b, a, 0, 0, 0, 1);
    }

    @LuaWhitelist
    @LuaMethodDoc("texture.get_name")
    public String getName() {
        return name;
    }

    @LuaWhitelist
    @LuaMethodDoc("texture.get_path")
    public String getPath() {
        return getLocation().toString();
    }

    @LuaWhitelist
    @LuaMethodDoc("texture.get_dimensions")
    public FiguraVec2 getDimensions() {
        return FiguraVec2.of(getWidth(), getHeight());
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = {Integer.class, Integer.class},
                    argumentNames = {"x", "y"}
            ),
            value = "texture.get_pixel")
    public FiguraVec4 getPixel(int x, int y) {
        try {
            return ColorUtils.abgrToRGBA(texture.getRGB(x, y));
        } catch (Exception e) {
            throw new LuaError(e.getMessage());
        }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = {Integer.class, Integer.class, FiguraVec3.class},
                            argumentNames = {"x", "y", "rgb"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Integer.class, Integer.class, FiguraVec4.class},
                            argumentNames = {"x", "y", "rgba"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Integer.class, Integer.class, Double.class, Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "r", "g", "b", "a"}
                    )
            },
            aliases = "pixel",
            value = "texture.set_pixel")
    public FiguraTexture setPixel(int x, int y, Object r, Double g, Double b, Double a) {
        try {
            backupImage();
            texture.setRGB(x, y, ColorUtils.rgbaToIntABGR(parseColor("setPixel", r, g, b, a)));
            return this;
        } catch (Exception e) {
            throw new LuaError(e.getMessage());
        }
    }

    @LuaWhitelist
    public FiguraTexture pixel(int x, int y, Object r, Double g, Double b, Double a) {
        return setPixel(x, y, r, g, b, a);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = {Integer.class, Integer.class, Integer.class, Integer.class, FiguraVec3.class},
                            argumentNames = {"x", "y", "width", "height", "rgb"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Integer.class, Integer.class, Integer.class, Integer.class, FiguraVec4.class},
                            argumentNames = {"x", "y", "width", "height", "rgba"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Integer.class, Integer.class, Integer.class, Integer.class, Double.class, Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "width", "height", "r", "g", "b", "a"}
                    )
            },
            value = "texture.fill")
    public FiguraTexture fill(int x, int y, int width, int height, Object r, Double g, Double b, Double a) {
        try {
            backupImage();
            int color = ColorUtils.rgbaToIntABGR(parseColor("fill", r, g, b, a));
            for (int i = y; i < y + height; ++i) {
                for (int j = x; j < x + width; ++j) {
                    texture.setRGB(x, y, color);
                }
            }
            return this;
        } catch (Exception e) {
            throw new LuaError(e.getMessage());
        }
    }

    @LuaWhitelist
    @LuaMethodDoc("texture.update")
    public FiguraTexture update() {
        this.dirty = true;
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc("texture.restore")
    public FiguraTexture restore() {
        if (modified) {
            this.texture = new BufferedImage(backup.getColorModel(), backup.copyData(null), backup.isAlphaPremultiplied(), null);
            this.modified = false;
        }
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc("texture.save")
    public String save() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(texture, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new LuaError(e.getMessage());
        }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = {Integer.class, Integer.class, Integer.class, Integer.class, LuaFunction.class},
                    argumentNames = {"x", "y", "width", "height", "func"}
            ),
            value = "texture.apply_func"
    )
    public FiguraTexture applyFunc(int x, int y, int width, int height, @LuaNotNil LuaFunction function) {
        for (int i = y; i < y + height; i++) {
            for (int j = x; j < x + width; j++) {
                FiguraVec4 color = getPixel(j, i);
                LuaValue result = function.call(owner.luaRuntime.typeManager.javaToLua(color).arg1(), LuaValue.valueOf(j), LuaValue.valueOf(i));
                if (!result.isnil() && result.isuserdata(FiguraVec4.class))
                    setPixel(j, i, result.checkuserdata(FiguraVec4.class), null, null, null);
            }
        }
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = {Integer.class, Integer.class, Integer.class, Integer.class, FiguraMat4.class},
                    argumentNames = {"x", "y", "width", "height", "matrix"}
            ),
            value = "texture.apply_matrix"
    )
    public FiguraTexture applyMatrix(int x, int y, int width, int height, @LuaNotNil FiguraMat4 matrix) {
        for (int i = y; i < y + height; i++) {
            for (int j = x; j < x + width; j++) {
                FiguraVec4 color = getPixel(j, i);
                color.transform(matrix);
                setPixel(j, i, color, null, null, null);
            }
        }
        return this;
    }

    @LuaWhitelist
    public Object __index(String arg) {
        return "name".equals(arg) ? name : null;
    }

    @Override
    public String toString() {
        return name + " (" + getWidth() + "x" + getHeight() + ") (Texture)";
    }
}