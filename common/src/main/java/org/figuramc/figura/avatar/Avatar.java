package org.figuramc.figura.avatar;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.animation.Animation;
import org.figuramc.figura.animation.AnimationPlayer;
import org.figuramc.figura.backend2.NetworkStuff;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.ducks.extensions.Vector3fExtension;
import org.figuramc.figura.lua.FiguraLuaPrinter;
import org.figuramc.figura.lua.FiguraLuaRuntime;
import org.figuramc.figura.lua.api.TextureAPI;
import org.figuramc.figura.lua.api.data.FiguraBuffer;
import org.figuramc.figura.lua.api.entity.EntityAPI;
import org.figuramc.figura.lua.api.net.FiguraSocket;
import org.figuramc.figura.lua.api.particle.ParticleAPI;
import org.figuramc.figura.lua.api.ping.PingArg;
import org.figuramc.figura.lua.api.ping.PingFunction;
import org.figuramc.figura.lua.api.sound.SoundAPI;
import org.figuramc.figura.lua.api.world.BlockStateAPI;
import org.figuramc.figura.lua.api.world.ItemStackAPI;
import org.figuramc.figura.math.matrix.FiguraMat3;
import org.figuramc.figura.math.matrix.FiguraMat4;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.mixin.sound.SoundHandlerAccessor;
import org.figuramc.figura.model.FiguraModelPart;
import org.figuramc.figura.model.ParentType;
import org.figuramc.figura.model.PartCustomization;
import org.figuramc.figura.model.rendering.AvatarRenderer;
import org.figuramc.figura.model.rendering.EntityRenderMode;
import org.figuramc.figura.model.rendering.ImmediateAvatarRenderer;
import org.figuramc.figura.model.rendering.PartFilterScheme;
import org.figuramc.figura.model.rendering.texture.FiguraTexture;
import org.figuramc.figura.model.rendering.texture.RenderTypes;
import org.figuramc.figura.permissions.PermissionManager;
import org.figuramc.figura.permissions.PermissionPack;
import org.figuramc.figura.permissions.Permissions;
import org.figuramc.figura.utils.*;
import org.figuramc.figura.utils.ui.UIHelper;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Quaternion;
import paulscode.sound.SoundBuffer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

// the avatar class
// contains all things related to the avatar
// and also related to the owner, like its permissions
public class Avatar {

    private static CompletableFuture<Void> tasks;
    public static boolean firstPerson;

    // properties
    public final UUID owner;
    public final Class<? extends Entity> entityType;
    public NBTTagCompound nbt;
    public boolean loaded = true;
    public final boolean isHost;

    //metadata
    public String name, entityName;
    public String authors;
    public Version version;
    public String id;
    public int fileSize;
    public String color;
    public Map<String, String> badgeToColor = new HashMap<>();
    public Map<String, byte[]> resources = new HashMap<>();

    public boolean minify;

    // Runtime data
    private final Queue<Runnable> events = new ConcurrentLinkedQueue<>();
    public final ArrayList<FiguraSocket> openSockets = new ArrayList<>();
    public final ArrayList<FiguraBuffer> openBuffers = new ArrayList<>();
    public AvatarRenderer renderer;
    public FiguraLuaRuntime luaRuntime;
    public EntityRenderMode renderMode = EntityRenderMode.OTHER;

    public final PermissionPack.PlayerPermissionPack permissions;

    public final Map<String, byte[]> customSounds = new HashMap<>();
    public final Map<Integer, Animation> animations = new HashMap<>();

    // runtime status
    public boolean hasTexture, scriptError;
    public ITextComponent errorText;
    public Set<Permissions> noPermissions = new HashSet<>();
    public Set<Permissions> permissionsToTick = new HashSet<>();
    public int lastPlayingSound = 0;
    public int versionStatus = 0;

    // limits
    public int animationComplexity;
    public final Instructions complexity;
    public final Instructions init, render, worldRender, tick, worldTick, animation;
    public final RefilledNumber particlesRemaining, soundsRemaining;
    private Avatar(UUID owner, Class<? extends Entity> type, String name) {
        this.owner = owner;
        this.entityType = type;
        this.isHost = type == EntityPlayer.class && FiguraMod.isLocal(owner);
        this.permissions = type == EntityPlayer.class ? PermissionManager.get(owner) : PermissionManager.getMobPermissions(owner);
        this.complexity = new Instructions(permissions.get(Permissions.COMPLEXITY));
        this.init = new Instructions(permissions.get(Permissions.INIT_INST));
        this.render = new Instructions(permissions.get(Permissions.RENDER_INST));
        this.worldRender = new Instructions(permissions.get(Permissions.WORLD_RENDER_INST));
        this.tick = new Instructions(permissions.get(Permissions.TICK_INST));
        this.worldTick = new Instructions(permissions.get(Permissions.WORLD_TICK_INST));
        this.animation = new Instructions(permissions.get(Permissions.ANIMATION_INST));
        this.particlesRemaining = new RefilledNumber(permissions.get(Permissions.PARTICLES));
        this.soundsRemaining = new RefilledNumber(permissions.get(Permissions.SOUNDS));
        this.entityName = name == null ? "" : name;
    }

    public Avatar(UUID owner) {
        this(owner, EntityPlayer.class, EntityUtils.getNameForUUID(owner));
    }

    public Avatar(Entity entity) {
        this(entity.getUniqueID(), entity.getClass(), entity.getName());
    }

    public void load(NBTTagCompound nbt) {
        Runnable toRun = () -> {
            this.nbt = nbt;
            loaded = false;
        };

        if (tasks == null || tasks.isDone()) {
            tasks = CompletableFuture.runAsync(toRun);
        } else {
            tasks.thenRun(toRun);
        }

        tasks.join();

        if (nbt == null) {
            loaded = true;
            return;
        }

        tasks.thenRun(() -> {
            try {
                // metadata
                NBTTagCompound metadata = nbt.getCompoundTag("metadata");
                name = metadata.getString("name");
                authors = metadata.getString("authors");
                version = new Version(metadata.getString("ver"));
                if (metadata.hasKey("id"))
                    id = metadata.getString("id");
                if (metadata.hasKey("color"))
                    color = metadata.getString("color");
                if (metadata.hasKey("minify"))
                    minify = metadata.getBoolean("minify");
                if (nbt.hasKey("resources")) {
                    NBTTagCompound res = nbt.getCompoundTag("resources");
                    for (String k :
                            res.getKeySet()) {
                        resources.put(k, res.getByteArray(k));
                    }
                }
                for (String key : metadata.getKeySet()) {
                    if (key.contains("badge_color_")) {
                        badgeToColor.put(key.replace("badge_color_", ""), metadata.getString(key));
                    }
                }
                fileSize = getFileSize();
                versionStatus = getVersionStatus();
                if (entityName.trim().isEmpty())
                    entityName = name;

                // animations and models
                loadAnimations();
                renderer = new ImmediateAvatarRenderer(this);

                // sounds and script
                loadCustomSounds();
                createLuaRuntime();
            } catch (Exception e) {
                FiguraMod.LOGGER.error("", e);
                clean();
                this.nbt = null;
                this.renderer = null;
                this.luaRuntime = null;
            }

            loaded = true;
        });
    }

    public void tick() {
        if (scriptError || luaRuntime == null || !loaded)
            return;

        // fetch this avatar entity
        if (luaRuntime.getUser() == null) {
            Entity entity = EntityUtils.getEntityByUUID(owner);
            if (entity != null) {
                luaRuntime.setUser(entity);
                run("ENTITY_INIT", init.post());
            }
        }

        // tick permissions
        for (Permissions t : permissionsToTick) {
            if (permissions.get(t) > 0) {
                noPermissions.remove(t);
            } else {
                noPermissions.add(t);
            }
        }
        if (lastPlayingSound > 0)
            lastPlayingSound--;

        // sound
        particlesRemaining.set(permissions.get(Permissions.PARTICLES));
        particlesRemaining.tick();

        // particles
        soundsRemaining.set(permissions.get(Permissions.SOUNDS));
        soundsRemaining.tick();

        // call events
        FiguraMod.pushProfiler("worldTick");
        worldTick.reset(permissions.get(Permissions.WORLD_TICK_INST));
        run("WORLD_TICK", worldTick);

        FiguraMod.popPushProfiler("tick");
        tick.reset(permissions.get(Permissions.TICK_INST));
        tickEvent();

        FiguraMod.popProfiler();
    }

    public void render(float delta) {
        if (complexity.remaining <= 0) {
            noPermissions.add(Permissions.COMPLEXITY);
        } else {
            noPermissions.remove(Permissions.COMPLEXITY);
        }

        complexity.reset(permissions.get(Permissions.COMPLEXITY));

        if (scriptError || luaRuntime == null || !loaded)
            return;

        render.reset(permissions.get(Permissions.RENDER_INST));
        worldRender.reset(permissions.get(Permissions.WORLD_RENDER_INST));
        run("WORLD_RENDER", worldRender, delta);
    }

    public void runPing(int id, byte[] data) {
        events.offer(() -> {
            if (scriptError || luaRuntime == null || !loaded)
                return;

            LuaValue[] args = PingArg.fromByteArray(data, this);
            String name = luaRuntime.ping.getName(id);
            PingFunction function = luaRuntime.ping.get(name);
            if (args == null || function == null)
                return;

            FiguraLuaPrinter.sendPingMessage(this, name, data.length, args);
            luaRuntime.run(function.func, tick, (Object[]) args);
        });
    }

    public LuaValue loadScript(String name, String chunk) {
        return scriptError || luaRuntime == null || !loaded ? null : luaRuntime.load(name, chunk);
    }

    private void flushQueuedEvents() {
        // run all queued events
        Runnable e;
        while ((e = events.poll()) != null) {
            try {
                e.run();
            } catch (Exception | StackOverflowError ex) {
                if (luaRuntime != null)
                    luaRuntime.error(ex);
            }
        }
    }

    public Varargs run(Object toRun, Instructions limit, Object... args) {
        // stuff that was not run yet
        flushQueuedEvents();

        if (scriptError || luaRuntime == null || !loaded)
            return null;

        // run event
        Varargs ret = luaRuntime.run(toRun, limit, args);

        // stuff that this run produced
        flushQueuedEvents();

        // return
        return ret;
    }

    public void punish(int amount) {
        if (luaRuntime != null)
            luaRuntime.takeInstructions(amount);
    }

    // -- script events -- //

    private boolean isCancelled(Varargs args) {
        if (args == null)
            return false;
        for (int i = 1; i <= args.narg(); i++) {
            if (args.arg(i).isboolean() && args.arg(i).checkboolean())
                return true;
        }
        return false;
    }

    public void tickEvent() {
        if (loaded && luaRuntime != null && luaRuntime.getUser() != null)
            run("TICK", tick);
    }

    public void renderEvent(float delta, FiguraMat4 poseMatrix) {
        if (loaded && luaRuntime != null && luaRuntime.getUser() != null)
            run("RENDER", render, delta, renderMode.name(), poseMatrix);
    }

    public void postRenderEvent(float delta, FiguraMat4 poseMatrix) {
        if (loaded && luaRuntime != null && luaRuntime.getUser() != null)
            run("POST_RENDER", render.post(), delta, renderMode.name(), poseMatrix);
        renderMode = EntityRenderMode.OTHER;
    }

    public void postWorldRenderEvent(float delta) {
        if (!loaded)
            return;

        if (renderer != null)
            renderer.allowMatrixUpdate = false;

        run("POST_WORLD_RENDER", worldRender.post(), delta);
    }

    public boolean skullRenderEvent(float delta, BlockStateAPI block, ItemStackAPI item, EntityAPI<?> entity, String mode) {
        Varargs result = null;
        if (loaded && renderer != null && renderer.allowSkullRendering)
            result = run("SKULL_RENDER", render, delta, block, item, entity, mode);
        return isCancelled(result);
    }

    public boolean useItemEvent(ItemStackAPI stack, String type, int particleCount) {
        Varargs result = loaded ? run("USE_ITEM", tick, stack, type, particleCount) : null;
        return isCancelled(result);
    }

    public boolean arrowRenderEvent(float delta, EntityAPI<?> arrow) {
        Varargs result = null;
        if (loaded) result = run("ARROW_RENDER", render, delta, arrow);
        return isCancelled(result);
    }

    public boolean tridentRenderEvent(float delta, EntityAPI<?> trident) {
        Varargs result = null;
        if (loaded) result = run("TRIDENT_RENDER", render, delta, trident);
        return isCancelled(result);
    }

    public boolean itemRenderEvent(ItemStackAPI item, String mode, FiguraVec3 pos, FiguraVec3 rot, FiguraVec3 scale, boolean leftHanded, RenderTypes.FiguraBufferSource bufferSource, int light, int overlay) {
        Varargs result = loaded ? run("ITEM_RENDER", render, item, mode, pos, rot, scale, leftHanded) : null;
        if (result == null)
            return false;

        boolean rendered = false;
        for (int i = 1; i <= result.narg(); i++) {
            if (result.arg(i).isuserdata(FiguraModelPart.class))
                rendered |= renderItem(bufferSource, (FiguraModelPart) result.arg(i).checkuserdata(FiguraModelPart.class), light, overlay);
        }
        return rendered;
    }

    public boolean playSoundEvent(String id, FiguraVec3 pos, float vol, float pitch, boolean loop, String category, String file) {
        Varargs result = null;
        if (loaded) result = run("ON_PLAY_SOUND", tick, id, pos, vol, pitch, loop, category, file);
        return isCancelled(result);
    }

    public void resourceReloadEvent() {
        if (loaded) run("RESOURCE_RELOAD", tick);
    }

    // -- host only events -- //

    public String chatSendMessageEvent(String message) { // piped event
        Varargs val = loaded ? run("CHAT_SEND_MESSAGE", tick, message) : null;
        return val == null || (!val.isnil(1) && !Configs.CHAT_MESSAGES.value) ? message : val.isnil(1) ? "" : val.arg(1).tojstring();
    }

    public Pair<String, Integer> chatReceivedMessageEvent(String message, String json) { // special case
        Varargs val = loaded ? run("CHAT_RECEIVE_MESSAGE", tick, message, json) : null;
        if (val == null)
            return null;

        if (val.arg(1).isboolean() && !val.arg(1).checkboolean())
            return Pair.of(null, null);

        String msg = val.isnil(1) ? json : val.arg(1).tojstring();
        Integer color = null;
        if (val.arg(2).isuserdata(FiguraVec3.class))
            color = ColorUtils.rgbToInt((FiguraVec3) val.arg(2).checkuserdata(FiguraVec3.class));

        return Pair.of(msg, color);
    }

    public boolean mouseScrollEvent(double delta) {
        Varargs result = loaded ? run("MOUSE_SCROLL", tick, delta) : null;
        return isCancelled(result);
    }

    public boolean mouseMoveEvent(double x, double y) {
        Varargs result = loaded ? run("MOUSE_MOVE", tick, x, y) : null;
        return isCancelled(result);
    }

    public boolean mousePressEvent(int button, int action, int modifiers) {
        Varargs result = loaded ? run("MOUSE_PRESS", tick, button, action, modifiers) : null;
        return isCancelled(result);
    }

    public boolean keyPressEvent(int key, int action, int modifiers) {
        Varargs result = loaded ? run("KEY_PRESS", tick, key, action, modifiers) : null;
        return isCancelled(result);
    }

    public void charTypedEvent(String chars, int modifiers, int codePoint) {
        if (loaded) run("CHAR_TYPED", tick, chars, modifiers, codePoint);
    }

    // -- rendering events -- //

    private void render() {
        if (renderMode == EntityRenderMode.RENDER || renderMode == EntityRenderMode.FIRST_PERSON) {
            complexity.use(renderer.render());
            return;
        }

        int prev = complexity.remaining;
        complexity.remaining = permissions.get(Permissions.COMPLEXITY);
        renderer.render();
        complexity.remaining = prev;
    }

    public void render(Entity entity, float yaw, float delta, float alpha, RenderTypes.FiguraBufferSource bufferSource, int light, int overlay, RenderLivingBase<?> entityRenderer, PartFilterScheme filter, boolean translucent, boolean glowing) {
        if (renderer == null || !loaded)
            return;

        renderer.vanillaModelData.update(entityRenderer);
        renderer.yaw = yaw;
        renderer.entity = entity;

        renderer.setupRenderer(
                filter, bufferSource,
                delta, light, alpha, overlay,
                translucent, glowing
        );

        render();
    }

    public synchronized void worldRender(Entity entity, double camX, double camY, double camZ, RenderTypes.FiguraBufferSource bufferSource, int lightFallback, float tickDelta, EntityRenderMode mode) {
        if (renderer == null || !loaded)
            return;

        EntityRenderMode prevRenderMode = renderMode;
        renderMode = mode;
        boolean update = prevRenderMode != EntityRenderMode.OTHER || renderMode == EntityRenderMode.FIRST_PERSON_WORLD;

        renderer.pivotCustomizations.values().clear();
        renderer.allowMatrixUpdate = renderer.updateLight = update;
        renderer.entity = entity;

        renderer.setupRenderer(
                PartFilterScheme.WORLD, bufferSource,
                tickDelta, lightFallback, 1f, 10 << 16,
                false, false,
                camX, camY, camZ
        );

        complexity.use(renderer.renderSpecialParts());

        renderMode = prevRenderMode;
        renderer.updateLight = false;
    }

    public void capeRender(Entity entity, RenderTypes.FiguraBufferSource bufferSource, int light, float tickDelta, ModelRenderer cloak) {
        if (renderer == null || !loaded)
            return;

        FiguraMod.pushProfiler(FiguraMod.MOD_ID);
        FiguraMod.pushProfiler(this);
        FiguraMod.pushProfiler("capeRender");

        renderer.vanillaModelData.update(ParentType.Cape, cloak);
        renderer.entity = entity;

        renderer.setupRenderer(
                PartFilterScheme.CAPE, bufferSource,
                tickDelta, light, 1f, 10 << 16,
                renderer.translucent, renderer.glowing
        );

        render();

        FiguraMod.popProfiler(3);
    }

    public void elytraRender(Entity entity, RenderTypes.FiguraBufferSource bufferSource, int light, float tickDelta, ModelBase model) {
        if (renderer == null || !loaded)
            return;

        FiguraMod.pushProfiler(FiguraMod.MOD_ID);
        FiguraMod.pushProfiler(this);
        FiguraMod.pushProfiler("elytraRender");

        renderer.entity = entity;

        renderer.setupRenderer(
                PartFilterScheme.LEFT_ELYTRA, bufferSource,
                tickDelta, light, 1f, 10 << 16,
                renderer.translucent, renderer.glowing
        );

        // left
        FiguraMod.pushProfiler("leftWing");
        renderer.vanillaModelData.update(ParentType.LeftElytra, model);
        renderer.renderSpecialParts();

        // right
        FiguraMod.popPushProfiler("rightWing");
        renderer.vanillaModelData.update(ParentType.RightElytra, model);
        renderer.currentFilterScheme = PartFilterScheme.RIGHT_ELYTRA;
        renderer.renderSpecialParts();

        FiguraMod.popProfiler(4);
    }

    public void firstPersonWorldRender(Entity watcher, RenderTypes.FiguraBufferSource bufferSource, RenderManager camera, float tickDelta) {
        if (renderer == null || !loaded)
            return;

        FiguraMod.pushProfiler(FiguraMod.MOD_ID);
        FiguraMod.pushProfiler(this);
        FiguraMod.pushProfiler("firstPersonWorldRender");

        int light = camera.world.getCombinedLight(watcher.getPosition(), 0);
        Vec3d camPos = new Vec3d(camera.viewerPosX, camera.viewerPosY, camera.viewerPosZ);

        worldRender(watcher, camPos.x, camPos.y, camPos.z, bufferSource, light, tickDelta, EntityRenderMode.FIRST_PERSON_WORLD);

        FiguraMod.popProfiler(3);
    }

    public void firstPersonRender(RenderTypes.FiguraBufferSource bufferSource, EntityPlayer player, RenderPlayer playerRenderer, ModelRenderer arm, int light, float tickDelta) {
        if (renderer == null || !loaded)
            return;

        boolean lefty = arm == playerRenderer.getMainModel().bipedLeftArm;

        FiguraMod.pushProfiler(FiguraMod.MOD_ID);
        FiguraMod.pushProfiler(this);
        FiguraMod.pushProfiler("firstPersonRender");
        FiguraMod.pushProfiler(lefty ? "leftArm" : "rightArm");

        PartFilterScheme filter = lefty ? PartFilterScheme.LEFT_ARM : PartFilterScheme.RIGHT_ARM;
        boolean config = Configs.ALLOW_FP_HANDS.value;
        renderer.allowHiddenTransforms = config;
        renderer.allowMatrixUpdate = false;
        renderer.ignoreVanillaVisibility = true;

        GlStateManager.pushMatrix();
        if (!config) {
            // GlStateManager.rotate(arm.rotateAngleZ, 0,0,1); //TODO: Remove quaternions here
            GlStateManager.rotate(((Vector3fExtension)UIHelper.ZP).figura$rotation(arm.rotateAngleZ));
         //   stack.mulPose(Vector3f.YP.rotation(arm.yRot));
            GlStateManager.rotate(((Vector3fExtension)UIHelper.YP).figura$rotation(arm.rotateAngleY));
           // stack.mulPose(Vector3f.XP.rotation(arm.xRot));
            GlStateManager.rotate(((Vector3fExtension)UIHelper.XP).figura$rotation(arm.rotateAngleX));
        }
        render(player, 0f, tickDelta, 1f, bufferSource, light, 10 << 16, playerRenderer, filter, false, false);
        GlStateManager.popMatrix();

        renderer.allowHiddenTransforms = true;
        renderer.ignoreVanillaVisibility = false;

        FiguraMod.popProfiler(4);
    }

    public void hudRender(RenderTypes.FiguraBufferSource bufferSource, Entity entity, float tickDelta) {
        if (renderer == null || !loaded)
            return;

        FiguraMod.pushProfiler(this);
        FiguraMod.pushProfiler("hudRender");

        GlStateManager.pushMatrix();
        //TODO investigate if the matrix still needs to be scaled:tm:
        GlStateManager.scale(16, 16, -16);
        GlStateManager.scale(1, 1, -1); // We flip on Z

        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.disableDepth();

        renderer.entity = entity;

        renderer.setupRenderer(
                PartFilterScheme.HUD, bufferSource,
                tickDelta, 15 << 20 | 15 << 4, 1f, 10 << 16,
                false, false
        );

        if (renderer.renderSpecialParts() > 0)
            renderer.bufferSource.endBatch();

        GlStateManager.enableDepth();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.popMatrix();

        FiguraMod.popProfiler(2);
    }

    public boolean skullRender(RenderTypes.FiguraBufferSource bufferSource, int light, EnumFacing direction, float yaw) {
        if (renderer == null || !loaded || !renderer.allowSkullRendering)
            return false;
        //TODO: check if these are still necessary too
        GlStateManager.pushMatrix();

        if (direction == null)
            GlStateManager.translate(0.5d, 0d, 0.5d);
        else
            GlStateManager.translate((0.5d - direction.getFrontOffsetX() * 0.25d), 0.25d, (0.5d - direction.getFrontOffsetZ() * 0.25d));

        GlStateManager.scale(-1f, -1f, 1f);
        GlStateManager.rotate(((Vector3fExtension)UIHelper.YP).figura$rotationDegrees(yaw));

        renderer.allowPivotParts = false;

        renderer.setupRenderer(
                PartFilterScheme.SKULL, bufferSource,
                1f, light, 1f, 10 << 16,
                false, false
        );

        int comp = renderer.renderSpecialParts();
        complexity.use(comp);

        // head
        boolean bool = comp > 0 || headRender(bufferSource, light, true);

        renderer.allowPivotParts = true;
        GlStateManager.popMatrix();
        return bool;
    }

    public boolean headRender(RenderTypes.FiguraBufferSource bufferSource, int light, boolean useComplexity) {
        if (renderer == null || !loaded)
            return false;

        boolean oldMat = renderer.allowMatrixUpdate;

        // pre render
        renderer.setupRenderer(
                PartFilterScheme.HEAD, bufferSource,
                1f, light, 1f, 10 << 16,
                false, false
        );

        renderer.allowHiddenTransforms = false;
        renderer.allowMatrixUpdate = false;
        renderer.ignoreVanillaVisibility = true;

        // render
        int comp = renderer.render();
        if (useComplexity)
            complexity.use(comp);

        // pos render
        renderer.allowMatrixUpdate = oldMat;
        renderer.allowHiddenTransforms = true;
        renderer.ignoreVanillaVisibility = false;

        return comp > 0 && luaRuntime != null && !luaRuntime.vanilla_model.HEAD.checkVisible();
    }

    public static FloatBuffer posBuf = BufferUtils.createFloatBuffer(16);
    public boolean renderPortrait(int x, int y, int size, float modelScale, boolean upsideDown) {
        if (!Configs.AVATAR_PORTRAIT.value || renderer == null || !loaded)
            return false;

        // matrices
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0d);
        GlStateManager.scale(modelScale, modelScale * (upsideDown ? 1 : -1), modelScale);
        GlStateManager.rotate(((Vector3fExtension)UIHelper.XP).figura$rotationDegrees(180f)); //TODO remove quaternionss

        // scissors
        GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, posBuf);
        Matrix4f transformedPos = new Matrix4f();
        transformedPos.load(posBuf);

        FiguraVec3 pos = FiguraMat4.of().set(transformedPos).apply(0d, 0d, 0d);

        int x1 = (int) pos.x;
        int y1 = (int) pos.y;
        int x2 = (int) pos.x + size;
        int y2 = (int) pos.y + size;

        UIHelper.setupScissor(x1, y1, x2 - x1, y2 - y1);
        UIHelper.paperdoll = true;
        UIHelper.dollScale = 16f;

        // setup render
        GlStateManager.translate(4d / 16d, upsideDown ? 0 : (8d / 16d), 0d);

        RenderHelper.enableGUIStandardItemLighting();

        RenderTypes.FiguraBufferSource buffer = RenderTypes.FiguraBufferSource.INSTANCE;
        int light = 15 << 20 | 15 << 4;

        renderer.allowPivotParts = false;

        renderer.setupRenderer(
                PartFilterScheme.PORTRAIT, buffer,
                1f, light, 1f, 10 << 16,
                false, false
        );

        // render
        int comp = renderer.renderSpecialParts();
        boolean ret = comp > 0 || headRender(buffer, light, false);

        // after render
        buffer.endBatch();
        GlStateManager.popMatrix();

        UIHelper.disableScissor();
        UIHelper.paperdoll = false;

        renderer.allowPivotParts = true;

        // return
        return ret;
    }

    public boolean renderArrow(RenderTypes.FiguraBufferSource bufferSource, float delta, int light) {
        if (renderer == null || !loaded)
            return false;

        GlStateManager.pushMatrix();
        Quaternion quaternionf = ((Vector3fExtension)UIHelper.XP).figura$rotationDegrees(135f); //TODO quaternionss
        Quaternion quaternionf2 = ((Vector3fExtension)UIHelper.YP).figura$rotationDegrees(-90f);
        Quaternion.mul(quaternionf, quaternionf2, quaternionf);
        GlStateManager.rotate(quaternionf);

        renderer.setupRenderer(
                PartFilterScheme.ARROW, bufferSource,
                delta, light, 1f, 10 << 16,
                false, false
        );

        int comp = renderer.renderSpecialParts();

        GlStateManager.popMatrix();
        return comp > 0;
    }

    public boolean renderTrident(RenderTypes.FiguraBufferSource bufferSource, float delta, int light) {
        if (renderer == null || !loaded)
            return false;

        GlStateManager.pushMatrix();
        Quaternion quaternionf = ((Vector3fExtension)UIHelper.ZP).figura$rotationDegrees(90f);
        Quaternion quaternionf2 = ((Vector3fExtension)UIHelper.YP).figura$rotationDegrees(90f);
        Quaternion.mul(quaternionf, quaternionf2, quaternionf);
        GlStateManager.rotate(quaternionf);

        renderer.setupRenderer(
                PartFilterScheme.TRIDENT, bufferSource,
                delta, light, 1f, 10 << 16,
                false, false
        );

        int comp = renderer.renderSpecialParts();

        GlStateManager.popMatrix();
        return comp > 0;
    }

    public boolean renderItem(RenderTypes.FiguraBufferSource bufferSource, FiguraModelPart part, int light, int overlay) {
        if (renderer == null || !loaded || part.parentType != ParentType.Item)
            return false;

        GlStateManager.pushMatrix();
        GlStateManager.rotate(((Vector3fExtension)UIHelper.ZP).figura$rotationDegrees(180f));

        renderer.setupRenderer(
                PartFilterScheme.ITEM, bufferSource,
                1f, light, 1f, overlay,
                false, false
        );

        renderer.itemToRender = part;

        int ret = renderer.renderSpecialParts();

        GlStateManager.popMatrix();
        return ret > 0;
    }

    private static final PartCustomization PIVOT_PART_RENDERING_CUSTOMIZATION = new PartCustomization();
    public synchronized boolean pivotPartRender(ParentType parent, Consumer<Matrix4f> consumer) {
        if (renderer == null || !loaded || !parent.isPivot)
            return false;

        Queue<Pair<FiguraMat4, FiguraMat3>> queue = renderer.pivotCustomizations.computeIfAbsent(parent, p -> new ConcurrentLinkedQueue<>());

        if (queue.isEmpty())
            return false;

        int i = 0;
        while (!queue.isEmpty() && i++ < 1000) { // limit of 1000 pivot part renders, just in case something goes infinitely somehow
            Pair<FiguraMat4, FiguraMat3> matrixPair = queue.poll();
            PIVOT_PART_RENDERING_CUSTOMIZATION.setPositionMatrix(matrixPair.getFirst());
            PIVOT_PART_RENDERING_CUSTOMIZATION.setNormalMatrix(matrixPair.getSecond());
            PIVOT_PART_RENDERING_CUSTOMIZATION.needsMatrixRecalculation = false;
            Matrix4f position = PIVOT_PART_RENDERING_CUSTOMIZATION.copyIntoGlobalPoseStack();
            consumer.accept(position);
        }

        queue.clear();
        return true;
    }

    public void updateMatrices(RenderLivingBase<?> entityRenderer) {
        if (renderer == null || !loaded)
            return;

        FiguraMod.pushProfiler(FiguraMod.MOD_ID);
        FiguraMod.pushProfiler(this);
        FiguraMod.pushProfiler("updateMatrices");

        renderer.vanillaModelData.update(entityRenderer);
        renderer.currentFilterScheme = PartFilterScheme.MODEL;
        renderer.setMatrices();
        renderer.updateMatrices();

        FiguraMod.popProfiler(3);
    }


    // -- animations -- //


    public void applyAnimations() {
        if (!loaded || scriptError)
            return;

        animation.reset(permissions.get(Permissions.ANIMATION_INST));

        int animationsLimit = permissions.get(Permissions.BB_ANIMATIONS);
        int limit = animationsLimit;
        for (Animation animation : animations.values())
            limit = AnimationPlayer.tick(animation, limit);
        animationComplexity = animationsLimit - limit;

        if (limit <= 0) {
            noPermissions.add(Permissions.BB_ANIMATIONS);
        } else {
            noPermissions.remove(Permissions.BB_ANIMATIONS);
        }
    }

    public void clearAnimations() {
        if (!loaded || scriptError)
            return;

        for (Animation animation : animations.values())
            AnimationPlayer.clear(animation);
    }

    // -- functions -- //

    /**
     * We should call this whenever an avatar is no longer reachable!
     * It free()s all the CachedType used inside of the avatar, and also
     * closes the native texture resources.
     * also closes and stops this avatar sounds
     */
    public void clean() {
        if (renderer != null)
            renderer.invalidate();

        clearSounds();
        clearParticles();
        closeSockets();
        closeBuffers();

        events.clear();
    }

    public void clearSounds() {
        SoundAPI.getSoundEngine().figura$stopSound(owner, null);
        for (Map.Entry<String, byte[]> value : customSounds.entrySet())
            customSounds.remove(value.getKey(), value.getValue());
    }

    public void closeSockets() {
        for (FiguraSocket socket :
                openSockets) {
            if (!socket.isClosed()) {
                try {
                    socket.baseClose();
                } catch (Exception ignored) {}
            }
        }
        openSockets.clear();
    }

    public void closeBuffers() {
        for (FiguraBuffer buffer :
                openBuffers) {
            if (!buffer.isClosed()) {
                try {
                    buffer.baseClose();
                } catch (Exception ignored) {}
            }
        }
        openBuffers.clear();
    }

    public void clearParticles() {
        ParticleAPI.getParticleEngine().figura$clearParticles(owner);
    }

    private int getFileSize() {
        try {
            // get size
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            CompressedStreamTools.writeCompressed(nbt, baos);
            return baos.size();
        } catch (Exception e) {
            FiguraMod.LOGGER.warn("Failed to generate file size for model " + this.name, e);
            return 0;
        }
    }

    private int getVersionStatus() {
        if (version == null || (NetworkStuff.latestVersion != null && version.compareTo(NetworkStuff.latestVersion) > 0))
            return 0;
        return version.compareTo(FiguraMod.VERSION);
    }

    // -- loading -- //

    private void createLuaRuntime() {
        if (!nbt.hasKey("scripts"))
            return;

        Map<String, String> scripts = new HashMap<>();
        NBTTagCompound scriptsNbt = nbt.getCompoundTag("scripts");
        for (String s : scriptsNbt.getKeySet())
            scripts.put(PathUtils.computeSafeString(s), new String(scriptsNbt.getByteArray(s), StandardCharsets.UTF_8));

        NBTTagCompound metadata = nbt.getCompoundTag("metadata");

        NBTTagList autoScripts;
        if (metadata.hasKey("autoScripts"))
            autoScripts = metadata.getTagList("autoScripts", NbtType.STRING.getValue());
        else
            autoScripts = null;

        FiguraLuaRuntime runtime = new FiguraLuaRuntime(this, scripts);
        if (renderer != null && renderer.root != null)
            runtime.setGlobal("models", renderer.root);

        init.reset(permissions.get(Permissions.INIT_INST));
        runtime.setInstructionLimit(init.remaining);

        events.offer(() -> {
            if (runtime.init(autoScripts))
                init.use(runtime.getInstructions());
        });
    }

    private void loadAnimations() {
        if (!nbt.hasKey("animations"))
            return;

        ArrayList<String> autoAnims = new ArrayList<>();
        NBTTagCompound metadata = nbt.getCompoundTag("metadata");
        if (metadata.hasKey("autoAnims")) {
            NBTTagList codeList = metadata.getTagList("autoAnims", NbtType.STRING.getValue());
            for (int tagIndx = 0; tagIndx < codeList.tagCount(); tagIndx++) {
                autoAnims.add(codeList.getStringTagAt(tagIndx));
            }
        }

        NBTTagList root = nbt.getTagList("animations", NbtType.COMPOUND.getValue());
        for (int i = 0; i < root.tagCount(); i++) {
            try {
                NBTTagCompound animNbt = root.getCompoundTagAt(i);

                if (!animNbt.hasKey("mdl") || !animNbt.hasKey("name"))
                    continue;

                String mdl = animNbt.getString("mdl");
                String name = animNbt.getString("name");
                Animation.LoopMode loop = Animation.LoopMode.ONCE;
                if (animNbt.hasKey("loop")) {
                    try {
                        loop = Animation.LoopMode.valueOf(animNbt.getString("loop").toUpperCase());
                    } catch (Exception ignored) {}
                }

                Animation animation = new Animation(this,
                        mdl, name, loop,
                        animNbt.hasKey("ovr") && animNbt.getBoolean("ovr"),
                        animNbt.hasKey("len") ? animNbt.getFloat("len") : 0f,
                        animNbt.hasKey("off") ? animNbt.getFloat("off") : 0f,
                        animNbt.hasKey("bld") ? animNbt.getFloat("bld") : 1f,
                        animNbt.hasKey("sdel") ? animNbt.getFloat("sdel") : 0f,
                        animNbt.hasKey("ldel") ? animNbt.getFloat("ldel") : 0f
                );

                if (animNbt.hasKey("code")) {
                    NBTTagList codeList = animNbt.getTagList("code", NbtType.COMPOUND.getValue());
                    for (int tagIndx = 0; tagIndx < codeList.tagCount(); tagIndx++) {
                        NBTTagCompound compound = codeList.getCompoundTagAt(tagIndx);
                        animation.newCode(compound.getFloat("time"), compound.getString("src"));
                    }
                }

                animations.put(i, animation);

                if (autoAnims.contains(mdl + "." + name))
                    animation.play();
            } catch (Exception ignored) {}
        }
    }

    private void loadCustomSounds() {
        if (!nbt.hasKey("sounds"))
            return;

        NBTTagCompound root = nbt.getCompoundTag("sounds");
        for (String key : root.getKeySet()) {
            try {
                loadSound(key, root.getByteArray(key));
            } catch (Exception e) {
                FiguraMod.LOGGER.warn("Failed to load custom sound \"" + key + "\"", e);
            }
        }
    }

    public void loadSound(String name, byte[] data) throws Exception {
        this.customSounds.put(name, data);
    }

    public FiguraTexture registerTexture(String name, BufferedImage image, boolean ignoreSize) {
        int max = permissions.get(Permissions.TEXTURE_SIZE);
        if (!ignoreSize && (image.getWidth() > max || image.getHeight() > max)) {
            noPermissions.add(Permissions.TEXTURE_SIZE);
            throw new LuaError("Texture exceeded max size of " + max + " x " + max + " resolution, got " + image.getWidth() + " x " + image.getHeight());
        }

        FiguraTexture oldText = renderer.customTextures.get(name);
        if (oldText != null)
            oldText.deleteGlTexture();

        if (renderer.customTextures.size() > TextureAPI.TEXTURE_LIMIT)
            throw new LuaError("Maximum amount of textures reached!");

        FiguraTexture texture = new FiguraTexture(this, name, image);
        renderer.customTextures.put(name, texture);
        return texture;
    }

    public static class Instructions {

        public int max, remaining;
        private int currPre, currPost;
        public int pre, post;
        private boolean inverted;

        public Instructions(int remaining) {
            reset(remaining);
        }

        public Instructions post() {
            inverted = true;
            return this;
        }

        public int getTotal() {
            return pre + post;
        }

        public void reset(int remaining) {
            this.max = this.remaining = remaining;
            currPre = currPost = 0;
        }

        public void use(int amount) {
            remaining -= amount;

            if (!inverted) {
                currPre += amount;
                pre = currPre;
            } else {
                currPost += amount;
                post = currPost;
                inverted = false;
            }
        }
    }
}
