package org.figuramc.figura.mixin.render;

import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.figuramc.figura.ducks.TextureMapAccessor;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Map;

@Mixin(TextureMap.class)
public abstract class TextureMapMixin implements TextureMapAccessor {
    @Shadow protected abstract ResourceLocation getResourceLocation(TextureAtlasSprite textureAtlasSprite);

    @Shadow @Final private String basePath;
    @Unique
    private int atlasWidth;
    @Unique
    private int atlasHeight;
    @Intrinsic
    @Accessor("mapRegisteredSprites")
    abstract Map<String, TextureAtlasSprite> getTextureField();

    public Map<String, TextureAtlasSprite> getTexturesByName() {
        return getTextureField();
    }

    @Override
    public int getWidth() {
        return atlasWidth;
    }

    @Override
    public int getHeight() {
        return atlasHeight;
    }

    @Inject(method = "loadTextureAtlas", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/Stitcher;getCurrentWidth()I"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void captureWidthAndHeight(IResourceManager resourceManager, CallbackInfo ci, int j, Stitcher stitcher) {
        this.atlasWidth = stitcher.getCurrentWidth();
        this.atlasHeight = stitcher.getCurrentHeight();
    }

    @Override
    public ResourceLocation invokeGetResourceLocation(TextureAtlasSprite textureAtlasSprite) {
        return this.getResourceLocation(textureAtlasSprite);
    }

    @Override
    public String getAtlasName() {
        return basePath;
    }
}
