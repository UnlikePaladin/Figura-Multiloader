package org.figuramc.figura.utils;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.api.vanilla_model.VanillaPart;
import org.figuramc.figura.model.ParentType;
import org.figuramc.figura.model.rendering.texture.RenderTypes;
import org.figuramc.figura.permissions.Permissions;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.function.Function;

public class RenderUtils {

    private static BufferedImage missing = null;
    public static BufferedImage getMissingTexture() {
        if (missing != null ) return missing;

        missing = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        missing.setRGB(0, 0, 16, 16, TextureUtil.MISSING_TEXTURE_DATA, 0, 16);
        return missing;
    }
    public static boolean vanillaModel(Avatar avatar) {
        return avatar != null && avatar.permissions.get(Permissions.VANILLA_MODEL_EDIT) >= 1;
    }

    public static boolean vanillaModelAndScript(Avatar avatar) {
        return avatar != null && avatar.luaRuntime != null && avatar.permissions.get(Permissions.VANILLA_MODEL_EDIT) >= 1;
    }

    public static TextureAtlasSprite firstFireLayer(Avatar avatar) {
        if (!vanillaModelAndScript(avatar))
            return null;

        ResourceLocation layer = avatar.luaRuntime.renderer.fireLayer1;
        return layer != null ? Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(layer.toString()) : null;
    }

    public static TextureAtlasSprite secondFireLayer(Avatar avatar) {
        if (!vanillaModelAndScript(avatar))
            return null;

        ResourceLocation layer1 = avatar.luaRuntime.renderer.fireLayer1;
        ResourceLocation layer2 = avatar.luaRuntime.renderer.fireLayer2;

        if (layer2 != null)
            return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(layer2.toString());
        if (layer1 != null)
            return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(layer1.toString());

        return null;
    }

    public static VanillaPart partFromSlot(Avatar avatar, EntityEquipmentSlot equipmentSlot) {
        if (!RenderUtils.vanillaModelAndScript(avatar))
            return null;

        switch (equipmentSlot) {
            case HEAD:
                return avatar.luaRuntime.vanilla_model.HELMET;
            case CHEST:
                return avatar.luaRuntime.vanilla_model.CHESTPLATE;
            case LEGS:
                return avatar.luaRuntime.vanilla_model.LEGGINGS;
            case FEET:
                return avatar.luaRuntime.vanilla_model.BOOTS;
            default:
                return null;
        }
    }

    public static EntityEquipmentSlot slotFromPart(ParentType type) {
        switch (type) {
            case Head:
            case HelmetItemPivot:
            case HelmetPivot:
            case Skull:
                return EntityEquipmentSlot.HEAD;
            case Body:
            case ChestplatePivot:
            case LeftShoulderPivot:
            case RightShoulderPivot:
            case LeftElytra:
            case RightElytra:
            case ElytraPivot:
                return EntityEquipmentSlot.CHEST;
            case LeftArm:
            case LeftItemPivot:
            case LeftSpyglassPivot:
                return EntityEquipmentSlot.OFFHAND;
            case RightArm:
            case RightItemPivot:
            case RightSpyglassPivot:
                return EntityEquipmentSlot.MAINHAND;
            case LeftLeggingPivot:
            case RightLeggingPivot:
            case LeftLeg:
            case RightLeg:
            case LeggingsPivot:
                return EntityEquipmentSlot.LEGS;
            case LeftBootPivot:
            case RightBootPivot:
                return EntityEquipmentSlot.FEET;
            default:
                return null;
        }

    }

    public static boolean renderArmItem(Avatar avatar, boolean lefty, CallbackInfo ci) {
        if (!vanillaModel(avatar))
            return false;

        if (avatar.luaRuntime != null && (
                lefty && !avatar.luaRuntime.vanilla_model.LEFT_ITEM.checkVisible() ||
                !lefty && !avatar.luaRuntime.vanilla_model.RIGHT_ITEM.checkVisible()
        )) {
            ci.cancel();
            return false;
        }

        return true;
    }

    @ExpectPlatform
    public static <T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> ResourceLocation getArmorResource(HumanoidArmorLayer<T, M, A> armorLayer, Entity entity, ItemStack stack, ArmorItem item, EquipmentSlot slot, boolean isInner, String type) {
        throw new AssertionError();
    }

    public static class TextRenderType extends RenderTypes.FiguraRenderType {
        public static Function<ResourceLocation, RenderTypes.FiguraRenderType> TEXT_BACKGROUND_SEE_THROUGH = ResourceUtils.memoize((texture) -> {
            return create("text_background_see_through", RenderTypes.FiguraRenderType.POSITION_COLOR_TEX_LIGHTMAP, VertexFormatMode.QUADS, 256, false, true, () -> {
                // Equivalent to enabling TextureState
                GlStateManager.enableTexture2D();
                TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
                textureManager.bindTexture(texture);
                textureManager.getTexture(texture).setBlurMipmap(false, false);
                // Equivalent to enabling TRANSLUCENT_TRANSPARENCY
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                // Disables writing to the depth buffer
                GlStateManager.depthMask(false);
                // Equivalent to enabling EQUAL_DEPTH_TEST
                GlStateManager.depthFunc(GL11.GL_ALWAYS);
                // Enables the light map
                Minecraft.getMinecraft().entityRenderer.enableLightmap();
            }, () -> {
                GlStateManager.depthFunc(GL11.GL_LEQUAL);
                GlStateManager.disableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                GlStateManager.depthMask(true);
                Minecraft.getMinecraft().entityRenderer.disableLightmap();
            });
        });
        public static Function<ResourceLocation, RenderTypes.FiguraRenderType> TEXT_BACKGROUND = ResourceUtils.memoize((texture) -> {
            return create("text_background", RenderTypes.FiguraRenderType.POSITION_COLOR_TEX_LIGHTMAP, VertexFormatMode.QUADS.asGLMode, 256, false, true, () -> {
                // Equivalent to enabling TextureState
                GlStateManager.enableTexture2D();
                TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
                textureManager.bindTexture(texture);
                textureManager.getTexture(texture).setBlurMipmap(false, false);
                // Equivalent to enabling TRANSLUCENT_TRANSPARENCY
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                // Enables the light map
                Minecraft.getMinecraft().entityRenderer.enableLightmap();
            }, () -> {
                GlStateManager.disableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                Minecraft.getMinecraft().entityRenderer.disableLightmap();
            });
        });

        public TextRenderType(String name, VertexFormat vertexFormat, VertexFormatMode drawMode, int expectedBufferSize, boolean hasCrumbling, boolean translucent, Runnable startAction, Runnable endAction) {
            super(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, startAction, endAction);
        }

    }

    public static final Vector3f INVENTORY_DIFFUSE_LIGHT_0 = ResourceUtils.make(new Vector3f(0.2f, -1.0f, -1.0f), Vector3f::normalise);
    public static final Vector3f INVENTORY_DIFFUSE_LIGHT_1 = ResourceUtils.make(new Vector3f(-0.2f, -1.0f, 0.0f), Vector3f::normalise);
    public static void setLights(Vector3f lightingVector1, Vector3f lightingVector2) {
        Vector4f vector4f = new Vector4f(lightingVector1.x, lightingVector1.y, lightingVector2.z, 1.0f);
        GlStateManager.pushMatrix();
        GlStateManager.glLight(GL11.GL_LIGHT0, GL11.GL_POSITION, getBuffer(vector4f.x, vector4f.y, vector4f.z, 0.0f));
        float f = 0.6f;
        GlStateManager.glLight(GL11.GL_LIGHT0, GL11.GL_DIFFUSE, getBuffer(f, f, f, 1.0f));
        GlStateManager.glLight(GL11.GL_LIGHT0, GL11.GL_AMBIENT, getBuffer(0.0f, 0.0f, 0.0f, 1.0f));
        GlStateManager.glLight(GL11.GL_LIGHT0, GL11.GL_SPECULAR, getBuffer(0.0f, 0.0f, 0.0f, 1.0f));
        Vector4f vector4f2 = new Vector4f(lightingVector2.x, lightingVector2.y, lightingVector2.z, 1.0f);

        GlStateManager.glLight(GL11.GL_LIGHT1, GL11.GL_POSITION, getBuffer(vector4f2.x, vector4f2.y, vector4f2.z, 0.0f));
        GlStateManager.glLight(GL11.GL_LIGHT1, GL11.GL_DIFFUSE, getBuffer(f, f, f, 1.0f));
        GlStateManager.glLight(GL11.GL_LIGHT1, GL11.GL_AMBIENT, getBuffer(0.0f, 0.0f, 0.0f, 1.0f));
        GlStateManager.glLight(GL11.GL_LIGHT1, GL11.GL_SPECULAR, getBuffer(0.0f, 0.0f, 0.0f, 1.0f));
        GlStateManager.shadeModel(GL11.GL_FLAT);
        float g = 0.4f;
        GlStateManager.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, getBuffer(g, g, g, 1.0f));
        GlStateManager.popMatrix();
    }
    private static final FloatBuffer FLOAT_ARG_BUFFER = BufferUtils.createFloatBuffer(4);
    protected static FloatBuffer getBuffer(float f, float g, float h, float i) {
        FLOAT_ARG_BUFFER.clear();
        FLOAT_ARG_BUFFER.put(f).put(g).put(h).put(i);
        FLOAT_ARG_BUFFER.flip();
        return FLOAT_ARG_BUFFER;
    }

}
