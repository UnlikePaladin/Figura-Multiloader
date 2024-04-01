package org.figuramc.figura.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.entries.FiguraScreen;
import org.figuramc.figura.gui.screens.*;
import org.figuramc.figura.utils.FiguraIdentifier;
import org.figuramc.figura.utils.Pair;
import org.figuramc.figura.utils.ui.UIHelper;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class PanelSelectorWidget extends AbstractContainerElement {

    public static final ResourceLocation BACKGROUND = new FiguraIdentifier("textures/gui/panels_background.png");

    private static final List<Function<GuiScreen, Pair<GuiScreen, PanelIcon>>> PANELS = new ArrayList<Function<GuiScreen, Pair<GuiScreen, PanelIcon>>>() {{
        add(s -> Pair.of(new ProfileScreen(s), PanelIcon.PROFILE));
        add(s -> Pair.of(new BrowserScreen(s), PanelIcon.BROWSER));
        add(s -> Pair.of(new WardrobeScreen(s), PanelIcon.WARDROBE));
        add(s -> Pair.of(new PermissionsScreen(s), PanelIcon.PERMISSIONS));
        add(s -> Pair.of(new ConfigScreen(s), PanelIcon.SETTINGS));
        add(s -> Pair.of(new HelpScreen(s), PanelIcon.HELP));
    }};

    // TODO - remove this when we actually implement those panels
    private static final List<Integer> PANELS_BLACKLIST = Arrays.asList(0, 1);

    private final List<PanelButton> buttons = new ArrayList<>();

    private PanelButton selected;

    public PanelSelectorWidget(GuiScreen parentScreen, int x, int y, int width, Class<? extends GuiScreen> selected) {
        super(x, y, width, 28);

        // buttons

        // size variables
        int buttonCount = PANELS.size() - (FiguraMod.debugModeEnabled() ? 0 : PANELS_BLACKLIST.size());
        int buttonWidth = Math.min(Math.max((width - 4) / buttonCount - 4, 24), 96) + 4;
        int spacing = (width - (4 + buttonWidth * buttonCount)) / 2;

        for (int i = 0; i < PANELS.size(); i++) {
            // skip blacklist
            if (!FiguraMod.debugModeEnabled() && PANELS_BLACKLIST.contains(i))
                continue;

            // get button data
            Pair<GuiScreen, PanelIcon> panel = PANELS.get(i).apply(parentScreen);
            GuiScreen s = panel.getFirst();
            PanelIcon icon = panel.getSecond();
            int buttonX = 4 + buttonWidth * buttons.size() + spacing;

            // create button
            createPanelButton(s, icon, s.getClass() == selected, buttonX, buttonWidth - 4);
        }

        // locked buttons
        if (FiguraMod.debugModeEnabled()) {
            for (int i : PANELS_BLACKLIST) {
                PanelButton button = buttons.get(i);
                button.setMessage(button.getMessage().createCopy().setStyle(new Style().setColor(TextFormatting.RED)));
            }
        }
    }

    public static void initEntryPoints(Set<FiguraScreen> set) {
        for (FiguraScreen figuraScreen : set) {
            PanelIcon icon = figuraScreen.getPanelIcon();
            PANELS.add(s -> Pair.of(figuraScreen.getScreen(s), icon == null ? PanelIcon.OTHER : icon));
        }
    }

    private void createPanelButton(GuiScreen panel, PanelIcon icon, boolean toggled, int x, int width) {
        // create button
        PanelButton button = new PanelButton(x, getY(), width, getHeight() - 4, (panel instanceof AbstractPanelScreen ? ((AbstractPanelScreen) panel).getTitle() : new TextComponentString("")), icon, this, bx -> Minecraft.getMinecraft().displayGuiScreen(panel));
        button.shouldHaveBackground(false);
        if (toggled) this.selected = button;

        // add button
        buttons.add(button);
        children.add(button);
    }

    @Override
    public void draw(Minecraft minecraft, int mouseX, int mouseY, float delta) {
        UIHelper.renderSliced(getX(), getY(), selected.getX() - getX(), getHeight() - 4, BACKGROUND);
        UIHelper.renderSliced(selected.getX() + selected.getWidth(), getY(), getWidth() - selected.getX() - selected.getWidth(), getHeight() - 4, BACKGROUND);
        super.draw(minecraft, mouseX, mouseY, delta);
    }

    public boolean cycleTab(int keyCode) {
        if (GuiScreen.isCtrlKeyDown()) {
            int i = this.getNextPanel(keyCode);
            if (i >= 0 && i < buttons.size()) {
                PanelButton button = buttons.get(i);
                button.run();
                return true;
            }
        }

        return false;
    }

    private int getNextPanel(int keyCode) {
        // numbers
        if (keyCode >= Keyboard.KEY_1 && keyCode <= Keyboard.KEY_9)
            return keyCode - Keyboard.KEY_1;

        // tab
        if (keyCode == Keyboard.KEY_TAB) {
            // get current button
            int index = buttons.indexOf(selected);

            int i = GuiScreen.isShiftKeyDown() ? index - 1 : index + 1;
            return Math.floorMod(i, buttons.size());
        }

        return -1;
    }

    public enum PanelIcon {
        PROFILE(0),
        BROWSER(1),
        WARDROBE(2),
        PERMISSIONS(3),
        SETTINGS(4),
        HELP(5),
        OTHER(6);

        public final int uv;

        PanelIcon(int uv) {
            this.uv = uv;
        }
    }

    private static class PanelButton extends IconButton {

        public static final ResourceLocation TEXTURE = new FiguraIdentifier("textures/gui/panels_button.png");
        public static final ResourceLocation ICONS = new FiguraIdentifier("textures/gui/panels.png");

        private final PanelSelectorWidget parent;

        public PanelButton(int x, int y, int width, int height, ITextComponent text, PanelIcon icon, PanelSelectorWidget parent, ButtonAction pressAction) {
            super(x, y, width, height, 20 * icon.uv, 0, 20, ICONS, 140, 20, text, null, pressAction);
            this.parent = parent;
        }

        @Override
        public void drawWidget(Minecraft minecraft, int mouseX, int mouseY, float delta) {
            super.drawWidget(minecraft, mouseX, mouseY, delta);
            boolean iconOnly = iconsOnly();

            if (iconOnly && this.mouseOver(mouseX, mouseY))
                UIHelper.setTooltip(getMessage());
        }

        @Override
        protected void renderTexture(Minecraft mc, float delta) {
            UIHelper.renderSliced(this.x, this.y, getWidth(), getHeight(), isSelected() ? 24f : 0f, (this.isHovered()) ? 24f : 0f, 24, 24, 48, 48, TEXTURE);

            UIHelper.setupTexture(texture);
            int size = getTextureSize();
            UIHelper.blit(this.x + (iconsOnly() ? (getWidth() - size) / 2 : 2), this.y + (getHeight() - size) / 2 + (!isSelected() ? 2 : 0), size, size, u, v, regionSize, regionSize, textureWidth, textureHeight);
        }

        @Override
        protected void renderText(Minecraft mc, float delta) {
            if (iconsOnly())
                return;

            int size = getTextureSize();
            int offset = !isSelected() ? 3 : 0;
            ITextComponent message = isSelected() ? getMessage().createCopy().setStyle(new Style().setUnderlined(true)) : getMessage();
            UIHelper.renderCenteredScrollingText(message, this.x + 4 + size, this.y + offset, getWidth() - 6 - size, getHeight(), getTextColor());
        }

        private boolean iconsOnly() {
            return getWidth() < 72;
        }

        private boolean isSelected() {
            return parent.selected == this;
        }
    }
}