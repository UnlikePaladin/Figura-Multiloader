package org.figuramc.figura.model.rendering.texture;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import org.figuramc.figura.mixin.render.layers.LayerArmorBaseAccessor;
import org.figuramc.figura.mixin.render.renderers.TileEntityEndPortalRendererAccessor;
import org.figuramc.figura.utils.ResourceUtils;
import org.figuramc.figura.utils.VertexFormatMode;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.function.BiFunction;
import java.util.function.Function;

public enum RenderTypes {
    NONE(null),

    CUTOUT(FiguraRenderType::entityCutoutNoCull),
    CUTOUT_CULL(FiguraRenderType::entityCutout),
    CUTOUT_EMISSIVE_SOLID(resourceLocation -> FiguraRenderType.CUTOUT_EMISSIVE_SOLID.apply(resourceLocation, false)),

    TRANSLUCENT(FiguraRenderType::entityTranslucent),
    TRANSLUCENT_CULL(FiguraRenderType::entityTranslucentCull),

    EMISSIVE(FiguraRenderType::eyes),
    EMISSIVE_SOLID(resourceLocation -> FiguraRenderType.beaconBeam(resourceLocation, false)),
    EYES(FiguraRenderType::eyes),

    END_PORTAL(t -> FiguraRenderType.getEndPortal(0), false),
    END_GATEWAY(t -> FiguraRenderType.getEndPortal(0), false),
    TEXTURED_PORTAL(resourceLocation -> FiguraRenderType.getTexturedPortal(resourceLocation, 0)),

    GLINT(t -> FiguraRenderType.ENTITY_GLINT_DIRECT, false, false),
    GLINT2(t -> FiguraRenderType.GLINT_DIRECT, false, false),
    TEXTURED_GLINT(FiguraRenderType.TEXTURED_GLINT, true, false),

    LINES(t -> FiguraRenderType.LINES, false),
    LINES_STRIP(t -> FiguraRenderType.LINE_STRIP, false),
    SOLID(t -> FiguraRenderType.SOLID, false),

    BLURRY(FiguraRenderType.BLURRY);

    private final Function<ResourceLocation, FiguraRenderType> func;
    private final boolean texture, offset;

    RenderTypes(Function<ResourceLocation, FiguraRenderType> func) {
        this(func, true);
    }

    RenderTypes(Function<ResourceLocation, FiguraRenderType> func, boolean texture) {
        this(func, texture, true);
    }

    RenderTypes(Function<ResourceLocation, FiguraRenderType> func, boolean texture, boolean offset) {
        this.func = func;
        this.texture = texture;
        this.offset = offset;
    }

    public boolean isOffset() {
        return offset;
    }

    public FiguraRenderType get(ResourceLocation id) {
        if (!texture)
            return func.apply(id);

        return id == null || func == null ? null : func.apply(id);
    }

    public static class FiguraBufferSource {
        public static FiguraBufferSource INSTANCE = new FiguraBufferSource();

        public BufferBuilder getBuffer(FiguraRenderType type) {
            Tessellator tessellator = Tessellator.getInstance();
            type.setupState.run();
            return tessellator.getBuffer();
        }

        public void endBatch() {

            Tessellator.getInstance().draw();
        }
    }
    public static class FiguraRenderType {


        public static final VertexFormat ENTITY_FORMAT = new VertexFormat().addElement(DefaultVertexFormats.POSITION_3F).addElement(DefaultVertexFormats.COLOR_4UB).addElement(DefaultVertexFormats.TEX_2F).addElement(DefaultVertexFormats.TEX_2S).addElement(DefaultVertexFormats.NORMAL_3B).addElement(DefaultVertexFormats.PADDING_1B);
        public static final VertexFormat POSITION_COLOR_TEX_LIGHTMAP = new VertexFormat().addElement(DefaultVertexFormats.POSITION_3F).addElement(DefaultVertexFormats.COLOR_4UB).addElement(DefaultVertexFormats.TEX_2F).addElement(DefaultVertexFormats.TEX_2S);

        private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer(16);

        public final VertexFormat format;
        public final int mode;
        private final int bufferSize;
        private final boolean affectsCrumbling;
        private final boolean sortOnUpload;
        protected final String name;
        public final Runnable setupState;
        public final Runnable clearState;

        public FiguraRenderType(String name, VertexFormat vertexFormat, VertexFormatMode drawMode, int expectedBufferSize, boolean hasCrumbling, boolean translucent, Runnable startAction, Runnable endAction) {
            this.name = name;
            this.setupState = startAction;
            this.clearState = endAction;
            this.format = vertexFormat;
            this.mode = drawMode.asGLMode;
            this.bufferSize = expectedBufferSize;
            this.affectsCrumbling = hasCrumbling;
            this.sortOnUpload = translucent;
        }

        public static FiguraRenderType create(String string, VertexFormat vertexFormat, VertexFormatMode mode, int buffSize, boolean crumbles, boolean translucent, Runnable start, Runnable end) {
            return INSTANCES.addOrGet(new FiguraRenderType(string, vertexFormat, mode, buffSize, crumbles, translucent, start, end));
        }

        public static FiguraRenderType create(String string, VertexFormat vertexFormat, VertexFormatMode mode, int buffSize, Runnable start, Runnable end) {
            return create(string, vertexFormat, mode, buffSize, false, false, start, end);
        }

        public static final FiguraRenderType SOLID = create(
                "figura_solid",
                DefaultVertexFormats.POSITION_COLOR,
                VertexFormatMode.QUADS,
                256,
                () -> {
                    // Equivalent to enabling VIEW_OFFSET_Z_LAYERING
                    GlStateManager.pushMatrix();
                    GlStateManager.scale(0.99975586f, 0.99975586f, 0.99975586f);
                    // Equivalent to enabling Empty LineStateShard
                    GlStateManager.glLineWidth(Math.max(2.5f, (float)Minecraft.getMinecraft().displayWidth / 1920.0f * 2.5f));
                    // Equivalent to enabling TRANSLUCENT_TRANSPARENCY
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                    // Equivalent to enabling NO_CULL
                    GlStateManager.disableCull();
                }, () -> {
                    // Equivalent to disabling VIEW_OFFSET_Z_LAYERING
                    GlStateManager.popMatrix();
                    // Equivalent to disabling Empty LineStateShard
                    GlStateManager.glLineWidth(1.0f);
                    // Equivalent to disabling TRANSLUCENT_TRANSPARENCY
                    GlStateManager.disableBlend();
                    GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                    // Equivalent to disabling NO_CULL
                    GlStateManager.enableCull();
                }
        );

        private static final BiFunction<ResourceLocation, Boolean, FiguraRenderType> CUTOUT_EMISSIVE_SOLID = ResourceUtils.memoize(
                (texture, affectsOutline) ->
                        create("figura_cutout_emissive_solid", DefaultVertexFormats.BLOCK, VertexFormatMode.QUADS, 256, true, true,
                            () -> {
                                // Equivalent to enabling TextureState
                                GlStateManager.enableTexture2D();
                                TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
                                textureManager.bindTexture(texture);
                                textureManager.getTexture(texture).setBlurMipmap(false, false);
                                // Equivalent to enabling TRANSLUCENT_TRANSPARENCY
                                GlStateManager.enableBlend();
                                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                                // Equivalent to enabling NO_CULL
                                GlStateManager.disableCull();
                                // Minecraft.getMinecraft().gameRenderer.overlayTexture().setupOverlayColor();
                                // TODO: Figure out how the overlay works
                            }, () -> {
                                // Equivalent to disabling TRANSLUCENT_TRANSPARENCY
                                GlStateManager.disableBlend();
                                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                                // Equivalent to disabling NO_CULL
                                GlStateManager.enableCull();
                        }));

        public static FiguraRenderType getTexturedPortal(ResourceLocation texture, int i) {
            Function<Void, Void> transparencyStateShard;
            if (i <= 1) {
                transparencyStateShard = tex -> {
                    // Equivalent to enabling TRANSLUCENT_TRANSPARENCY
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                    return null;
                };
            } else {
                transparencyStateShard = tex -> {
                    // Equivalent to enabling TRANSLUCENT_TRANSPARENCY
                    GlStateManager.enableBlend();
                    GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
                    return null;
                };
            }
            return create(
                    "figura_textured_portal",
                            DefaultVertexFormats.POSITION_COLOR,
                            VertexFormatMode.QUADS,
                            256,
                            false,
                            false, () -> {
                            // Equivalent to enabling TextureState
                            GlStateManager.enableTexture2D();
                            TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
                            textureManager.bindTexture(texture);
                            textureManager.getTexture(texture).setBlurMipmap(false, false);
                            // Equivalent the PortalTexturingState
                            setupEndPortal(i);
                            // Equivalent to calling setTransparencyState
                            transparencyStateShard.apply(null);
                    }, () -> {
                        GlStateManager.disableBlend();
                        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                        clearEndPortal();
                    }
            );
        }

        public static FiguraRenderType getEndPortal(int i) {
            Function<Void, Void> transparencyStateShard;
            ResourceLocation texture;
            if (i <= 1) {
                transparencyStateShard = tex -> {
                    // Equivalent to enabling TRANSLUCENT_TRANSPARENCY
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                    return null;
                };
                texture = TileEntityEndPortalRendererAccessor.getEndSkyTexture();
            } else {
                transparencyStateShard = tex -> {
                    // Equivalent to enabling TRANSLUCENT_TRANSPARENCY
                    GlStateManager.enableBlend();
                    GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
                    return null;
                };
                texture = TileEntityEndPortalRendererAccessor.getEndPortalTexture();
            }
            return create(
                    "end_portal",
                    DefaultVertexFormats.POSITION_COLOR,
                    VertexFormatMode.QUADS,
                    256,
                    false,
                    false, () -> {
                        // Equivalent to enabling TextureState
                        GlStateManager.enableTexture2D();
                        TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
                        textureManager.bindTexture(texture);
                        textureManager.getTexture(texture).setBlurMipmap(false, false);
                        // Equivalent the PortalTexturingState
                        setupEndPortal(i);
                        // Equivalent to calling setTransparencyState
                        transparencyStateShard.apply(null);
                        // Sets the fog to black
                        Minecraft.getMinecraft().entityRenderer.setupFogColor(true);
                    }, () -> {
                        GlStateManager.disableBlend();
                        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                        clearEndPortal();
                        Minecraft.getMinecraft().entityRenderer.setupFogColor(false);
                    }
            );
        }

        public static void setupEndPortal(int i) {
            GlStateManager.matrixMode(GL11.GL_TEXTURE);
            GlStateManager.pushMatrix();
            GlStateManager.loadIdentity();
            GlStateManager.translate(0.5f, 0.5f, 0.0f);
            GlStateManager.scale(0.5f, 0.5f, 1.0f);
            GlStateManager.translate(17.0f / i, (2.0f + i / 1.5f) * ((Minecraft.getSystemTime() % 800000L) / 800000.0f), 0.0f);
            GlStateManager.rotate(((i * i) * 4321.0f + i * 9.0f) * 2.0f, 0.0f, 0.0f, 1.0f);
            GlStateManager.scale(4.5f - i / 4.0f, 4.5f - i / 4.0f, 1.0f);

            GlStateManager.getFloat(GL11.GL_PROJECTION_MATRIX, MATRIX_BUFFER);
            GlStateManager.multMatrix(MATRIX_BUFFER);
            GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, MATRIX_BUFFER);
            GlStateManager.multMatrix(MATRIX_BUFFER);

            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            setupEndPortalTexGen();
        }

        public static void clearEndPortal() {
            GlStateManager.matrixMode(GL11.GL_TEXTURE);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.disableTexGenCoord(GlStateManager.TexGen.S);
            GlStateManager.disableTexGenCoord(GlStateManager.TexGen.T);
            GlStateManager.disableTexGenCoord(GlStateManager.TexGen.R);
        }

        private static final FloatBuffer FLOAT_ARG_BUFFER = BufferUtils.createFloatBuffer(4);

        public static void setupEndPortalTexGen() {
            GlStateManager.texGen(GlStateManager.TexGen.S, GL11.GL_EYE_LINEAR);
            GlStateManager.texGen(GlStateManager.TexGen.T, GL11.GL_EYE_LINEAR);
            GlStateManager.texGen(GlStateManager.TexGen.R, GL11.GL_EYE_LINEAR);
            GlStateManager.texGen(GlStateManager.TexGen.S, GL11.GL_EYE_PLANE, getBuffer(1.0f, 0.0f, 0.0f, 0.0f));
            GlStateManager.texGen(GlStateManager.TexGen.T, GL11.GL_EYE_PLANE, getBuffer(0.0f, 1.0f, 0.0f, 0.0f));
            GlStateManager.texGen(GlStateManager.TexGen.R, GL11.GL_EYE_PLANE, getBuffer(0.0f, 0.0f, 1.0f, 0.0f));
            GlStateManager.enableTexGenCoord(GlStateManager.TexGen.S);
            GlStateManager.enableTexGenCoord(GlStateManager.TexGen.T);
            GlStateManager.enableTexGenCoord(GlStateManager.TexGen.R);
        }

        protected static FloatBuffer getBuffer(float f, float g, float h, float i) {
            ((Buffer)FLOAT_ARG_BUFFER).clear();
            FLOAT_ARG_BUFFER.put(f).put(g).put(h).put(i);
            ((Buffer)FLOAT_ARG_BUFFER).flip();
            return FLOAT_ARG_BUFFER;
        }


        public static final Function<ResourceLocation, FiguraRenderType> BLURRY = ResourceUtils.memoize(
                texture -> create(
                        "figura_blurry",
                        ENTITY_FORMAT,
                        VertexFormatMode.QUADS,
                        256,
                        true,
                        true,
                        () -> {
                            // Equivalent to enabling TextureState
                            GlStateManager.enableTexture2D();
                            TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
                            textureManager.bindTexture(texture);
                            textureManager.getTexture(texture).setBlurMipmap(true, false);
                            // Equivalent to enabling TRANSLUCENT_TRANSPARENCY
                            GlStateManager.enableBlend();
                            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                            // Equivalent to enabling NO_CULL
                            GlStateManager.disableCull();
                            // TODO: Enable overlay
                            Minecraft.getMinecraft().entityRenderer.enableLightmap();

                        }, () -> {
                            GlStateManager.disableBlend();
                            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                            GlStateManager.enableCull();
                            Minecraft.getMinecraft().entityRenderer.disableLightmap();
                        }
                )
        );

        public static final Function<ResourceLocation, FiguraRenderType> TEXTURED_GLINT = ResourceUtils.memoize(
                texture -> create(
                        "figura_textured_glint_direct",
                        DefaultVertexFormats.POSITION_TEX,
                        VertexFormatMode.QUADS,
                        256,
                        false,
                        false,
                        () -> {
                            // Equivalent to enabling TextureState
                            GlStateManager.enableTexture2D();
                            TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
                            textureManager.bindTexture(texture);
                            textureManager.getTexture(texture).setBlurMipmap(false, false);
                            // Equivalent to enabling NO_CULL
                            GlStateManager.disableCull();
                            GlStateManager.enableDepth();
                            // Equivalent to enabling EQUAL_DEPTH_TEST
                            GlStateManager.depthFunc(GL11.GL_EQUAL);
                            // Equivalent to enabling GLINT_TRANSPARENCY
                            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
                            // Equivalent to enabling ENTITY_GLINT_TEXTURING
                            setupGlint(0.16f);
                            // Disables writing to the depth buffer
                            GlStateManager.depthMask(false);
                        }, () -> {
                            GlStateManager.enableCull();
                            GlStateManager.disableDepth();
                            GlStateManager.depthFunc(GL11.GL_LEQUAL);
                            GlStateManager.disableBlend();
                            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                            disableGlint();
                            GlStateManager.depthMask(true);
                        }
                )
        );


        private static void setupGlint(float scale) {
            GlStateManager.matrixMode(GL11.GL_TEXTURE);
            GlStateManager.pushMatrix();
            GlStateManager.loadIdentity();
            long l = Minecraft.getSystemTime() * 8L;
            float f = (float)(l % 110000L) / 110000.0f;
            float g = (float)(l % 30000L) / 30000.0f;
            GlStateManager.translate(-f, g, 0.0f);
            GlStateManager.rotate(10.0f, 0.0f, 0.0f, 1.0f);
            GlStateManager.scale(scale, scale, scale);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        }

        private static void disableGlint() {
            GlStateManager.matrixMode(GL11.GL_TEXTURE);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        }

        public static final FiguraRenderType LINE_STRIP = FiguraRenderType.create("line_strip", DefaultVertexFormats.POSITION_COLOR, VertexFormatMode.LINE_STRIP, 256,
                () -> {
                    // Equivalent to setting the line width
                    GlStateManager.glLineWidth(0.5f);
                    // Enables wireframe
                    GlStateManager.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
                    // Equivalent to VIEW_OFFSET_Z_LAYERING
                    GlStateManager.pushMatrix();
                    GlStateManager.scale(0.99975586f, 0.99975586f, 0.99975586f);
                    // Disables culling
                    GlStateManager.disableCull();
                }, () -> {
                    GlStateManager.glLineWidth(1.0f);
                    GlStateManager.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
                    GlStateManager.popMatrix();
                    GlStateManager.enableCull();
            });
        public static final Function<ResourceLocation, FiguraRenderType> TEXT_POLYGON_OFFSET = ResourceUtils.memoize(texture -> FiguraRenderType.create("text_polygon_offset", POSITION_COLOR_TEX_LIGHTMAP, VertexFormatMode.QUADS, 256, false, true,
                () -> {
                    // Equivalent to enabling TextureState
                    GlStateManager.enableTexture2D();
                    TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
                    textureManager.bindTexture(texture);
                    textureManager.getTexture(texture).setBlurMipmap(false, false);
                    // Equivalent to enabling TRANSLUCENT_TRANSPARENCY
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                    Minecraft.getMinecraft().entityRenderer.enableLightmap();
                    GlStateManager.doPolygonOffset(-1.0f, -10.0f);
                    GlStateManager.enablePolygonOffset();
                }, () -> {
                    GlStateManager.disableBlend();
                    GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                    GlStateManager.doPolygonOffset(0.0f, 0.0f);
                    GlStateManager.disablePolygonOffset();
                    Minecraft.getMinecraft().entityRenderer.disableLightmap();
                }));

        public static FiguraRenderType entityCutoutNoCull(ResourceLocation texture) {
            return FiguraRenderType.create("entity_cutout_no_cull", ENTITY_FORMAT, VertexFormatMode.QUADS, 256, true, false, () -> {
                // Equivalent to enabling TextureState
                GlStateManager.enableTexture2D();
                TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
                textureManager.bindTexture(texture);
                textureManager.getTexture(texture).setBlurMipmap(false, false);
                // Equivalent to disabling transparency
                GlStateManager.disableBlend();
                // Sets diffuse lighting
                GlStateManager.enableLighting();
                GlStateManager.enableColorMaterial();
                GlStateManager.colorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE);
                // Sets alpha
                GlStateManager.enableAlpha();
                GlStateManager.alphaFunc(GL11.GL_GREATER, 0.003921569f);
                // Disables cull
                GlStateManager.disableCull();
                Minecraft.getMinecraft().entityRenderer.enableLightmap();
                //TODO: Figure out outline
            }, () -> {
                GlStateManager.disableLighting();
                GlStateManager.disableColorMaterial();
                GlStateManager.disableAlpha();
                GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
                GlStateManager.enableCull();
                Minecraft.getMinecraft().entityRenderer.disableLightmap();
            });
        }

        public static FiguraRenderType entityCutout(ResourceLocation texture) {
            return FiguraRenderType.create("entity_cutout", ENTITY_FORMAT, VertexFormatMode.QUADS, 256, true, false, () -> {
                // Equivalent to enabling TextureState
                GlStateManager.enableTexture2D();
                TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
                textureManager.bindTexture(texture);
                textureManager.getTexture(texture).setBlurMipmap(false, false);
                // Equivalent to disabling transparency
                GlStateManager.disableBlend();
                // Sets diffuse lighting
                GlStateManager.enableLighting();
                GlStateManager.enableColorMaterial();
                GlStateManager.colorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE);
                // Sets alpha
                GlStateManager.enableAlpha();
                GlStateManager.alphaFunc(GL11.GL_GREATER, 0.003921569f);
                //TODO: Figure out outline
                Minecraft.getMinecraft().entityRenderer.enableLightmap();
            }, () -> {
                GlStateManager.disableLighting();
                GlStateManager.disableColorMaterial();
                GlStateManager.disableAlpha();
                GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
                Minecraft.getMinecraft().entityRenderer.disableLightmap();
            });
        }

        public static FiguraRenderType entityTranslucent(ResourceLocation texture) {
            return FiguraRenderType.create("entity_translucent", ENTITY_FORMAT, VertexFormatMode.QUADS, 256, true, true, () -> {
                // Equivalent to enabling TextureState
                GlStateManager.enableTexture2D();
                TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
                textureManager.bindTexture(texture);
                textureManager.getTexture(texture).setBlurMipmap(false, false);
                // Sets diffuse lighting
                GlStateManager.enableLighting();
                GlStateManager.enableColorMaterial();
                GlStateManager.colorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE);
                // Sets alpha
                GlStateManager.enableAlpha();
                GlStateManager.alphaFunc(GL11.GL_GREATER, 0.003921569f);
                // Disables cull
                GlStateManager.disableCull();
                // Enables transparency
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                //TODO: Figure out outline
                Minecraft.getMinecraft().entityRenderer.enableLightmap();
            }, () -> {
                GlStateManager.disableLighting();
                GlStateManager.disableColorMaterial();
                GlStateManager.disableAlpha();
                GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
                GlStateManager.disableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                Minecraft.getMinecraft().entityRenderer.disableLightmap();
            });
        }

        public static FiguraRenderType entityTranslucentCull(ResourceLocation texture) {
            return FiguraRenderType.create("entity_translucent_cull", ENTITY_FORMAT, VertexFormatMode.QUADS, 256, true, true, () -> {
                // Equivalent to enabling TextureState
                GlStateManager.enableTexture2D();
                TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
                textureManager.bindTexture(texture);
                textureManager.getTexture(texture).setBlurMipmap(false, false);
                // Sets diffuse lighting
                GlStateManager.enableLighting();
                GlStateManager.enableColorMaterial();
                GlStateManager.colorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE);
                // Sets alpha
                GlStateManager.enableAlpha();
                GlStateManager.alphaFunc(GL11.GL_GREATER, 0.003921569f);
                // Enables transparency
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                //TODO: Figure out outline
                Minecraft.getMinecraft().entityRenderer.enableLightmap();
            }, () -> {
                GlStateManager.disableLighting();
                GlStateManager.disableColorMaterial();
                GlStateManager.disableAlpha();
                GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
                GlStateManager.disableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                Minecraft.getMinecraft().entityRenderer.disableLightmap();
            });
        }

        public static FiguraRenderType eyes(ResourceLocation texture) {
            return FiguraRenderType.create("eyes", ENTITY_FORMAT, VertexFormatMode.QUADS, 256, true, true, () -> {
                // Equivalent to enabling TextureState
                GlStateManager.enableTexture2D();
                TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
                textureManager.bindTexture(texture);
                textureManager.getTexture(texture).setBlurMipmap(false, false);
                // Enables additive transparency
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
                // Sets the fog to black
                Minecraft.getMinecraft().entityRenderer.setupFogColor(true);
            }, () -> {
                GlStateManager.disableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                Minecraft.getMinecraft().entityRenderer.setupFogColor(false);
            });
        }

        public static FiguraRenderType beaconBeam(ResourceLocation texture, boolean colorFlag) {
            Function<Void, Void> transparencyFunc;
            Function<Void, Void> endTransparencyFunc;
            // Transparency depending on color flag
            if (colorFlag) {
                transparencyFunc = translucent -> {
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                    GlStateManager.depthMask(false);
                  return null;
                };
                endTransparencyFunc = disable -> {
                    GlStateManager.disableBlend();
                    GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                    GlStateManager.depthMask(true);
                    return null;
                };
            } else {
                transparencyFunc = opaque -> {
                    GlStateManager.disableBlend();
                    return null;
                };
                endTransparencyFunc = unused -> null;
            }
            return FiguraRenderType.create("beacon_beam", DefaultVertexFormats.BLOCK, VertexFormatMode.QUADS, 256, false, true, () -> {
                // Equivalent to enabling TextureState
                GlStateManager.enableTexture2D();
                TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
                textureManager.bindTexture(texture);
                textureManager.getTexture(texture).setBlurMipmap(false, false);
                transparencyFunc.apply(null);
            }, () -> {
                endTransparencyFunc.apply(null);
            });
        }

        private static final FiguraRenderType ENTITY_GLINT_DIRECT = FiguraRenderType.create("entity_glint_direct", DefaultVertexFormats.POSITION_TEX, VertexFormatMode.QUADS, 256,
                () -> {
                    ResourceLocation texture = LayerArmorBaseAccessor.getItemGlint();
                    // Equivalent to enabling TextureState
                    GlStateManager.enableTexture2D();
                    TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
                    textureManager.bindTexture(texture);
                    textureManager.getTexture(texture).setBlurMipmap(true, false);
                    // Disables writing to the depth buffer
                    GlStateManager.depthMask(false);
                    // Disables culling
                    GlStateManager.disableCull();
                    // Equivalent to enabling EQUAL_DEPTH_TEST
                    GlStateManager.depthFunc(GL11.GL_EQUAL);
                    // Equivalent to enabling GLINT_TRANSPARENCY
                    GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
                    // Equivalent to enabling ENTITY_GLINT_TEXTURING
                    setupGlint(0.16f);
                }, () -> {
                    GlStateManager.depthMask(true);
                    GlStateManager.enableCull();
                    GlStateManager.depthFunc(GL11.GL_LEQUAL);
                    GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                    disableGlint();
                });

        private static final FiguraRenderType GLINT_DIRECT = FiguraRenderType.create("entity_glint_direct", DefaultVertexFormats.POSITION_TEX, VertexFormatMode.QUADS, 256,
                () -> {
                    ResourceLocation texture = LayerArmorBaseAccessor.getItemGlint();
                    // Equivalent to enabling TextureState
                    GlStateManager.enableTexture2D();
                    TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
                    textureManager.bindTexture(texture);
                    textureManager.getTexture(texture).setBlurMipmap(true, false);
                    // Disables writing to the depth buffer
                    GlStateManager.depthMask(false);
                    // Disables culling
                    GlStateManager.disableCull();
                    // Equivalent to enabling EQUAL_DEPTH_TEST
                    GlStateManager.depthFunc(GL11.GL_EQUAL);
                    // Equivalent to enabling GLINT_TRANSPARENCY
                    GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
                    // Equivalent to enabling GLINT_TEXTURING
                    setupGlint(8.0f);
                }, () -> {
                    GlStateManager.depthMask(true);
                    GlStateManager.enableCull();
                    GlStateManager.depthFunc(GL11.GL_LEQUAL);
                    GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                    disableGlint();
                });

        private static final ObjectOpenCustomHashSet<FiguraRenderType> INSTANCES = new ObjectOpenCustomHashSet<>(new Hash.Strategy<FiguraRenderType>() {
            @Override
            public int hashCode(FiguraRenderType o) {
                if (o == null)
                    return 0;
                return Objects.hashCode(o.name);
            }

            @Override
            public boolean equals(FiguraRenderType a, FiguraRenderType b) {
                if (a == b) {
                    return true;
                }
                if (a == null || b == null) {
                    return false;
                }
                return Objects.equals(a.name, b.name);
            }
        });

        public static final FiguraRenderType LINES = FiguraRenderType.create("lines", DefaultVertexFormats.POSITION_COLOR, VertexFormatMode.DEBUG_LINES, 256,
            () -> {
                // Equivalent to VIEW_OFFSET_Z_LAYERING
                GlStateManager.pushMatrix();
                GlStateManager.scale(0.99975586f, 0.99975586f, 0.99975586f);
                // Equivalent to enabling Empty LineStateShard
                GlStateManager.glLineWidth(Math.max(2.5f, (float)Minecraft.getMinecraft().displayWidth / 1920.0f * 2.5f));
                // Equivalent to enabling TRANSLUCENT_TRANSPARENCY
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

            }, () -> {
                GlStateManager.popMatrix();
                GlStateManager.glLineWidth(1.0f);
                GlStateManager.disableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            });

        public static FiguraRenderType itemEntityTranslucentCull(ResourceLocation texture) {
            return FiguraRenderType.create("item_entity_translucent_cull", ENTITY_FORMAT, VertexFormatMode.QUADS, 256, true, true,
                    () -> {
                        // Equivalent to enabling TextureState
                        GlStateManager.enableTexture2D();
                        TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
                        textureManager.bindTexture(texture);
                        textureManager.getTexture(texture).setBlurMipmap(false, false);

                        // Equivalent to enabling TRANSLUCENT_TRANSPARENCY
                        GlStateManager.enableBlend();
                        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                        // Sets diffuse lighting
                        GlStateManager.enableLighting();
                        GlStateManager.enableColorMaterial();
                        GlStateManager.colorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE);
                        // Sets alpha
                        GlStateManager.enableAlpha();
                        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.003921569f);
                        // Set lightmap
                        Minecraft.getMinecraft().entityRenderer.enableLightmap();
                        // TODO set outline
                    }, () -> {
                        GlStateManager.disableBlend();
                        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                        GlStateManager.disableLighting();
                        GlStateManager.disableColorMaterial();
                        GlStateManager.disableAlpha();
                        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
                        Minecraft.getMinecraft().entityRenderer.disableLightmap();
                    });
        }

        public static FiguraRenderType outline(ResourceLocation texture) {
            return FiguraRenderType.create("outline", DefaultVertexFormats.POSITION_TEX_COLOR, VertexFormatMode.QUADS, 256,
                    () -> {
                        // Equivalent to enabling TextureState
                        GlStateManager.enableTexture2D();
                        TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
                        textureManager.bindTexture(texture);
                        textureManager.getTexture(texture).setBlurMipmap(false, false);
                        // Set lightmap
                        Minecraft.getMinecraft().entityRenderer.enableLightmap();
                        // Sets alpha
                        GlStateManager.enableAlpha();
                        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.003921569f);

                        GlStateManager.glTexEnvi(8960, 8704, 34160);
                        GlStateManager.glTexEnvi(8960, 34161, 7681);
                        GlStateManager.glTexEnvi(8960, 34176, 34168);
                        GlStateManager.glTexEnvi(8960, 34192, 768);
                    }, () -> {
                        Minecraft.getMinecraft().entityRenderer.disableLightmap();
                        GlStateManager.disableAlpha();
                        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
                        GlStateManager.disableOutlineMode();
                    });
        }

    }
}