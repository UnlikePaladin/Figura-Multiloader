package org.figuramc.figura.utils.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.Badges;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.ducks.TextureMapAccessor;
import org.figuramc.figura.ducks.extensions.FontExtension;
import org.figuramc.figura.ducks.extensions.StyleExtension;
import org.figuramc.figura.ducks.extensions.Vector3fExtension;
import org.figuramc.figura.gui.screens.AbstractPanelScreen;
import org.figuramc.figura.gui.screens.FiguraConfirmScreen;
import org.figuramc.figura.gui.widgets.ContextMenu;
import org.figuramc.figura.gui.widgets.FiguraWidget;
import org.figuramc.figura.math.vector.FiguraVec4;
import org.figuramc.figura.model.rendering.EntityRenderMode;
import org.figuramc.figura.utils.*;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class UIHelper extends Gui {

    // -- Variables -- //

    public static Vector3f ZP = new Vector3f(0.0f, 0.0f, 1.0f);
    public static Vector3f XP = new Vector3f(1.0f, 0.0f, 0.0f);
    public static Vector3f YP = new Vector3f(0.0f, 1.0f, 0.0f);


    public static final ResourceLocation OUTLINE_FILL = new FiguraIdentifier("textures/gui/outline_fill.png");
    public static final ResourceLocation OUTLINE = new FiguraIdentifier("textures/gui/outline.png");
    public static final ResourceLocation TOOLTIP = new FiguraIdentifier("textures/gui/tooltip.png");
    public static final ResourceLocation UI_FONT = new FiguraIdentifier("ui");
    public static final ResourceLocation SPECIAL_FONT = new FiguraIdentifier("special");

    public static final ITextComponent UP_ARROW = new TextComponentString("^").setStyle(((StyleExtension)new Style()).setFont(UI_FONT));
    public static final ITextComponent DOWN_ARROW = new TextComponentString("V").setStyle(((StyleExtension)new Style()).setFont(UI_FONT));

    // Used for GUI rendering
    private static final CustomFramebuffer FIGURA_FRAMEBUFFER = new CustomFramebuffer();
    private static int previousFBO = -1;
    public static boolean paperdoll = false;
    public static float fireRot = 0f;
    public static float dollScale = 1f;
    private static final Stack<FiguraVec4> SCISSORS_STACK = new Stack<>();

    // -- Functions -- //

    public static void useFiguraGuiFramebuffer() {
       /* previousFBO = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);

        int width = Minecraft.getInstance().getWindow().getWidth();
        int height = Minecraft.getInstance().getWindow().getHeight();
        FIGURA_FRAMEBUFFER.setSize(width, height);

        // Enable stencil buffer during this phase of rendering
        GL30.glEnable(GL30.GL_STENCIL_TEST);
        GlStateManager._stencilMask(0xFF);
        // Bind custom GUI framebuffer to be used for rendering
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, FIGURA_FRAMEBUFFER.getFbo());

        // Clear GUI framebuffer
        GlStateManager._clearStencil(0);
        GlStateManager._clearColor(0f, 0f, 0f, 1f);
        GlStateManager._clearDepth(1);
        GlStateManager._clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL30.GL_STENCIL_BUFFER_BIT, false);

        Matrix4f mf = RenderSystem.getProjectionMatrix();
        Minecraft.getInstance().getMainRenderTarget().blitToScreen(width, height, false);
        RenderSystem.setProjectionMatrix(mf);*/
    }

    public static void useVanillaFramebuffer() {
        // Reset state before we go back to normal rendering
        GlStateManager.enableDepth();
        // Set a sensible default for stencil buffer operations
        GL11.glStencilFunc(GL11.GL_EQUAL, 0, 0xFF);
        GL11.glDisable(GL11.GL_STENCIL_TEST);

        // Bind vanilla framebuffer again
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousFBO);

        GlStateManager.disableBlend();
        // Draw GUI framebuffer -> vanilla framebuffer
        int windowWidth = Minecraft.getMinecraft().displayWidth;
        int windowHeight = Minecraft.getMinecraft().displayHeight;

        // I don't feel like porting this rn + we don't switch framebuffers anymore in rewrite
     //   Matrix4f mf = RenderSystem.getProjectionMatrix();
      //  FIGURA_FRAMEBUFFER.drawToScreen(windowWidth, windowHeight);
      //  RenderSystem.setProjectionMatrix(mf);
        GlStateManager.enableBlend();
    }

    private static final Vector3f INVENTORY_DIFFUSE_LIGHT_0 = ResourceUtils.make(new Vector3f(0.2f, -1.0f, -1.0f), Vector3f::normalise);
    private static final Vector3f INVENTORY_DIFFUSE_LIGHT_1 = ResourceUtils.make(new Vector3f(-0.2f, -1.0f, 0.0f), Vector3f::normalise);
    @SuppressWarnings("deprecation")
    public static void drawEntity(float x, float y, float scale, float pitch, float yaw, EntityLivingBase entity, EntityRenderMode renderMode) {
        // backup entity variables
        float headX = entity.rotationPitch;
        float headY = entity.rotationYawHead;
        boolean invisible = entity.isInvisible();

        float bodyY = entity.renderYawOffset; // not truly a backup
        if (entity.getRidingEntity() instanceof EntityLivingBase) {
            EntityLivingBase l = (EntityLivingBase) entity.getRidingEntity();
            // drawEntity(x, y, scale, pitch, yaw, l, stack, renderMode);
            bodyY = l.renderYawOffset;
        }

        // setup rendering properties
        float xRot, yRot;
        double xPos = 0d;
        double yPos = 0d;

        switch (renderMode) {
            case PAPERDOLL: {
                // rotations
                xRot = pitch;
                yRot = yaw + bodyY + 180;

                // positions
                yPos--;

                if (entity.isElytraFlying())
                    xPos += MathUtils.triangleWave((float) Math.toRadians(270), (float) (Math.PI * 2));

                if (entity.isElytraFlying()) {
                    yPos++;
                    entity.rotationPitch = 0f;
                }

                RenderUtils.setLights(RenderUtils.INVENTORY_DIFFUSE_LIGHT_0, RenderUtils.INVENTORY_DIFFUSE_LIGHT_1);

                // invisibility
                if (Configs.PAPERDOLL_INVISIBLE.value)
                    entity.setInvisible(false);
                break;
            }
            case FIGURA_GUI: {
                // rotations
                xRot = pitch;
                yRot = yaw + bodyY + 180;

                if (!Configs.PREVIEW_HEAD_ROTATION.value) {
                    entity.rotationPitch = 0f;
                    entity.rotationYawHead = bodyY;
                }

                // positions
                yPos--;

                // set up lighting
                RenderHelper.enableGUIStandardItemLighting();
                RenderUtils.setLights(ResourceUtils.make(new Vector3f(-0.2f, -1f, -1f), Vector3f::normalise), ResourceUtils.make(new Vector3f(-0.2f, 0.4f, -0.3f), Vector3f::normalise));

                // invisibility
                entity.setInvisible(false);
                break;
            }
            default: {
                // rotations
                xRot = pitch;
                yRot = yaw + bodyY + 180;

                entity.rotationPitch = -xRot;
                entity.rotationYawHead = -yaw + bodyY;

                // lightning

                RenderUtils.setLights(RenderUtils.INVENTORY_DIFFUSE_LIGHT_0, RenderUtils.INVENTORY_DIFFUSE_LIGHT_1);
                break;
            }
        }

        // apply matrix transformers
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, renderMode == EntityRenderMode.MINECRAFT_GUI ? 250d : -250d);
        GlStateManager.scale(scale, scale, scale);
       // stack.last().pose().multuliply(Matrix4f.createScaleMatrix(1f, 1f, -1f)); // Scale ONLY THE POSITIONS! Inverted normals don't work for whatever reason

        Avatar avatar = AvatarManager.getAvatar(entity);
        if (RenderUtils.vanillaModelAndScript(avatar) && !avatar.luaRuntime.renderer.getRootRotationAllowed()) {
            yRot = yaw;
        }

        // apply rotations
        Quaternion quaternion = ((Vector3fExtension)ZP).figura$rotationDegrees(180f);
        Quaternion quaternion2 = ((Vector3fExtension)YP).figura$rotationDegrees(yRot);
        Quaternion quaternion3 = ((Vector3fExtension)XP).figura$rotationDegrees(xRot);
        Quaternion.mul(quaternion3, quaternion2, quaternion3);
        Quaternion.mul(quaternion, quaternion3, quaternion);

        GlStateManager.rotate(quaternion);
        quaternion3.negate();

        // setup entity renderer
        Minecraft minecraft = Minecraft.getMinecraft();
        RenderManager dispatcher = minecraft.getRenderManager();
        boolean renderHitboxes = dispatcher.isDebugBoundingBox();
        dispatcher.setDebugBoundingBox(false);
        dispatcher.setRenderShadow(false);
        //TODO: Check this
        double quatYaw = Math.atan2(2.0*(quaternion3.y*quaternion3.z + quaternion3.w*quaternion3.x), quaternion3.w*quaternion3.w - quaternion3.x*quaternion3.x - quaternion3.y*quaternion3.y + quaternion3.z*quaternion3.z);
        double quatPitch = Math.asin(-2.0*(quaternion3.x*quaternion3.z - quaternion3.w*quaternion3.y));
        double quatRoll = Math.atan2(2.0*(quaternion3.x*quaternion3.y + quaternion3.w*quaternion3.z), quaternion3.w*quaternion3.w + quaternion3.x*quaternion3.x - quaternion3.y*quaternion3.y - quaternion3.z*quaternion3.z);

        dispatcher.playerViewY = (float) quatYaw;
        dispatcher.playerViewX = (float) quatPitch;
//        dispatcher.overrideCameraOrientation(quaternion3);

        // render
        paperdoll = true;
        fireRot = -yRot;
        dollScale = scale;

        if (avatar != null) avatar.renderMode = renderMode;

        double finalXPos = xPos;
        double finalYPos = yPos;
        int packedLight = 15 << 20 | 15 << 4;
        int j = packedLight % 65536;
        int k = packedLight / 65536;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float)j, (float)k);
        dispatcher.renderEntity(entity, finalXPos, finalYPos, 0d, 0f, 1f, false);

        paperdoll = false;

        // restore entity rendering data
        dispatcher.setDebugBoundingBox(renderHitboxes);
        dispatcher.setRenderShadow(true);

        // pop matrix
        GlStateManager.popMatrix();
        RenderHelper.enableStandardItemLighting();

        // restore entity data
        entity.rotationPitch = headX;
        entity.rotationYawHead = headY;
        entity.setInvisible(invisible);
    }

    public static void setupTexture(ResourceLocation texture) {
        GlStateManager.enableBlend();
        //Sus
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    public static void renderTexture(int x, int y, int width, int height, ResourceLocation texture) {
        setupTexture(texture);
        blit(x, y, width, height, 0, 0, 1, 1, 1, 1);
    }


    public static void blit(int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight) {
        blit(x, y, width, height, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }

    public static void blit(int x, int y, int width, int height, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        bufferBuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        int x2 = x + width;
        int y2 = y + height;
        float minU = (uOffset + 0.0f) / (float)textureWidth;
        float maxU = (uOffset + (float)uWidth) / (float)textureWidth;
        float minV = (vOffset + 0.0f) / (float)textureHeight;
        float maxV = (vOffset + (float)vHeight) / (float)textureHeight;

        bufferBuilder.pos(x, y2, 0).tex(minU, maxV).endVertex();
        bufferBuilder.pos(x2, y2, 0).tex(maxU, maxV).endVertex();
        bufferBuilder.pos(x2, y, 0).tex(maxU, minV).endVertex();
        bufferBuilder.pos(x, y, 0).tex(minU, minV).endVertex();
        GlStateManager.enableAlpha();
        Tessellator.getInstance().draw();
        // TODO: CHeck if VBO usage is possible
    }

    public static void renderAnimatedBackground(ResourceLocation texture, float x, float y, float width, float height, float textureWidth, float textureHeight, double speed, float delta) {
        if (speed != 0) {
            double d = (FiguraMod.ticks + delta) * speed;
            x -= d % textureWidth;
            y -= d % textureHeight;
        }

        width += textureWidth;
        height += textureHeight;

        if (speed < 0) {
            x -= textureWidth;
            y -= textureHeight;
        }

        renderBackgroundTexture(texture, x, y, width, height, textureWidth, textureHeight);
    }

    public static void renderBackgroundTexture(ResourceLocation texture, float x, float y, float width, float height, float textureWidth, float textureHeight) {
        setupTexture(texture);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        bufferBuilder.begin(VertexFormatMode.QUADS.asGLMode, DefaultVertexFormats.POSITION_TEX);

        float u1 = width / textureWidth;
        float v1 = height / textureHeight;
        quad(bufferBuilder, x, y, width, height, -999f, 0f, u1, 0f, v1);

        tessellator.draw();
    }

    public static void fillRounded(int x, int y, int width, int height, int color) {
        drawRect(x + 1, y, x + width - 1, y + 1, color);
        drawRect(x, y + 1, x + width, y + height - 1, color);
        drawRect(x + 1, y + height - 1, x + width - 1, y + height, color);
    }

    public static void fillOutline(int x, int y, int width, int height, int color) {
        drawRect(x + 1, y, x + width - 1, y + 1, color);
        drawRect(x, y + 1, x + 1, y + height - 1, color);
        drawRect(x + width - 1, y + 1, x + width, y + height - 1, color);
        drawRect(x + 1, y + height - 1, x + width - 1, y + height, color);
    }

    public static void renderSliced(int x, int y, int width, int height, ResourceLocation texture) {
        renderSliced(x, y, width, height, 0f, 0f, 15, 15, 15, 15, texture);
    }

    public static void renderSliced(int x, int y, int width, int height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight, ResourceLocation texture) {
        setupTexture(texture);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(VertexFormatMode.QUADS.asGLMode, DefaultVertexFormats.POSITION_TEX);

        float rWidthThird = regionWidth / 3f;
        float rHeightThird = regionHeight / 3f;

        // top left
        quad(buffer, x, y, rWidthThird, rHeightThird, u, v, rWidthThird, rHeightThird, textureWidth, textureHeight);
        // top middle
        quad(buffer, x + rWidthThird, y, width - rWidthThird * 2, rHeightThird, u + rWidthThird, v, rWidthThird, rHeightThird, textureWidth, textureHeight);
        // top right
        quad(buffer, x + width - rWidthThird, y, rWidthThird, rHeightThird, u + rWidthThird * 2, v, rWidthThird, rHeightThird, textureWidth, textureHeight);

        // middle left
        quad(buffer, x, y + rHeightThird, rWidthThird, height - rHeightThird * 2, u, v + rHeightThird, rWidthThird, rHeightThird, textureWidth, textureHeight);
        // middle middle
        quad(buffer, x + rWidthThird, y + rHeightThird, width - rWidthThird * 2, height - rHeightThird * 2, u + rWidthThird, v + rHeightThird, rWidthThird, rHeightThird, textureWidth, textureHeight);
        // middle right
        quad(buffer, x + width - rWidthThird, y + rHeightThird, rWidthThird, height - rHeightThird * 2, u + rWidthThird * 2, v + rHeightThird, rWidthThird, rHeightThird, textureWidth, textureHeight);

        // bottom left
        quad(buffer, x, y + height - rHeightThird, rWidthThird, rHeightThird, u, v + rHeightThird * 2, rWidthThird, rHeightThird, textureWidth, textureHeight);
        // bottom middle
        quad(buffer, x + rWidthThird, y + height - rHeightThird, width - rWidthThird * 2, rHeightThird, u + rWidthThird, v + rHeightThird * 2, rWidthThird, rHeightThird, textureWidth, textureHeight);
        // bottom right
        quad(buffer, x + width - rWidthThird, y + height - rHeightThird, rWidthThird, rHeightThird, u + rWidthThird * 2, v + rHeightThird * 2, rWidthThird, rHeightThird, textureWidth, textureHeight);

        tessellator.draw();
    }

    public static void renderHalfTexture(int x, int y, int width, int height, int textureWidth, ResourceLocation texture) {
        renderHalfTexture(x, y, width, height, 0f, 0f, textureWidth, 1, textureWidth, 1, texture);
    }

    public static void renderHalfTexture(int x, int y, int width, int height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight, ResourceLocation texture) {
        setupTexture(texture);

        // left
        int w = width / 2;
        blit(x, y, w, height, u, v, w, regionHeight, textureWidth, textureHeight);
        // right
        x += w;
        if (width % 2 == 1) w++;
        blit(x, y, w, height, u + regionWidth - w, v, w, regionHeight, textureWidth, textureHeight);
    }

    public static void renderSprite(int x, int y, int z, int width, int height, TextureAtlasSprite sprite) {
        setupTexture(((TextureMapAccessor)Minecraft.getMinecraft().getTextureMapBlocks()).invokeGetResourceLocation(sprite));
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        bufferBuilder.begin(VertexFormatMode.QUADS.asGLMode, DefaultVertexFormats.POSITION_TEX);
        quad(bufferBuilder, x, y, width, height, z, sprite.getMinU(), sprite.getMaxU(), sprite.getMinU(), sprite.getMaxV());
        bufferBuilder.finishDrawing();
        Tessellator.getInstance().draw();
    }

    public static void setupScissor(int x, int y, int width, int height) {
        FiguraVec4 vec = FiguraVec4.of(x, y, width, height);
        if (!SCISSORS_STACK.isEmpty()) {
            FiguraVec4 old = SCISSORS_STACK.peek();
            double newX = Math.max(x, old.x());
            double newY = Math.max(y, old.y());
            double newWidth = Math.min(x + width, old.x() + old.z()) - newX;
            double newHeight = Math.min(y + height, old.y() + old.w()) - newY;
            vec.set(newX, newY, newWidth, newHeight);
        }

        SCISSORS_STACK.push(vec);
        setupScissor(vec);
    }

    private static void quad(BufferBuilder bufferBuilder, float x, float y, float width, float height, float u, float v, float regionWidth, float regionHeight, int textureWidth, int textureHeight) {
        float u0 = u / textureWidth;
        float v0 = v / textureHeight;
        float u1 = (u + regionWidth) / textureWidth;
        float v1 = (v + regionHeight) / textureHeight;
        quad(bufferBuilder, x, y, width, height, 0f, u0, u1, v0, v1);
    }

    private static void quad(BufferBuilder bufferBuilder, float x, float y, float width, float height, float z, float u0, float u1, float v0, float v1) {
        float x1 = x + width;
        float y1 = y + height;
        bufferBuilder.pos(x, y1, z).tex(u0, v1).endVertex();
        bufferBuilder.pos(x1, y1, z).tex(u1, v1).endVertex();
        bufferBuilder.pos(x1, y, z).tex(u1, v0).endVertex();
        bufferBuilder.pos(x, y, z).tex(u0, v0).endVertex();
    }

    private static void setupScissor(FiguraVec4 dimensions) {
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        double scale = scaledResolution.getScaleFactor();
        int screenY = Minecraft.getMinecraft().displayHeight;

        int scaledWidth = (int) Math.max(dimensions.z * scale, 0);
        int scaledHeight = (int) Math.max(dimensions.w * scale, 0);
        enableScissor((int) (dimensions.x * scale), (int) (screenY - dimensions.y * scale - scaledHeight), scaledWidth, scaledHeight);
    }

    private static void enableScissor(int x, int y, int width, int height) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x, y, width, height);
    }

    public static void disableScissor() {
        SCISSORS_STACK.pop();
        if (!SCISSORS_STACK.isEmpty()) {
            setupScissor(SCISSORS_STACK.peek());
        } else {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    public static void renderWithoutScissors(Runnable toRun) {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        toRun.run();
        if (!SCISSORS_STACK.isEmpty()) {
            setupScissor(SCISSORS_STACK.peek());
        }
    }

    public static void highlight(FiguraWidget widget, ITextComponent text) {
        // screen
        int screenW, screenH;
        if (Minecraft.getMinecraft().currentScreen instanceof AbstractPanelScreen) {
            AbstractPanelScreen panel = (AbstractPanelScreen) Minecraft.getMinecraft().currentScreen;
            screenW = panel.width;
            screenH = panel.height;
        } else {
            return;
        }

        // draw

        int x = widget.getX();
        int y = widget.getY();
        int width = widget.getWidth();
        int height = widget.getHeight();
        int color = 0xDD000000;

        // left
        drawRect(0, 0, x, y + height, color);
        // right
        drawRect(x + width, y, screenW, screenH, color);
        // up
        drawRect(x, 0, screenW, y, color);
        // down
        drawRect(0, y + height, x + width, screenH, color);

        // outline
        fillOutline(Math.max(x - 1, 0), Math.max(y - 1, 0), Math.min(width + 2, screenW), Math.min(height + 2, screenH), 0xFFFFFFFF);

        // text

        if (text == null)
            return;

        int bottomDistance = screenH - (y + height);
        int rightDistance = screenW - (x + width);
        int verArea = y * screenW - bottomDistance * screenW;
        int horArea = x * screenH - rightDistance * screenH;
        FiguraVec4 square = new FiguraVec4();

        if (Math.abs(verArea) > Math.abs(horArea)) {
            if (verArea >= 0) {
                square.set(0, 0, screenW, y);
            } else {
                square.set(0, y + height, screenW, bottomDistance);
            }
        } else {
            if (horArea >= 0) {
                square.set(0, 0, x, screenH);
            } else {
                square.set(x + width, 0, rightDistance, screenH);
            }
        }

        // fill(stack, (int) square.x, (int) square.y, (int) (square.x + square.z), (int) (square.y + square.w), 0xFFFF72AD);
        // renderTooltip(stack, text, 0, 0, false);
    }

    // widget.isMouseOver() returns false if the widget is disabled or invisible
    public static boolean isMouseOver(int x, int y, int width, int height, double mouseX, double mouseY) {
        return isMouseOver(x, y, width, height, mouseX, mouseY, false);
    }

    public static boolean isMouseOver(int x, int y, int width, int height, double mouseX, double mouseY, boolean force) {
        ContextMenu context = force ? null : getContext();
        return (context == null || !context.isVisible()) && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public static void renderOutlineText(FontRenderer textRenderer, ITextComponent text, int x, int y, int color, int outline) {
        ((FontExtension)textRenderer).figura$drawInBatch8xOutline(text, x, y, color, outline);
    }

    public static void renderTooltip(ITextComponent tooltip, int mouseX, int mouseY, boolean background) {
        Minecraft minecraft = Minecraft.getMinecraft();

        // window
        ScaledResolution scaledResolution = new ScaledResolution(minecraft);
        int screenX = scaledResolution.getScaledWidth();
        int screenY = scaledResolution.getScaledHeight();

        boolean reduced = Configs.REDUCED_MOTION.value;

        // calculate pos
        int x = reduced ? 0 : mouseX;
        int y = reduced ? screenY : mouseY - 12;

        // prepare text
        FontRenderer font = minecraft.fontRenderer;
        List<ITextComponent> text = TextUtils.wrapTooltip(tooltip, font, x, screenX, 12);
        int height = font.FONT_HEIGHT * text.size();

        // clamp position to bounds
        x += 12;
        y = Math.min(Math.max(y, 0), screenY - height);
        int width = TextUtils.getWidth(text, font);
        if (x + width > screenX)
            x = Math.max(x - width - 24, 0);

        if (reduced) {
            x += (screenX - width) / 2;
            if (background)
                y -= 4;
        }

        // render
        GlStateManager.pushMatrix();
        GlStateManager.translate(0d, 0d, 999d);

        if (background)
            renderSliced(x - 4, y - 4, width + 8, height + 8, TOOLTIP);

        for (int i = 0; i < text.size(); i++) {
            ITextComponent charSequence = text.get(i);
            font.drawStringWithShadow(charSequence.getFormattedText(), x, y + font.FONT_HEIGHT * i, 0xFFFFFF);
        }

        GlStateManager.popMatrix();
    }

    public static void renderScrollingText(ITextComponent text, int x, int y, int width, int color) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int textWidth = font.getStringWidth(text.getFormattedText());
        int textX = x;

        // the text fit :D
        if (textWidth <= width) {
            font.drawString(text.getFormattedText(), textX, y, color);
            return;
        }

        // oh, no it doesn't fit
        textX += getTextScrollingOffset(textWidth, width, false);

        // draw text
        setupScissor(x, y, width, font.FONT_HEIGHT);
        font.drawString(text.getFormattedText(), textX, y, color);
        disableScissor();
    }

    public static void renderCenteredScrollingText(ITextComponent text, int x, int y, int width, int height, int color) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int textWidth = font.getStringWidth(text.getFormattedText());
        int textX = x + width / 2;
        int textY = y + height / 2 - font.FONT_HEIGHT / 2;

        // the text fit :D
        if (textWidth <= width) {
            font.drawStringWithShadow(text.getFormattedText(), (float)(textX - font.getStringWidth(text.getFormattedText()) / 2), (float)textY, color);
            return;
        }

        // oh, no it doesn't fit
        textX += getTextScrollingOffset(textWidth, width, true);

        // draw text
        setupScissor(x, y, width, height);
        font.drawStringWithShadow(text.getFormattedText(), (float)(textX - font.getStringWidth(text.getFormattedText()) / 2), (float)textY, color);
        disableScissor();
    }

    private static int getTextScrollingOffset(int textWidth, int width, boolean centered) {
        float speed = Configs.TEXT_SCROLL_SPEED.tempValue;
        int scrollLen = textWidth - width;
        int startingOffset = (int) Math.ceil(scrollLen / 2d);
        int stopDelay = (int) (Configs.TEXT_SCROLL_DELAY.tempValue * speed);
        int time = scrollLen + stopDelay;
        int totalTime = time * 2;
        int ticks = (int) (FiguraMod.ticks * speed);
        int currentTime = ticks % time;
        int dir = (ticks % totalTime) > time - 1 ? 1 : -1;

        int clamp = Math.min(Math.max(currentTime - stopDelay, 0), scrollLen);
        return (startingOffset - clamp) * dir - (centered ? 0 : startingOffset);
    }

    public static Runnable openURL(String url) {
        Minecraft minecraft = Minecraft.getMinecraft();
        return () -> minecraft.displayGuiScreen(new FiguraConfirmScreen.FiguraConfirmLinkScreen(bl -> {
            if (bl) {
                try {
                    PlatformUtils.openWebLink(new URI(url));
                } catch (URISyntaxException e) {
                }
            }
            return null;
        }, url, minecraft.currentScreen));
    }

    public static void renderLoading(int x, int y) {
        ITextComponent text = new TextComponentString(Integer.toHexString(Math.abs(FiguraMod.ticks) % 16)).setStyle(((StyleExtension)new Style()).setFont(Badges.FONT));
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        font.drawStringWithShadow(text.getFormattedText(), x - (float) font.getStringWidth(text.getFormattedText()) / 2, y - font.FONT_HEIGHT / 2, -1);
    }

    public static void setContext(ContextMenu context) {
        if (Minecraft.getMinecraft().currentScreen instanceof AbstractPanelScreen) {
            AbstractPanelScreen panelScreen = (AbstractPanelScreen) Minecraft.getMinecraft().currentScreen;
            panelScreen.contextMenu = context;
        }
    }

    public static ContextMenu getContext() {
        if (Minecraft.getMinecraft().currentScreen instanceof AbstractPanelScreen) {
            AbstractPanelScreen panelScreen = (AbstractPanelScreen) Minecraft.getMinecraft().currentScreen;
            return panelScreen.contextMenu;
        }
        return null;
    }

    public static void setTooltip(ITextComponent text) {
        if (Minecraft.getMinecraft().currentScreen instanceof AbstractPanelScreen) {
            AbstractPanelScreen panelScreen = (AbstractPanelScreen) Minecraft.getMinecraft().currentScreen;
            panelScreen.tooltip = text;
        }
    }

    public static void setTooltip(Style style) {
        if (style == null || style.getHoverEvent() == null)
            return;

        ITextComponent text = style.getHoverEvent().getValue();
        if (text != null)
            setTooltip(text);
    }

    public static void renderOutline(int x, int y, int width, int height, int color) {
        drawRect(x, y, x + width, y + 1, color);
        drawRect(x, y + height - 1, x + width, y + height, color);
        drawRect(x, y + 1, x + 1, y + height - 1, color);
        drawRect(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    public static void fill(int minX, int minY, int maxX, int maxY, int color) {
        Gui.drawRect(minX, minY, maxX, maxY, color);
    }

    public static void drawCenteredString(FontRenderer font, ITextComponent title, int x, int y, int color) {
        font.drawStringWithShadow(title.getFormattedText(), (float)(x - font.getStringWidth(title.getFormattedText()) / 2), (float)y, color);
    }

    public static void drawString(FontRenderer font, ITextComponent text, int i, int j, int color) {
        font.drawStringWithShadow(text.getFormattedText(), (float)i, (float)j, color);
    }

    public static ITextComponent getClickedComponentAt(ITextComponent text, int widgetWidth, int i) {
        if (text == null) {
            return null;
        } else {
            Minecraft mc = Minecraft.getMinecraft();
            int j = mc.fontRenderer.getStringWidth(text.getFormattedText());
            int k = widgetWidth / 2 - j / 2;
            int l = widgetWidth / 2 + j / 2;
            int m = k;
            if (i >= k && i <= l) {
                Iterator<ITextComponent> iTextComponentIterator = text.iterator();

                ITextComponent retVal;
                do {
                    if (!iTextComponentIterator.hasNext()) {
                        return null;
                    }
                    retVal = iTextComponentIterator.next();
                    m += mc.fontRenderer.getStringWidth(GuiUtilRenderComponents.removeTextColorsIfConfigured(retVal.getUnformattedComponentText(), false));
                } while(m <= i);

                return retVal;
            } else {
                return null;
            }
        }
    }
}
