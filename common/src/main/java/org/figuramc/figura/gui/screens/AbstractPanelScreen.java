package org.figuramc.figura.gui.screens;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.gui.widgets.*;
import org.figuramc.figura.lua.api.ClientAPI;
import org.figuramc.figura.utils.FiguraIdentifier;
import org.figuramc.figura.utils.ui.UIHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.Arrays;
import java.util.List;

public abstract class AbstractPanelScreen extends GuiScreen {
    private final List<FiguraRenderable> renderables = Lists.newArrayList();
    protected final List<AbstractFiguraWidget> buttons = Lists.newArrayList();
    protected final List<FiguraGuiEventListener> children = Lists.newArrayList();
    protected FiguraGuiEventListener selectedListener;
    private int prevMouseX, prevMouseY;

    public static final List<ResourceLocation> BACKGROUNDS = Arrays.asList(
            new FiguraIdentifier("textures/gui/background/background_0.png"),
            new FiguraIdentifier("textures/gui/background/background_1.png"),
            new FiguraIdentifier("textures/gui/background/background_2.png")
    );

    // variables
    protected final GuiScreen parentScreen;
    public PanelSelectorWidget panels;

    // overlays
    public ContextMenu contextMenu;
    public ITextComponent tooltip;
    public ITextComponent title;
    // stuff :3
    private static final String EGG = "ĉĉĈĈćĆćĆBAā";
    private String egg = EGG;

    protected AbstractPanelScreen(GuiScreen parentScreen, ITextComponent title) {
        super();
        this.title = title;
        this.parentScreen = parentScreen;
    }

    @Override
    public void setWorldAndResolution(Minecraft minecraft, int width, int height) {
        this.renderables.clear();
        this.children.clear();
        this.buttons.clear();
        super.setWorldAndResolution(minecraft, width, height);
    }

    public Class<? extends GuiScreen> getSelectedPanel() {
        return this.getClass();
    };

    @Override
    public void initGui() {
        super.initGui();

        // add panel selector
        this.addRenderableWidget(panels = new PanelSelectorWidget(parentScreen, 0, 0, width, getSelectedPanel()));

        // clear overlays
        contextMenu = null;
        tooltip = null;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        tick();
    }

    public void tick() {
        for (FiguraRenderable renderable : this.renderables()) {
            if (renderable instanceof FiguraTickable) {
                FiguraTickable tickable = (FiguraTickable) renderable;
                tickable.tick();
            }
        }

        renderables().removeIf(r -> r instanceof FiguraRemovable && ((FiguraRemovable) r).isRemoved());

    }

    public List<FiguraRenderable> renderables() {
        return renderables;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float delta) {
        // setup figura framebuffer
        // UIHelper.useFiguraGuiFramebuffer();

        // render background
        this.renderBackground(delta);

        // render contents
        super.drawScreen(mouseX, mouseY, delta);

        for (FiguraRenderable renderable : renderables) {
            renderable.draw(Minecraft.getMinecraft(), mouseX, mouseY, delta);
        }

        // render overlays
        this.renderOverlays(mouseX, mouseY, delta);

        // restore vanilla framebuffer
        // UIHelper.useVanillaFramebuffer();
    }

    public void renderBackground(float delta) {
        // render
        float speed = Configs.BACKGROUND_SCROLL_SPEED.tempValue * 0.125f;
        for (ResourceLocation background : BACKGROUNDS) {
            UIHelper.renderAnimatedBackground(background, 0, 0, this.width, this.height, 64, 64, speed, delta);
            speed /= 0.5;
        }
    }

    public void renderOverlays(int mouseX, int mouseY, float delta) {
        if (Configs.GUI_FPS.value)
            fontRenderer.drawString(ClientAPI.getFPS() + " fps", 1, 1, 0xFFFFFF);

        // render context
        if (contextMenu != null && contextMenu.isVisible()) {
            // translate the stack here because of nested contexts
            GlStateManager.pushMatrix();
            GlStateManager.translate(0f, 0f, 500f);
            contextMenu.draw(Minecraft.getMinecraft(), mouseX, mouseY, delta);
            GlStateManager.popMatrix();
        }

        // render tooltip
        if (tooltip != null)
            UIHelper.renderTooltip(tooltip, mouseX, mouseY, true);

        tooltip = null;
    }

    @Override
    public void onGuiClosed() {
        this.mc.displayGuiScreen(parentScreen);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        //fix mojang focusing for text fields
        for (FiguraGuiEventListener listener : this.getChildren()) {
            if (listener instanceof TextField) {
                TextField field = (TextField) listener;
                field.getField().setFocused(field.isEnabled() && field.mouseOver(mouseX, mouseY));
            }
        }
        if (this.contextMenuClick(mouseX, mouseY, mouseButton))
            return;

        for (FiguraGuiEventListener guiEventListener : this.getChildren()) {
            if (!guiEventListener.mouseButtonClicked(mouseX, mouseY, mouseButton)) continue;
            this.setFocusedElement(guiEventListener);
            return;
        }
    }

    private void setFocusedElement(FiguraGuiEventListener guiEventListener) {
        this.selectedListener = guiEventListener;
    }

    private List<FiguraGuiEventListener> getChildren() {
        return children;
    }


    public boolean contextMenuClick(int mouseX, int mouseY, int button) {
        // attempt to run context first
        if (contextMenu != null && contextMenu.isVisible()) {
            // attempt to click on the context menu
            boolean clicked = contextMenu.mouseButtonClicked(mouseX, mouseY, button);

            // then try to click on the category container and suppress it
            // let the category handle the context menu visibility
            if (!clicked && contextMenu.parent != null && contextMenu.parent.mouseButtonClicked(mouseX, mouseY, button))
                return true;

            // otherwise, remove visibility and suppress the click only if we clicked on the context
            contextMenu.setVisible(false);
            return clicked;
        }

        // no interaction was made
        return false;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long timeSinceLastClick) {
        // yeet mouse 0 and isDragging check
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution scaledResolution = new ScaledResolution(minecraft);
        double dragX = (mouseX - this.prevMouseX) * scaledResolution.getScaledWidth_double() / minecraft.displayWidth;
        double dragY = (mouseY - this.prevMouseY) * scaledResolution.getScaledHeight_double() / minecraft.displayHeight;

        if (this.getFocusedListener() != null)
            this.getFocusedListener().mouseDragged(Minecraft.getMinecraft(), mouseX, mouseY, button, dragX, dragY);

        prevMouseX = mouseX;
        prevMouseY = mouseY;
    }

    private FiguraGuiEventListener getFocusedListener() {
        return selectedListener;
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int button) {
        // better check for mouse released when outside element boundaries
        boolean bool = this.getFocusedListener() != null && this.getFocusedListener().mouseButtonReleased(mouseX, mouseY, button);

        // remove focused when clicking
        if (bool) setFocusedElement(null);

    }

    @Override
    public void handleMouseInput() {
        int mouseScroll = Mouse.getEventDWheel();
        double mouseMoveScroll = Math.signum(mouseScroll) * 1.0; // TODO: Make this 1.0, sensitivity, a config option along with discrete scrolling
        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

        if (mouseScrolled(mouseX, mouseY, mouseMoveScroll))
            return;

        super.handleMouseInput();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        // hide previous context
        if (contextMenu != null)
            contextMenu.setVisible(false);

        // fix scrolling targeting only one child
        boolean ret = false;
        for (FiguraGuiEventListener child : this.getChildren()) {
            if (child.mouseOver(mouseX, mouseY))
                ret = ret || child.mouseScroll(mouseX, mouseY, amount);
        }
        return ret;
    }

    @Override
    public void keyTyped(char keyCode, int scanCode) {
        egg += (char) keyCode;
        egg = egg.substring(1);
        if (EGG.equals(egg)) {
            Minecraft.getMinecraft().displayGuiScreen(new GameScreen(this));
            return;
        }

        if (getChildren().contains(panels) && panels.cycleTab(scanCode))
            return;

        if (scanCode == Keyboard.KEY_ESCAPE && contextMenu != null && contextMenu.isVisible()) {
            contextMenu.setVisible(false);
            return;
        }
        super.keyTyped(keyCode, scanCode);

        if (this.getFocusedListener() != null)
            this.getFocusedListener().pressedKey(keyCode, scanCode);
    }

    protected <T extends FiguraGuiEventListener & FiguraRenderable> T addRenderableWidget(T widget) {
        this.renderables.add(widget);
        return addWidget(widget);
    }

    protected void removeWidget(FiguraGuiEventListener child) {
        if (child instanceof FiguraRenderable)
            this.renderables.remove(child);
        this.children.remove(child);
    }

    protected <T extends FiguraRenderable> T addRenderableOnly(T drawable) {
        this.renderables.add(drawable);
        return drawable;
    }

    protected <T extends AbstractFiguraWidget> T addButton(T button) {
        this.buttons.add(button);
        return this.addWidget(button);
    }

    protected <T extends FiguraGuiEventListener> T addWidget(T listener) {
        this.children.add(listener);
        return listener;
    }

    public ITextComponent getTitle() {
        return title;
    }
}
