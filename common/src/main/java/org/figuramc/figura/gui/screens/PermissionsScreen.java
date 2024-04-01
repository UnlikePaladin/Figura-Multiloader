package org.figuramc.figura.gui.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.extensions.StyleExtension;
import org.figuramc.figura.gui.FiguraToast;
import org.figuramc.figura.gui.widgets.Button;
import org.figuramc.figura.gui.widgets.EntityPreview;
import org.figuramc.figura.gui.widgets.SliderWidget;
import org.figuramc.figura.gui.widgets.SwitchButton;
import org.figuramc.figura.gui.widgets.lists.PermissionsList;
import org.figuramc.figura.gui.widgets.lists.PlayerList;
import org.figuramc.figura.gui.widgets.permissions.AbstractPermPackElement;
import org.figuramc.figura.gui.widgets.permissions.PlayerPermPackElement;
import org.figuramc.figura.permissions.PermissionManager;
import org.figuramc.figura.permissions.PermissionPack;
import org.figuramc.figura.permissions.Permissions;
import org.figuramc.figura.utils.FiguraIdentifier;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.MathUtils;
import org.figuramc.figura.utils.ui.UIHelper;
import org.lwjgl.input.Keyboard;

import java.util.UUID;

public class PermissionsScreen extends AbstractPanelScreen {

    // -- widgets -- //
    private PlayerList playerList;
    private EntityPreview entityWidget;

    private SliderWidget slider;

    private PermissionsList permissionsList;
    private SwitchButton expandButton;
    private Button reloadAll;
    private Button back;
    private Button resetButton;
    private SwitchButton precisePermissions;

    // -- widget logic -- //
    private float listYPrecise;
    private float expandYPrecise;
    private float resetYPrecise;

    private boolean expanded;
    private PlayerPermPackElement dragged = null;

    public PermissionsScreen(GuiScreen parentScreen) {
        super(parentScreen, new FiguraText("gui.panels.title.permissions"));
    }

    @Override
    public void initGui() {
        super.initGui();

        int middle = this.width / 2;
        int listWidth = Math.min(middle - 6, 208);
        int lineHeight =  fontRenderer.FONT_HEIGHT;

        int entitySize = (int) Math.min(height - 95 - lineHeight * 1.5 - (FiguraMod.debugModeEnabled() ? 24 : 0), listWidth);
        int modelSize = 11 * entitySize / 29;
        int entityX = Math.max(middle + (listWidth - entitySize) / 2 + 1, middle + 2);

        // entity widget
        entityWidget = new EntityPreview(entityX, 28, entitySize, entitySize, modelSize, -15f, 30f, Minecraft.getMinecraft().player, this);

        // permission slider and list
        slider = new SliderWidget(middle + 2, (int) (entityWidget.getY() + entityWidget.getHeight() + lineHeight * 1.5 + 20), listWidth, 11, 1d, 5, true) {
            @Override
            public void drawWidget(Minecraft mc, int mouseX, int mouseY, float delta) {
                super.drawWidget(mc, mouseX, mouseY, delta);

                PermissionPack selectedPack = playerList.selectedEntry.getPack();
                ITextComponent text = selectedPack.getCategoryName();

                int x = (int) (this.getX() + this.getWidth() / 2f - fontRenderer.getStringWidth(text.getFormattedText()) * 0.75f);
                int y = this.getY() - 4 - fontRenderer.FONT_HEIGHT * 2;

                GlStateManager.pushMatrix();
                GlStateManager.translate(x, y, 0f);
                GlStateManager.scale(1.5f, 1.5f, 1f);
                UIHelper.renderOutlineText(fontRenderer, text, 0, 0, 0xFFFFFF, 0x202020);
                GlStateManager.popMatrix();

                ITextComponent info = new TextComponentString("?").setStyle(((StyleExtension)new Style()).setFont(UIHelper.UI_FONT));
                int color = 0x404040;

                int width = fontRenderer.getStringWidth(info.getFormattedText());
                x = Math.min((int) (x + fontRenderer.getStringWidth(text.getFormattedText()) * 1.5f + fontRenderer.getStringWidth("  ")), PermissionsScreen.this.width - width);
                y += fontRenderer.FONT_HEIGHT * 0.25f;

                if (UIHelper.isMouseOver(x, y, width, fontRenderer.FONT_HEIGHT, mouseX, mouseY)) {
                    color = 0xFFFFFF;
                    UIHelper.setTooltip(selectedPack.getCategory().info);
                }

                fontRenderer.drawStringWithShadow(info.getFormattedText(), x, y, color);
            }
        };
        permissionsList = new PermissionsList(middle + 2, height, listWidth, height - 54);

        // -- left -- //

        // player list
        addRenderableWidget(playerList = new PlayerList(middle - listWidth - 2, 28, listWidth, height - 32, this));

        // -- right -- //

        // add entity widget
        addRenderableWidget(entityWidget);

        // -- bottom -- //

        // add slider
        addRenderableWidget(slider);

        // reload all
        int bottomButtonsWidth = (listWidth - 24) / 2 - 2;
        addRenderableWidget(reloadAll = new Button(middle + 2, height - 24, bottomButtonsWidth, 20, new FiguraText("gui.permissions.reload_all"), null, bx -> {
            AvatarManager.clearAllAvatars();
            FiguraToast.sendToast(new FiguraText("toast.reload_all"));
        }));

        // back button
        addRenderableWidget(back = new Button(middle + 6 + bottomButtonsWidth, height - 24, bottomButtonsWidth, 20, new FiguraText("gui.done"), null, bx -> onGuiClosed()));

        // expand button
        addRenderableWidget(expandButton = new SwitchButton( middle + listWidth - 18, height - 24, 20, 20, 0, 0, 20, new FiguraIdentifier("textures/gui/expand_v.png"), 60, 40, new FiguraText("gui.permissions.expand_permissions.tooltip"), btn -> {
            expanded = expandButton.isToggled();

            // hide widgets
            entityWidget.setVisible(!expanded);
            slider.setVisible(!expanded);
            slider.setActive(!expanded);
            reloadAll.setVisible(!expanded);
            back.setVisible(!expanded);

            // update expand button
            expandButton.setTooltip(expanded ? new FiguraText("gui.permissions.minimize_permissions.tooltip") : new FiguraText("gui.permissions.expand_permissions.tooltip"));

            // set reset button activeness
            resetButton.setActive(expanded);
        }));

        // reset all button
        addRenderableWidget(resetButton = new Button(middle + 2, height, 60, 20, new FiguraText("gui.permissions.reset"), null, btn -> {
            // clear permissions
            PermissionPack pack = playerList.selectedEntry.getPack();
            pack.clear();
            updatePermissions(pack);
        }));

        addRenderableWidget(precisePermissions = new SwitchButton(middle + 66, height, listWidth - 88, 20, new FiguraText("gui.permissions.precise"), false) {
            @Override
            public void widgetPressed(int mouseX, int mouseY) {
                super.widgetPressed(mouseX, mouseY);
                permissionsList.precise = this.isToggled();
                permissionsList.updateList(playerList.selectedEntry.getPack());
            }
        });
        precisePermissions.setUnderline(false);

        // add permissions list
        addRenderableWidget(permissionsList);

        listYPrecise = permissionsList.getY();
        expandYPrecise = expandButton.getY();
        resetYPrecise = resetButton.getY();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float delta) {
        // set entity to render
        AbstractPermPackElement entity = playerList.selectedEntry;
        World world = Minecraft.getMinecraft().world;
        if (world != null && entity instanceof PlayerPermPackElement) {
            PlayerPermPackElement player = (PlayerPermPackElement) entity;
            entityWidget.setEntity(world.getPlayerEntityByUUID(UUID.fromString(player.getPack().name)));
        } else
            entityWidget.setEntity(null);

        // expand animation
        float lerpDelta = MathUtils.magicDelta(0.6f, delta);

        listYPrecise = (float) MathUtils.lerp(lerpDelta, listYPrecise, expandButton.isToggled() ? 50f : height + 1);
        this.permissionsList.setY((int) listYPrecise);

        expandYPrecise = (float) MathUtils.lerp(lerpDelta, expandYPrecise, expandButton.isToggled() ? listYPrecise - 22f : listYPrecise - 24f);
        this.expandButton.setY((int) expandYPrecise);

        resetYPrecise = (float) MathUtils.lerp(lerpDelta, resetYPrecise, expandButton.isToggled() ? listYPrecise - 22f : height);
        this.resetButton.setY((int) resetYPrecise);
        this.precisePermissions.setY((int) resetYPrecise);

        // render
        super.drawScreen(mouseX, mouseY, delta);
    }

    @Override
    public void renderOverlays(int mouseX, int mouseY, float delta) {
        if (dragged != null && dragged.dragged)
            dragged.renderDragged(mc, mouseX, mouseY, delta);

        super.renderOverlays(mouseX, mouseY, delta);
    }

    @Override
    public void onGuiClosed() {
        PermissionManager.saveToDisk();
        super.onGuiClosed();
    }

    @Override
    public void keyTyped(char keyCode, int scanCode) {
        // yeet ESC key press for collapsing the card list
        if (scanCode == Keyboard.KEY_ESCAPE && expandButton.isToggled()) {
            expandButton.widgetPressed(0,0);
            return;
        }

        super.keyTyped(keyCode, scanCode);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        dragged = null;

        if (button == 0 && playerList.selectedEntry instanceof PlayerPermPackElement && ((PlayerPermPackElement) playerList.selectedEntry).mouseOver(mouseX, mouseY)) {
            PlayerPermPackElement element = (PlayerPermPackElement) playerList.selectedEntry;
            dragged = element;
            element.anchorX = (int) mouseX;
            element.anchorY = (int) mouseY;
            element.initialY = element.getY();
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long timeSinceLastClick) {
        if (dragged != null) {
            dragged.index = playerList.getCategoryAt(mouseY);
            dragged.dragged = true;
            return;
        }

        super.mouseClickMove(mouseX, mouseY, button, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int button) {
        super.mouseReleased(mouseX, mouseY, button);

        if (dragged == null || !dragged.dragged)
            return;

        PermissionPack pack = dragged.getPack();
        Permissions.Category category = Permissions.Category.indexOf(Math.min(dragged.index, Permissions.Category.values().length - 1));

        pack.setCategory(PermissionManager.CATEGORIES.get(category));
        updatePermissions(pack);

        dragged.dragged = false;
        dragged = null;
    }

    public void updatePermissions(PermissionPack pack) {
        // reset run action
        slider.setAction(null);

        // set slider active only for players
        slider.setActive(pack instanceof PermissionPack.PlayerPermissionPack && !expanded);

        // set step sizes
        slider.setMax(Permissions.Category.values().length);

        // set slider progress
        slider.setScrollProgress(pack.getCategory().index / (slider.getMax() - 1d));

        // set new slider action
        slider.setAction(scroll -> {
            // set new permissions category
            Permissions.Category category = Permissions.Category.indexOf(((SliderWidget) scroll).getIntValue());
            pack.setCategory(PermissionManager.CATEGORIES.get(category));

            // and update the advanced permissions
            permissionsList.updateList(pack);
        });

        // update advanced permissions list
        permissionsList.updateList(pack);
    }
}
