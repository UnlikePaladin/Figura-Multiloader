package org.figuramc.figura.model.rendertasks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.LuaNotNil;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.api.world.ItemStackAPI;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaMethodOverload;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.model.FiguraModelPart;
import org.figuramc.figura.model.rendering.texture.RenderTypes;
import org.figuramc.figura.utils.LuaUtils;
import org.luaj.vm2.LuaError;

import java.util.Random;

@LuaWhitelist
@LuaTypeDoc(
        name = "ItemTask",
        value = "item_task"
)
public class ItemTask extends RenderTask {

    private ItemStack item;
    private ItemCameraTransforms.TransformType displayMode = ItemCameraTransforms.TransformType.NONE;
    private boolean left = false;
    private int cachedComplexity;

    public ItemTask(String name, Avatar owner, FiguraModelPart parent) {
        super(name, owner, parent);
    }

    @Override
    public void renderTask(RenderTypes.FiguraBufferSource buffer, int light, int overlay) {
        GlStateManager.pushMatrix();
        GlStateManager.scale(-16, 16, -16);

        EntityLivingBase entity = owner.renderer.entity instanceof EntityLivingBase ? (EntityLivingBase) owner.renderer.entity : null;
        int newLight = this.customization.light != null ? this.customization.light : light;
        int newOverlay = this.customization.overlay != null ? this.customization.overlay : overlay;

        float f = (float)(newLight & 65535);
        float g = (float)(newLight >> 16);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, f, g);
        Minecraft.getMinecraft().getItemRenderer().renderItemSide(
                entity, item, displayMode, left
        );
        GlStateManager.popMatrix();
    }

    @Override
    public int getComplexity() {
        return cachedComplexity;
    }

    @Override
    public boolean shouldRender() {
        return super.shouldRender() && item != null && !item.isEmpty();
    }

    // -- lua -- //


    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = String.class,
                            argumentNames = "item"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = ItemStackAPI.class,
                            argumentNames = "item"
                    )
            },
            aliases = "item",
            value = "item_task.set_item"
    )
    public ItemTask setItem(Object item) {
        this.item = LuaUtils.parseItemStack("item", item);
        Minecraft client = Minecraft.getMinecraft();
        Random random = client.world != null ? client.world.rand : new Random();
        cachedComplexity = client.getRenderItem().getItemModelWithOverrides(this.item, null, null).getQuads(null, null, random.nextLong()).size();
        return this;
    }

    @LuaWhitelist
    public ItemTask item(Object item) {
        return setItem(item);
    }

    @LuaWhitelist
    @LuaMethodDoc("item_task.get_display_mode")
    public String getDisplayMode() {
        return this.displayMode.name();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                        argumentTypes = String.class,
                        argumentNames = "displayMode"
            ),
            aliases = "displayMode",
            value = "item_task.set_display_mode"
    )
    public ItemTask setDisplayMode(@LuaNotNil String mode) {
        try {
            this.displayMode = ItemCameraTransforms.TransformType.valueOf(mode.toUpperCase());
            this.left = this.displayMode == ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND || this.displayMode == ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND;
            return this;
        } catch (Exception ignored) {
            throw new LuaError("Illegal display mode: \"" + mode + "\".");
        }
    }

    @LuaWhitelist
    public ItemTask displayMode(@LuaNotNil String mode) {
        return setDisplayMode(mode);
    }

    @Override
    public String toString() {
        return name + " (Item Render Task)";
    }
}
