package org.figuramc.figura.mixin.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.gui.screens.WardrobeScreen;
import org.figuramc.figura.gui.widgets.Button;
import org.figuramc.figura.utils.FiguraIdentifier;
import org.figuramc.figura.utils.FiguraText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngameMenu.class)
public class GuiIngameMenuMixin extends GuiScreen {

    protected GuiIngameMenuMixin() {
        super();
    }

    @Unique
    private static final ResourceLocation FIGURA_ICON = new FiguraIdentifier("textures/gui/icon.png");

    @Unique
    private Button figura$wardrobeButton;

    @Inject(method = "initGui", at = @At("RETURN"))
    private void createPauseMenuButton(CallbackInfo ci) {
        int x, y;

        int config = Configs.BUTTON_LOCATION.value;
        switch (config) {
            case 1: // top left
                x = 4;
                y = 4;
                break;
            case 2: // top right
                x = this.width - 68;
                y = 4;
                break;
            case 3: // bottom left
                x = 4;
                y = this.height - 24;
                break;
            case 4: // bottom right
                x = this.width - 68;
                y = this.height - 24;
                break;
            default: // icon
                x = this.width / 2 + 106;
                y = this.height / 4 + 80;
                break;
        }

        if (config > 0) { // button
            figura$wardrobeButton = new Button(x, y, 64, 20, new FiguraText(), null, btn -> this.mc.displayGuiScreen(new WardrobeScreen(this))) {
                @Override
                public void drawWidget(Minecraft mc, int mouseX, int mouseY, float delta) {
                    TextFormatting color;
                    if (this.isHovered() || this.isFocused()) {
                        color = TextFormatting.AQUA;
                    } else if (AvatarManager.panic) {
                        color = TextFormatting.GRAY;
                    } else {
                        color = TextFormatting.WHITE;
                    }
                    setMessage(getMessage().createCopy().setStyle(new Style().setColor(color)));

                    renderVanillaBackground(mc, mouseX, mouseY, delta);
                    super.drawWidget(mc, mouseX, mouseY, delta);
                }

                @Override
                protected void renderDefaultTexture(Minecraft mc, float delta) {}
            };
        } else { // icon
            figura$wardrobeButton = new Button(x, y, 20, 20, 0, 0, 20, FIGURA_ICON, 60, 20, null, btn -> this.mc.displayGuiScreen(new WardrobeScreen(this))) {
                @Override
                public void drawWidget(Minecraft mc, int mouseX, int mouseY, float delta) {
                    renderVanillaBackground(mc, mouseX, mouseY, delta);
                    super.drawWidget(mc, mouseX, mouseY, delta);
                }

                @Override
                protected int getU() {
                    int u = super.getU();
                    if (u == 1 && AvatarManager.panic)
                        return 0;
                    return u;
                }
            };
        }
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    public void drawFiguraButton(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (figura$wardrobeButton != null)
            figura$wardrobeButton.draw(mc, mouseX, mouseY, partialTicks);
    }
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (figura$wardrobeButton != null && figura$wardrobeButton.mouseButtonClicked(mouseX, mouseY, mouseButton))
            return;

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }
}
