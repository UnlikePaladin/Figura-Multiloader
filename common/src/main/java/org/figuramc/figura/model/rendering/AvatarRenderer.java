package org.figuramc.figura.model.rendering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.Vec3d;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.math.matrix.FiguraMat3;
import org.figuramc.figura.math.matrix.FiguraMat4;
import org.figuramc.figura.model.FiguraModelPart;
import org.figuramc.figura.model.ParentType;
import org.figuramc.figura.model.VanillaModelData;
import org.figuramc.figura.model.rendering.texture.FiguraTexture;
import org.figuramc.figura.model.rendering.texture.FiguraTextureSet;
import org.figuramc.figura.model.rendering.texture.RenderTypes;
import org.figuramc.figura.utils.MathUtils;
import org.figuramc.figura.utils.NbtType;
import org.figuramc.figura.utils.Pair;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix3f;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Mainly exists as an abstract superclass for VAO-based and
 * immediate mode avatar renderers. (VAO-based don't exist yet)
 */
public abstract class AvatarRenderer {

    protected final Avatar avatar;
    public FiguraModelPart root;

    protected final Map<ParentType, List<FiguraModelPart>> separatedParts = new ConcurrentHashMap<>();

    protected boolean isRendering, dirty;

    // -- rendering data -- //

    // entity
    public Entity entity;
    public float yaw, tickDelta;
    public int light;
    public int overlay;
    public float alpha;
    public boolean translucent, glowing;
    public FiguraMat4 posMat = FiguraMat4.of();
    public FiguraMat3 normalMat = FiguraMat3.of();

    // matrices
    public RenderTypes.FiguraBufferSource bufferSource;
    public VanillaModelData vanillaModelData = new VanillaModelData();

    public PartFilterScheme currentFilterScheme;
    public final HashMap<ParentType, ConcurrentLinkedQueue<Pair<FiguraMat4, FiguraMat3>>> pivotCustomizations = new HashMap<>(ParentType.values().length);
    protected final List<FiguraTextureSet> textureSets = new ArrayList<>();
    public final HashMap<String, FiguraTexture> textures = new HashMap<>();
    public final HashMap<String, FiguraTexture> customTextures = new HashMap<>();
    protected static int shouldRenderPivots;
    public boolean allowMatrixUpdate = false;
    public boolean allowHiddenTransforms = true;
    public boolean allowSkullRendering = true;
    public boolean allowPivotParts = true;
    public boolean updateLight = false;
    public boolean doIrisEmissiveFix = false;
    public boolean offsetRenderLayers = false;
    public boolean ignoreVanillaVisibility = false;
    public FiguraModelPart itemToRender;

    public AvatarRenderer(Avatar avatar) {
        this.avatar = avatar;

        // textures

        NBTTagCompound nbt = avatar.nbt.getCompoundTag("textures");
        NBTTagCompound src = nbt.getCompoundTag("src");

        // src files
        for (String key : src.getKeySet()) {
            byte[] bytes = src.getByteArray(key);
            if (bytes.length > 0) {
                textures.put(key, new FiguraTexture(avatar, key, bytes));
            } else {
                NBTTagList size = src.getTagList(key, NbtType.INT.getValue());
                textures.put(key, new FiguraTexture(avatar, key, size.getIntAt(0), size.getIntAt(1)));
            }
        }

        // data files
        NBTTagList texturesList = nbt.getTagList("data", NbtType.COMPOUND.getValue());
        for (int i = 0; i < texturesList.tagCount(); i++) {
            NBTTagCompound tag = texturesList.getCompoundTagAt(i);
            textureSets.add(new FiguraTextureSet(
                    getTextureName(tag),
                    textures.get(tag.getString("d")),
                    textures.get(tag.getString("e")),
                    textures.get(tag.getString("s")),
                    textures.get(tag.getString("n"))
            ));
        }

        avatar.hasTexture = !texturesList.hasNoTags();
    }

    private String getTextureName(NBTTagCompound tag) {
        String s = tag.getString("d");
        if (!s.isEmpty()) return s;
        s = tag.getString("e");
        if (!s.isEmpty()) return s.substring(0, s.length() - 2);
        s = tag.getString("s");
        if (!s.isEmpty()) return s.substring(0, s.length() - 2);
        s = tag.getString("n");
        if (!s.isEmpty()) return s.substring(0, s.length() - 2);
        return "";
    }

    public FiguraTexture getTexture(String name) {
        FiguraTexture texture = customTextures.get(name);
        if (texture != null)
            return texture;

        for (Map.Entry<String, FiguraTexture> entry : textures.entrySet()) {
            if (entry.getKey().equals(name))
                return entry.getValue();
        }

        return null;
    }

    public abstract int render();
    public abstract int renderSpecialParts();
    public abstract void updateMatrices();

    protected void clean() {
        for (FiguraTextureSet set : textureSets)
            set.clean();
        for (FiguraTexture texture : customTextures.values())
            texture.deleteGlTexture();
    }

    public void invalidate() {
        this.dirty = true;
        if (!this.isRendering)
            clean();
    }

    public void sortParts() {
        separatedParts.clear();
        _sortParts(root);
    }

    private void _sortParts(FiguraModelPart part) {
        if (part.parentType.isSeparate) {
            List<FiguraModelPart> list = separatedParts.computeIfAbsent(part.parentType, parentType -> new ArrayList<>());
            list.add(part);
        }

        for (FiguraModelPart child : part.children)
            _sortParts(child);
    }

    /**
     * Returns the matrix for an entity, used to transform from entity space to world space.
     * @param e The entity to get the matrix for.
     * @return A matrix which represents the transformation from entity space to part space.
     */
    public static FiguraMat4 entityToWorldMatrix(Entity e, float delta) {
        double yaw = e instanceof EntityLivingBase ? MathUtils.lerp(delta, ((EntityLivingBase) e).prevRenderYawOffset, ((EntityLivingBase) e).renderYawOffset) : e.getRotationYawHead();
        FiguraMat4 result = FiguraMat4.of();
        result.rotateX(180 - yaw);
        double d = MathUtils.lerp(delta, e.prevPosX, e.posX);
        double g = MathUtils.lerp(delta, e.prevPosY, e.posY);
        double f = MathUtils.lerp(delta, e.prevPosZ, e.posZ);
        result.translate(d, g, f);
        return result;
    }

    public static double getYawOffsetRot(Entity e, float delta) {
        double yaw = e instanceof EntityLivingBase ? MathUtils.lerp(delta, ((EntityLivingBase) e).prevRenderYawOffset, ((EntityLivingBase) e).renderYawOffset) : e.getRotationYawHead();
        return 180 - yaw;
    }

    /**
     * Gets a matrix to transform from world space to view space, based on the
     * player's camera position and orientation.
     * @return That matrix.
     */
    public static FiguraMat4 worldToViewMatrix() {
        Minecraft client = Minecraft.getMinecraft();
        RenderManager manager = client.getRenderManager();
        Matrix3f cameraMat3f = new Matrix3f();
        cameraMat3f.setIdentity();

        float cosX = (float) Math.cos(manager.playerViewX);
        float sinX = (float) Math.sin(manager.playerViewX);
        float cosY = (float) Math.cos(manager.playerViewY);
        float sinY = (float) Math.sin(manager.playerViewY);

        cameraMat3f.m00 = cosY;
        cameraMat3f.m01 = -sinX * sinY;
        cameraMat3f.m02 = cosX * sinY;

        cameraMat3f.m10 = 0.0f;
        cameraMat3f.m11 = cosX;
        cameraMat3f.m12 = sinX;

        cameraMat3f.m20 = -sinY;
        cameraMat3f.m21 = -cosY * sinX;
        cameraMat3f.m22 = cosX * cosY;

        cameraMat3f.invert();
        FiguraMat4 result = FiguraMat4.of();
        Vec3d cameraPos = new Vec3d(manager.viewerPosX, manager.viewerPosY, manager.viewerPosZ).scale(-1);
        result.translate(cameraPos.x, cameraPos.y, cameraPos.z);
        FiguraMat3 cameraMat = FiguraMat3.of().set(cameraMat3f);
        result.multiply(cameraMat.augmented());
        result.scale(-1, 1, -1);
        return result;
    }

    public void setupRenderer(PartFilterScheme currentFilterScheme, RenderTypes.FiguraBufferSource bufferSource, float tickDelta, int light, float alpha, int overlay, boolean translucent, boolean glowing, double camX, double camY, double camZ) {
        this.setupRenderer(currentFilterScheme, bufferSource, tickDelta, light, alpha, overlay, translucent, glowing);
        this.setMatrices(camX, camY, camZ);
    }

    public void setupRenderer(PartFilterScheme currentFilterScheme, RenderTypes.FiguraBufferSource bufferSource, float tickDelta, int light, float alpha, int overlay, boolean translucent, boolean glowing) {
        this.currentFilterScheme = currentFilterScheme;
        this.bufferSource = bufferSource;
        this.tickDelta = tickDelta;
        this.light = light;
        this.alpha = alpha;
        this.overlay = overlay;
        this.translucent = translucent;
        this.glowing = glowing;
    }

    public static FloatBuffer posBuf = BufferUtils.createFloatBuffer(16);
    public void setMatrices() {
        GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, posBuf);
        Matrix4f transformedPos = new Matrix4f();
        transformedPos.load(posBuf);
        this.posMat.set(transformedPos);
    }


    public void setMatrices(double camX, double camY, double camZ) {
        // pos
//        Matrix4f posMat = new Matrix4f(matrices.last().pose());
        GlStateManager.translate(-camX, -camY, -camZ);
        GlStateManager.scale(-1, -1, 1);
        setMatrices();

     //   posMat.multiply(Matrix4f.createTranslateMatrix((float) -camX, (float) -camY, (float) -camZ));
       // posMat.multiply(Matrix4f.createScaleMatrix(-1, -1, 1));
     //   this.posMat.set(posMat);

        // normal
        //Matrix3f normalMat = new Matrix3f(matrices.last().normal());
       // normalMat.mul(Matrix3f.createScaleMatrix(-1, -1, 1));
        //this.normalMat.set(normalMat);
    }
}
