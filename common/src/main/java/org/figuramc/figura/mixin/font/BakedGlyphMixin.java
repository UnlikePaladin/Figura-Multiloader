package org.figuramc.figura.mixin.font;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import org.figuramc.figura.compat.ImmediatelyFastCompat;
import org.figuramc.figura.ducks.BakedGlyphAccessor;
import org.figuramc.figura.font.EmojiContainer;
import org.figuramc.figura.font.EmojiMetadata;
import org.figuramc.figura.font.Emojis;
import org.figuramc.figura.utils.PlatformUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Mixin(FontRenderer.class)
public abstract class BakedGlyphMixin implements BakedGlyphAccessor {
    @Unique
    EmojiMetadata figura$metadata;

    @Override
    public void figura$setupEmoji(@Nullable EmojiContainer container, int codepoint) {
        if (container != null) {
            figura$metadata = container.getLookup().getMetadata(codepoint);
        }
    }

    @Inject(method = "renderChar", at = @At("HEAD"), cancellable = true)
    public void render(char c, boolean italic, CallbackInfoReturnable<Float> cir) {
        if (figura$metadata == null) return;
        int j = i % 16 * 8;
        int k = i / 16 * 8;
        int l = italic ? 1 : 0;


        float h = this.up - 3.0f;
        float j = this.down - 3.0f;
        float k = y + h;
        float l = y + j;
        float m = italic ? 1.0f - 0.25f * h : 0f;
        float n = italic ? 1.0f - 0.25f * j : 0f;

        final float singleWidth = 8f / ImmediatelyFastCompat.getFontWidthIMF();
        float shift = singleWidth * figura$metadata.getCurrentFrame();

        float u = u0 + shift;
        vertexConsumer.vertex(matrix, x + m, k, 0.0f).color(red, green, blue, alpha).uv(u, this.v0).uv2(light).endVertex();
        vertexConsumer.vertex(matrix, x + n, l, 0.0f).color(red, green, blue, alpha).uv(u, this.v1).uv2(light).endVertex();
        vertexConsumer.vertex(matrix, x + figura$metadata.width + n, l, 0.0f).color(red, green, blue, alpha).uv(u + singleWidth, this.v1).uv2(light).endVertex();
        vertexConsumer.vertex(matrix, x + figura$metadata.width + m, k, 0.0f).color(red, green, blue, alpha).uv(u + singleWidth, this.v0).uv2(light).endVertex();
        ci.cancel();
    }

    @Override
    public float figura$getU0() {
        return u0;
    }

    @Override
    public float figura$getV0() {
        return v0;
    }

    @Override
    public float figura$getU1() {
        return u1;
    }

    @Override
    public float figura$getV1() {
        return v1;
    }
}
