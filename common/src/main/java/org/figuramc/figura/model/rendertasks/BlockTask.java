package org.figuramc.figura.model.rendertasks;

import net.minecraft.block.BlockAir;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.api.world.BlockStateAPI;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaMethodOverload;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.model.FiguraModelPart;
import org.figuramc.figura.model.PartCustomization;
import org.figuramc.figura.model.rendering.texture.RenderTypes;
import org.figuramc.figura.utils.LuaUtils;

import java.util.Random;

@LuaWhitelist
@LuaTypeDoc(
        name = "BlockTask",
        value = "block_task"
)
public class BlockTask extends RenderTask {

    private IBlockState block;
    private int cachedComplexity;

    public BlockTask(String name, Avatar owner, FiguraModelPart parent) {
        super(name, owner, parent);
    }

    @Override
    public void renderTask(RenderTypes.FiguraBufferSource buffer, int light, int overlay) {
        GlStateManager.pushMatrix();
        GlStateManager.scale(16, 16, 16);
    //TODO: check if this has to actually be scaled
        int newLight = this.customization.light != null ? this.customization.light : light;
        int newOverlay = this.customization.overlay != null ? this.customization.overlay : overlay;

        float f = (float)(newLight & 65535);
        float g = (float)(newLight >> 16);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, f, g);
        Minecraft.getMinecraft().getBlockRendererDispatcher().renderBlock(block, getPos().asBlockPos(), Minecraft.getMinecraft().world, Tessellator.getInstance().getBuffer());
        GlStateManager.popMatrix();
    }

    @Override
    public int getComplexity() {
        return cachedComplexity;
    }

    @Override
    public boolean shouldRender() {
        return super.shouldRender() && block != null && !(block.getMaterial() == Material.AIR);
    }

    // -- lua -- //


    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = String.class,
                            argumentNames = "block"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = BlockStateAPI.class,
                            argumentNames = "block"
                    )
            },
            aliases = "block",
            value = "block_task.set_block"
    )
    public BlockTask setBlock(Object block) {
        this.block = LuaUtils.parseBlockState("block", block);
        Minecraft client = Minecraft.getMinecraft();
        Random random = client.world != null ? client.world.rand : new Random();

        IBakedModel blockModel = client.getBlockRendererDispatcher().getModelForState(this.block);
        cachedComplexity = blockModel.getQuads(this.block, null, random.nextLong()).size();
        for (EnumFacing dir : EnumFacing.values())
            cachedComplexity += blockModel.getQuads(this.block, dir, random.nextLong()).size();

        return this;
    }

    @LuaWhitelist
    public BlockTask block(Object block) {
        return setBlock(block);
    }

    @Override
    public String toString() {
        return name + " (Block Render Task)";
    }
}
