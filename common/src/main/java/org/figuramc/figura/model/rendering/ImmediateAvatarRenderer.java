package org.figuramc.figura.model.rendering;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.ducks.extensions.Vector4fExtension;
import org.figuramc.figura.lua.api.ClientAPI;
import org.figuramc.figura.math.matrix.FiguraMat3;
import org.figuramc.figura.math.matrix.FiguraMat4;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.math.vector.FiguraVec4;
import org.figuramc.figura.model.*;
import org.figuramc.figura.model.rendering.texture.FiguraTexture;
import org.figuramc.figura.model.rendering.texture.FiguraTextureSet;
import org.figuramc.figura.model.rendering.texture.RenderTypes;
import org.figuramc.figura.model.rendertasks.RenderTask;
import org.figuramc.figura.utils.ColorUtils;
import org.figuramc.figura.utils.Pair;
import org.figuramc.figura.utils.ui.UIHelper;
import org.lwjgl.util.vector.Vector4f;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class ImmediateAvatarRenderer extends AvatarRenderer {

    protected final PartCustomization.PartCustomizationStack customizationStack = new PartCustomization.PartCustomizationStack();

    public static final FiguraMat4 VIEW_TO_WORLD_MATRIX = FiguraMat4.of();
    private static final PartCustomization pivotOffsetter = new PartCustomization();
    protected static final VertexBuffer VERTEX_BUFFER = new VertexBuffer();

    public ImmediateAvatarRenderer(Avatar avatar) {
        super(avatar);

        // Vertex data, read model parts
        root = FiguraModelPartReader.read(avatar, avatar.nbt.getCompoundTag("models"), textureSets, false);

        sortParts();
    }

    public void checkEmpty() {
        if (!customizationStack.isEmpty())
            throw new IllegalStateException("Customization stack not empty!");
    }

    @Override
    public int render() {
        return commonRender(1.5d);
    }

    @Override
    public int renderSpecialParts() {
        return commonRender(0);
    }

    @Override
    public void updateMatrices() {
        // flag rendering state
        this.isRendering = true;

        // setup root customizations
        PartCustomization customization = setupRootCustomization(1.5d);

        // Push transform
        customizationStack.push(customization);

        // world matrices
        VIEW_TO_WORLD_MATRIX.set(AvatarRenderer.worldToViewMatrix().invert());

        // calculate each part matrices
        calculatePartMatrices(root);

        // finish rendering
        customizationStack.pop();
        checkEmpty();

        this.isRendering = false;
    }

    protected int commonRender(double vertOffset) {
        // flag rendering state
        this.isRendering = true;

        // iris fix
        int irisConfig = UIHelper.paperdoll || !ClientAPI.hasShaderPackMod() ? 0 : Configs.IRIS_COMPATIBILITY_FIX.value;
        doIrisEmissiveFix = (irisConfig >= 2 && ClientAPI.hasShaderPack()) || (avatar.renderMode != EntityRenderMode.RENDER && avatar.renderMode != EntityRenderMode.WORLD);
        offsetRenderLayers = irisConfig >= 1;

        // custom textures
        for (FiguraTextureSet set : textureSets)
            set.uploadIfNeeded();
        for (FiguraTexture texture : customTextures.values())
            texture.uploadIfDirty();

        // Set shouldRenderPivots
        int config = Configs.RENDER_DEBUG_PARTS_PIVOT.value;
        if (!Minecraft.getMinecraft().getRenderManager().isDebugBoundingBox() || (!avatar.isHost && config < 2))
            shouldRenderPivots = 0;
        else
            shouldRenderPivots = config;

        // world matrices
        if (allowMatrixUpdate)
            VIEW_TO_WORLD_MATRIX.set(AvatarRenderer.worldToViewMatrix().invert());

        // complexity
        int prev = avatar.complexity.remaining;
        int[] remainingComplexity = new int[] {prev};

        // render all model parts
        if (root.customization.visible == null || root.customization.visible) {
            if (currentFilterScheme.parentType.isSeparate) {
                List<FiguraModelPart> parts = separatedParts.get(currentFilterScheme.parentType);
                if (parts != null) {
                    boolean renderLayer = !currentFilterScheme.parentType.isRenderLayer;
                    if (renderLayer) {
                        PartCustomization customization = setupRootCustomization(vertOffset);
                        customizationStack.push(customization); // push root
                        customizationStack.push(root.customization); // push "models"
                    }

                    for (FiguraModelPart part : parts) {
                        if (currentFilterScheme.parentType == ParentType.Item && part != itemToRender)
                            continue;

                        boolean saved = part.savedCustomization != null;
                        if (saved) {
                            customizationStack.push(part.savedCustomization);
                            part.savedCustomization = null;
                        }

                        renderPart(part, remainingComplexity, currentFilterScheme.initialValue);

                        if (saved) customizationStack.pop();
                    }

                    if (renderLayer) {
                        customizationStack.pop(); // pop "models"
                        customizationStack.pop(); // pop root
                    }
                }
            } else {
                PartCustomization customization = setupRootCustomization(vertOffset);
                customizationStack.push(customization);
                renderPart(root, remainingComplexity, currentFilterScheme.initialValue);
                customizationStack.pop();
            }

            // push vertices to vertex consumer
            FiguraMod.pushProfiler("draw");
            FiguraMod.pushProfiler("primary");
            VERTEX_BUFFER.consume(true, bufferSource);
            FiguraMod.popPushProfiler("secondary");
            VERTEX_BUFFER.consume(false, bufferSource);
            FiguraMod.popProfiler(2);

            // finish rendering
            checkEmpty();
        }

        this.isRendering = false;
        if (this.dirty)
            clean();

        return prev - Math.max(remainingComplexity[0], 0);
    }

    protected PartCustomization setupRootCustomization(double vertOffset) {
        PartCustomization customization = new PartCustomization();

        customization.setPrimaryRenderType(RenderTypes.TRANSLUCENT);
        customization.setSecondaryRenderType(RenderTypes.EMISSIVE);

        double s = 1.0 / 16;
        customization.positionMatrix.scale(s, s, s);
        customization.positionMatrix.rotateZ(180);
        customization.positionMatrix.translate(0, vertOffset, 0);
        customization.normalMatrix.rotateZ(180);

        customization.positionMatrix.multiply(posMat);
        customization.normalMatrix.multiply(normalMat);

        customization.light = light;
        customization.alpha = alpha;
        customization.overlay = overlay;

        customization.primaryTexture = new TextureCustomization(FiguraTextureSet.OverrideType.PRIMARY, null);
        customization.secondaryTexture = new TextureCustomization(FiguraTextureSet.OverrideType.SECONDARY, null);

        return customization;
    }

    protected boolean renderPart(FiguraModelPart part, int[] remainingComplexity, boolean prevPredicate) {
        FiguraMod.pushProfiler(part.name);

        PartCustomization custom = part.customization;

        // test the current filter scheme
        FiguraMod.pushProfiler("predicate");
        Boolean thisPassedPredicate = currentFilterScheme.test(part.parentType, prevPredicate);
        if (thisPassedPredicate == null || (custom.visible != null && !custom.visible)) {
            if (part.parentType.isRenderLayer)
                part.savedCustomization = customizationStack.peek();
            FiguraMod.popProfiler(2);
            return true;
        }

        // calculate part transforms

        // calculate vanilla parent
        FiguraMod.popPushProfiler("copyVanillaPart");
        part.applyVanillaTransforms(vanillaModelData);
        part.applyExtraTransforms(customizationStack.peek());

        // visibility
        FiguraMod.popPushProfiler("checkVanillaVisible");
        if (!ignoreVanillaVisibility && custom.vanillaVisible != null && !custom.vanillaVisible) {
            FiguraMod.popPushProfiler("removeVanillaTransforms");
            part.resetVanillaTransforms();
            FiguraMod.popProfiler(2);
            return true;
        }

        // pre render function
        if (part.preRender != null) {
            FiguraMod.popPushProfiler("preRenderFunction");
            avatar.run(part.preRender, avatar.render, tickDelta, avatar.renderMode.name(), part);
        }

        // recalculate stuff
        FiguraMod.popPushProfiler("calculatePartMatrices");
        custom.recalculate();

        // void blocked matrices
        // that's right, check only for previous predicate
        FiguraMat4 positionCopy = null;
        FiguraMat3 normalCopy = null;
        boolean voidMatrices = !allowHiddenTransforms && !prevPredicate;
        if (voidMatrices) {
            FiguraMod.popPushProfiler("clearMatrices");
            positionCopy = custom.positionMatrix.copy();
            normalCopy = custom.normalMatrix.copy();
            custom.positionMatrix.reset();
            custom.normalMatrix.reset();
        }

        // push stack
        FiguraMod.popPushProfiler("pushCustomizationStack");
        customizationStack.push(custom);

        // restore variables
        if (voidMatrices) {
            FiguraMod.popPushProfiler("restoreMatrices");
            custom.positionMatrix.set(positionCopy);
            custom.normalMatrix.set(normalCopy);
        }

        if (thisPassedPredicate) {
            // recalculate world matrices
            FiguraMod.popPushProfiler("worldMatrices");
            if (allowMatrixUpdate) {
                FiguraMat4 mat = partToWorldMatrices(custom);
                part.savedPartToWorldMat.set(mat);
            }

            // recalculate light
            FiguraMod.popPushProfiler("calculateLight");
            World l;
            if (custom.light != null)
                updateLight = false;
            else if (updateLight && (l = Minecraft.getMinecraft().world) != null) {
                FiguraVec3 pos = part.savedPartToWorldMat.apply(0d, 0d, 0d);
                int block = l.getLightFor(EnumSkyBlock.BLOCK, pos.asBlockPos());
                int sky = l.getLightFor(EnumSkyBlock.SKY, pos.asBlockPos());
                customizationStack.peek().light = block << 4 | sky << 20;
            }
        }

        // mid render function
        if (part.midRender != null) {
            FiguraMod.popPushProfiler("midRenderFunction");
            avatar.run(part.midRender, avatar.render, tickDelta, avatar.renderMode.name(), part);
        }

        // render this
        FiguraMod.popPushProfiler("pushVertices");
        boolean breakRender = thisPassedPredicate && !part.pushVerticesImmediate(this, remainingComplexity);

        // render extras
        FiguraMod.popPushProfiler("extras");
        if (!breakRender && thisPassedPredicate) {
            boolean renderPivot = shouldRenderPivots > 0;
            boolean renderTasks = !part.renderTasks.isEmpty();
            boolean renderPivotParts = part.parentType.isPivot && allowPivotParts;

            if (renderPivot || renderTasks || renderPivotParts) {
                // fix pivots
                FiguraMod.pushProfiler("fixMatricesPivot");

                FiguraVec3 pivot = custom.getPivot().copy().add(custom.getOffsetPivot());
                pivotOffsetter.setPos(pivot);
                pivotOffsetter.recalculate();
                customizationStack.push(pivotOffsetter);

                PartCustomization peek = customizationStack.peek();

                // render pivot indicators
                if (renderPivot) {
                    FiguraMod.popPushProfiler("renderPivotCube");
                    renderPivot(part, peek);
                }

                // render tasks
                if (renderTasks) {
                    FiguraMod.popPushProfiler("renderTasks");
                    int light = peek.light;
                    int overlay = peek.overlay;
                    allowSkullRendering = false;
                    for (RenderTask task : part.renderTasks.values()) {
                        if (!task.shouldRender())
                            continue;
                        int neededComplexity = task.getComplexity();
                        if (neededComplexity > remainingComplexity[0])
                            break;
                        FiguraMod.pushProfiler(task.getName());
                        task.renderTask(customizationStack, bufferSource, light, overlay);
                        remainingComplexity[0] -= neededComplexity;
                        FiguraMod.popProfiler();
                    }
                    allowSkullRendering = true;
                }

                // render pivot parts
                if (renderPivotParts && part.parentType.isPivot) {
                    FiguraMod.popPushProfiler("savePivotParts");
                    savePivotTransform(part.parentType, peek);
                }

                customizationStack.pop();
                FiguraMod.popProfiler();
            }
        }

        // render children
        FiguraMod.popPushProfiler("children");
        for (FiguraModelPart child : new ArrayList<>(part.children)) {
            if (!renderPart(child, remainingComplexity, thisPassedPredicate)) {
                breakRender = true;
                break;
            }
        }

        // reset the parent
        FiguraMod.popPushProfiler("removeVanillaTransforms");
        part.resetVanillaTransforms();

        // post render function
        if (part.postRender != null) {
            FiguraMod.popPushProfiler("postRenderFunction");
            avatar.run(part.postRender, avatar.render, tickDelta, avatar.renderMode.name(), part);
        }

        // pop
        customizationStack.pop();
        FiguraMod.popProfiler(2);

        return !breakRender;
    }

    protected void renderPivot(FiguraModelPart part, PartCustomization customization) {
        boolean group = part.customization.partType == PartCustomization.PartType.GROUP;
        FiguraVec3 color = group ? ColorUtils.Colors.BLUE.vec : ColorUtils.Colors.AWESOME_BLUE.vec;
        double boxSize = group ? 1 / 16d : 1 / 32d;
        boxSize /= Math.max(Math.cbrt(part.savedPartToWorldMat.det()), 0.02);

        Matrix4f pos = customization.copyIntoGlobalPoseStack();
        Vector4f vector4f = new Vector4f((float) boxSize, (float) boxSize, (float) boxSize, 1.0f);
        ((Vector4fExtension)vector4f).figura$transform(pos);

        RenderGlobal.drawBoundingBox(bufferSource.getBuffer(RenderTypes.FiguraRenderType.LINES),
                -vector4f.x, -vector4f.y, -vector4f.z,
                vector4f.x, vector4f.y, vector4f.z,
                (float) color.x, (float) color.y, (float) color.z, 1f);
    }

    protected void savePivotTransform(ParentType parentType, PartCustomization customization) {
        FiguraMat4 currentPosMat = customization.getPositionMatrix();
        FiguraMat3 currentNormalMat = customization.getNormalMatrix();
        ConcurrentLinkedQueue<Pair<FiguraMat4, FiguraMat3>> queue = pivotCustomizations.computeIfAbsent(parentType, p -> new ConcurrentLinkedQueue<>());
        queue.add(new Pair<>(currentPosMat, currentNormalMat)); // These are COPIES, so ok to add
    }

    protected FiguraMat4 partToWorldMatrices(PartCustomization cust) {
        FiguraMat4 customizePeek = customizationStack.peek().positionMatrix.copy();
        customizePeek.multiply(VIEW_TO_WORLD_MATRIX);
        FiguraVec3 piv = cust.getPivot();

        FiguraMat4 translation = FiguraMat4.of();
        translation.translate(piv);
        customizePeek.rightMultiply(translation);

        return customizePeek;
    }

    protected void calculatePartMatrices(FiguraModelPart part) {
        FiguraMod.pushProfiler(part.name);

        PartCustomization custom = part.customization;

        // Store old visibility, but overwrite it in case we only want to render certain parts
        FiguraMod.pushProfiler("predicate");
        Boolean thisPassedPredicate = currentFilterScheme.test(part.parentType, true);
        if (thisPassedPredicate == null) {
            FiguraMod.popProfiler(2);
            return;
        }

        // calculate part transforms

        // calculate vanilla parent
        FiguraMod.popPushProfiler("copyVanillaPart");
        part.applyVanillaTransforms(vanillaModelData);
        part.applyExtraTransforms(customizationStack.peek());

        // push customization stack
        FiguraMod.popPushProfiler("calculatePartMatrices");
        custom.recalculate();
        FiguraMod.popPushProfiler("applyOnStack");
        customizationStack.push(custom);

        // render extras
        if (thisPassedPredicate) {
            // part to world matrices
            FiguraMod.popPushProfiler("worldMatrices");
            FiguraMat4 mat = partToWorldMatrices(custom);
            part.savedPartToWorldMat.set(mat);
        }

        // render children
        FiguraMod.popPushProfiler("children");
        for (FiguraModelPart child : part.children)
            calculatePartMatrices(child);

        // reset the parent
        part.resetVanillaTransforms();

        // pop
        customizationStack.pop();
        FiguraMod.popProfiler(2);
    }

    public void pushFaces(int faceCount, int[] remainingComplexity, FiguraTextureSet textureSet, List<Vertex> vertices) {
        // Handle cases that we can quickly
        if (faceCount == 0 || vertices.isEmpty())
            return;

        PartCustomization customization = customizationStack.peek();

        VertexData primary = getTexture(customization, textureSet, true);
        VertexData secondary = getTexture(customization, textureSet, false);

        if (primary.renderType == null && secondary.renderType == null) {
            remainingComplexity[0] += faceCount;
            return;
        }

        if (primary.renderType != null)
            pushToBuffer(faceCount, primary, customization, textureSet, vertices);
        if (secondary.renderType != null)
            pushToBuffer(faceCount, secondary, customization, textureSet, vertices);
    }

    private VertexData getTexture(PartCustomization customization, FiguraTextureSet textureSet, boolean primary) {
        RenderTypes types = primary ? customization.getPrimaryRenderType() : customization.getSecondaryRenderType();
        TextureCustomization texture = primary ? customization.primaryTexture : customization.secondaryTexture;
        VertexData ret = new VertexData();

        if (types == RenderTypes.NONE)
            return ret;

        // get texture
        ResourceLocation id = textureSet.getOverrideTexture(avatar.owner, texture);

        // color
        ret.color = primary ? customization.color : customization.color2;

        // primary
        ret.primary = primary;

        // get render type
        if (id != null) {
            if (translucent) {
                ret.renderType = RenderTypes.FiguraRenderType.itemEntityTranslucentCull(id);
                return ret;
            }
            if (glowing) {
                ret.renderType = RenderTypes.FiguraRenderType.outline(id);
                return ret;
            }
        }

        if (types == null)
            return ret;

        if (offsetRenderLayers && !primary && types.isOffset())
            ret.vertexOffset = FiguraMod.VERTEX_OFFSET;

        // Switch to cutout with fullbright if the iris emissive fix is enabled
        if (doIrisEmissiveFix && types == RenderTypes.EMISSIVE) {
            ret.fullBright = true;
            ret.renderType = RenderTypes.TRANSLUCENT_CULL.get(id);
        } else {
            ret.renderType = types.get(id);
        }

        if (ret.renderType == RenderTypes.END_PORTAL.get(id)) {
            ret.endRenderType = 1;
        } else if (ret.renderType == RenderTypes.END_GATEWAY.get(id)) {
            ret.endRenderType = 2;
        }else if (ret.renderType == RenderTypes.TEXTURED_PORTAL.get(id)) {
            ret.endRenderType = 3;
        } else {
            ret.endRenderType = 0;
        }

        ret.texture = id;

        return ret;
    }

    private static final FiguraVec4 pos = FiguraVec4.of();
    private static final FiguraVec3 normal = FiguraVec3.of();
    private static final FiguraVec3 uv = FiguraVec3.of(0, 0, 1);
    private static final List<RenderTypes.FiguraRenderType> END_RENDER_TYPES = IntStream.range(0, 16).mapToObj(i -> RenderTypes.FiguraRenderType.getEndPortal(i + 1)).collect(ImmutableList.toImmutableList());
    private static final Map<Pair<Integer, ResourceLocation>, RenderTypes.FiguraRenderType> TEXTURED_END_RENDER_TYPES = new HashMap<>();
    private void pushToBuffer(int faceCount, VertexData vertexData, PartCustomization customization, FiguraTextureSet textureSet, List<Vertex> vertices) {
        int vertCount = faceCount * 4;

        FiguraVec3 uvFixer = FiguraVec3.of();
        uvFixer.set(textureSet.getWidth(), textureSet.getHeight(), 1); // Dividing by this makes uv 0 to 1

        int overlay = customization.overlay;
        int light = vertexData.fullBright ? 15 << 20 | 15 << 4 : customization.light;
        Random RANDOM = new Random(31100L);

        if (vertexData.endRenderType >= 1) {
            // Magical values from TheEndPortalRenderer
            float r = (RANDOM.nextFloat() * 0.5f + 0.1f) * 0.15f;
            float g = (RANDOM.nextFloat() * 0.5f + 0.4f) * 0.15f;
            float b = (RANDOM.nextFloat() * 0.5f + 0.5f) * 0.15f;
            float finalR1 = r;
            float finalG1 = g;
            float finalB1 = b;
            VERTEX_BUFFER.getBufferFor(vertexData.renderType, vertexData.primary, vertexConsumer -> {
                vertexData.renderType.setupState.run();
                vertexConsumer.begin(vertexData.renderType.mode, vertexData.renderType.format);
                // Initial render with the end colors
                for (int vert = 0; vert < vertCount; vert++) {
                    Vertex vertex = vertices.get(vert);

                    pos.set(vertex.x, vertex.y, vertex.z, 1);
                    pos.transform(customization.positionMatrix);
                    pos.add(pos.normalized().scale(vertexData.vertexOffset));
                    normal.set(vertex.nx, vertex.ny, vertex.nz);
                    normal.transform(customization.normalMatrix);
                    uv.set(vertex.u, vertex.v, 1);
                    uv.divide(uvFixer);
                    uv.transform(customization.uvMatrix);

                    vertexConsumer
                            .pos(pos.x, pos.y, pos.z)
                            .color(finalR1, finalG1, finalB1, customization.alpha)
                            .tex((float) uv.x, (float) uv.y)
                            .lightmap(light & 0xFFFF, light >> 16 & 0xFFFF)
                            .normal((float) normal.x, (float) normal.y, (float) normal.z)
                            .endVertex();
                }
                vertexConsumer.finishDrawing();
                vertexData.renderType.clearState.run();
            });
            int passes = vertexData.endRenderType == 2 ? 16 : 15;
            // Re render to get the portal effect, there's an extra pass on the gateway
            for (int i = 1; i < passes; ++i) {
                float color = 2.0f / (float)(18 - i);
                r = (RANDOM.nextFloat() * 0.5f + 0.1f) * color;
                g = (RANDOM.nextFloat() * 0.5f + 0.4f) * color;
                b = (RANDOM.nextFloat() * 0.5f + 0.5f) * color;
                float finalR = r;
                float finalG = g;
                float finalB = b;

                RenderTypes.FiguraRenderType renderType;
                if (vertexData.endRenderType == 3){
                    renderType = TEXTURED_END_RENDER_TYPES.computeIfAbsent(new Pair<>(i, vertexData.texture), integerResourceLocationPair -> RenderTypes.FiguraRenderType.getTexturedPortal(integerResourceLocationPair.getSecond(), integerResourceLocationPair.getFirst()));
                } else {
                    renderType = END_RENDER_TYPES.get(i);
                }
                VERTEX_BUFFER.getBufferFor(renderType, vertexData.primary, vertexConsumer -> {
                    vertexData.renderType.setupState.run();
                    vertexConsumer.begin(vertexData.renderType.mode, vertexData.renderType.format);

                    for (int vert = 0; vert < vertCount; vert++) {
                        Vertex vertex = vertices.get(vert);

                        pos.set(vertex.x, vertex.y, vertex.z, 1);
                        pos.transform(customization.positionMatrix);
                        pos.add(pos.normalized().scale(vertexData.vertexOffset));
                        normal.set(vertex.nx, vertex.ny, vertex.nz);
                        normal.transform(customization.normalMatrix);
                        uv.set(vertex.u, vertex.v, 1);
                        uv.divide(uvFixer);
                        uv.transform(customization.uvMatrix);

                        vertexConsumer
                                .pos(pos.x, pos.y, pos.z)
                                .color(finalR, finalG, finalB, customization.alpha)
                                .tex((float) uv.x, (float) uv.y)
                                .lightmap(light & 0xFFFF, light >> 16 & 0xFFFF)
                                .normal((float) normal.x, (float) normal.y, (float) normal.z)
                                .endVertex();
                    }
                    vertexConsumer.finishDrawing();
                    vertexData.renderType.clearState.run();
                });
            }
        } else {
            VERTEX_BUFFER.getBufferFor(vertexData.renderType, vertexData.primary, vertexConsumer -> {
                vertexData.renderType.setupState.run();
                vertexConsumer.begin(vertexData.renderType.mode, vertexData.renderType.format);

                for (int i = 0; i < vertCount; i++) {
                    Vertex vertex = vertices.get(i);

                    pos.set(vertex.x, vertex.y, vertex.z, 1);
                    pos.transform(customization.positionMatrix);
                    pos.add(pos.normalized().scale(vertexData.vertexOffset));
                    normal.set(vertex.nx, vertex.ny, vertex.nz);
                    normal.transform(customization.normalMatrix);
                    uv.set(vertex.u, vertex.v, 1);
                    uv.divide(uvFixer);
                    uv.transform(customization.uvMatrix);

                    vertexConsumer
                            .pos(pos.x, pos.y, pos.z)
                            .color((float) vertexData.color.x, (float) vertexData.color.y, (float) vertexData.color.z, customization.alpha)
                            .tex((float) uv.x, (float) uv.y)
                            .lightmap(light & 0xFFFF, light >> 16 & 0xFFFF)
                            .normal((float) normal.x, (float) normal.y, (float) normal.z)
                            .endVertex();
                }
                vertexConsumer.finishDrawing();
                vertexData.renderType.clearState.run();
            });
        }
    }

    private static class VertexData {
        public RenderTypes.FiguraRenderType renderType;
        public boolean fullBright;
        public float vertexOffset;
        public FiguraVec3 color;
        public boolean primary;
        // EndPortal = 1; EndGateway = 2; FiguraPortal = 3; None = 0;
        public int endRenderType;
        public ResourceLocation texture;
    }

    private static class VertexBuffer {
        private final HashMap<RenderTypes.FiguraRenderType, List<Consumer<BufferBuilder>>> primaryBuffers = new LinkedHashMap<>();
        private final HashMap<RenderTypes.FiguraRenderType, List<Consumer<BufferBuilder>>> secondaryBuffers = new LinkedHashMap<>();

        public void getBufferFor(RenderTypes.FiguraRenderType renderType, boolean primary, Consumer<BufferBuilder> consumer) {
            HashMap<RenderTypes.FiguraRenderType, List<Consumer<BufferBuilder>>> buffer = primary ? primaryBuffers : secondaryBuffers;
            List<Consumer<BufferBuilder>> list = buffer.computeIfAbsent(renderType, renderType1 -> new ArrayList<>());
            list.add(consumer);
        }

        public void consume(boolean primary, RenderTypes.FiguraBufferSource bufferSource) {
            HashMap<RenderTypes.FiguraRenderType, List<Consumer<BufferBuilder>>> map = primary ? primaryBuffers : secondaryBuffers;
            for (Map.Entry<RenderTypes.FiguraRenderType, List<Consumer<BufferBuilder>>> entry : map.entrySet()) {
                BufferBuilder vertexConsumer = bufferSource.getBuffer(entry.getKey());
                List<Consumer<BufferBuilder>> consumers = entry.getValue();
                for (Consumer<BufferBuilder> consumer : consumers)
                    consumer.accept(vertexConsumer);
            }
            map.clear();
        }
    }
}
