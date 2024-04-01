package org.figuramc.figura.gui.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.gui.widgets.Button;
import org.figuramc.figura.gui.widgets.IconButton;
import org.figuramc.figura.gui.widgets.Label;
import org.figuramc.figura.gui.widgets.ParticleWidget;
import org.figuramc.figura.utils.FiguraIdentifier;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.TextUtils;
import org.figuramc.figura.utils.ui.UIHelper;

public class HelpScreen extends AbstractPanelScreen {

    public static final ResourceLocation ICONS = new FiguraIdentifier("textures/gui/help_icons.png");
    public static final String LUA_VERSION = "5.2 - Figura";

    private IconButton kofi;

    public HelpScreen(GuiScreen parentScreen) {
        super(parentScreen, new FiguraText("gui.panels.title.help"));
    }

    @Override
    public void initGui() {
        super.initGui();

        int lineHeight = this.mc.fontRenderer.FONT_HEIGHT;
        int middle = width / 2;
        int labelWidth = Math.min(width - 8, 420);
        int y = 28;
        Style color = FiguraMod.getAccentColor();

        // in-game docs
        this.addRenderableWidget(new Title(new FiguraText("gui.help.docs").setStyle(color), middle, y, labelWidth));

        IconButton docs;
        this.addRenderableWidget(docs = new IconButton(middle - 60, y += lineHeight + 4, 120, 24, 20, 0, 20, ICONS, 60, 40, new FiguraText("gui.help.ingame_docs"), null, button -> this.mc.displayGuiScreen(new DocsScreen(this))));
        docs.setActive(false);
        this.addRenderableWidget(new IconButton(middle - 60, y += 28, 120, 24, 0, 0, 20, ICONS, 60, 40, new FiguraText("gui.help.lua_manual"), null, bx -> UIHelper.openURL(FiguraMod.Links.LuaManual.url).run()));
        this.addRenderableWidget(new IconButton(middle - 60, y += 28, 120, 24, 40, 0, 20, ICONS, 60, 40, new FiguraText("gui.help.external_wiki"), null, bx -> UIHelper.openURL(FiguraMod.Links.Wiki.url).run()));

        // links
        this.addRenderableWidget(new Title(new FiguraText("gui.help.links").setStyle(color), middle, y += 28, labelWidth));

        this.addRenderableWidget(new IconButton(middle - 124, y += lineHeight + 4, 80, 24, 0, 20, 20, ICONS, 60, 40, new TextComponentString("Discord"), null, bx -> UIHelper.openURL(FiguraMod.Links.Discord.url).run()));
        this.addRenderableWidget(new IconButton(middle - 40, y, 80, 24, 20, 20, 20, ICONS, 60, 40, new TextComponentString("GitHub"), null, bx -> UIHelper.openURL(FiguraMod.Links.Github.url).run()) {
            @Override
            public boolean mouseButtonClicked(int mouseX, int mouseY, int button) {
                if (Configs.EASTER_EGGS.value && (this.isHovered()) && this.mouseOver(mouseX, mouseY) && button == 1) {
                    int dim = getTextureSize();
                    int x = (int) (Math.random() * dim) + getX() + 2;
                    int y = (int) (Math.random() * dim) + getY() + 2;
                    addRenderableOnly(new ParticleWidget(x, y, EnumParticleTypes.HEART));

                    boolean purr = Math.random() < 0.95;
                    mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(purr ? SoundEvents.ENTITY_CAT_PURR : SoundEvents.ENTITY_CAT_AMBIENT, 1f));
                    return false;
                }

                return super.mouseButtonClicked(mouseX, mouseY, button);
            }
        });
        this.addRenderableWidget(kofi = new IconButton(middle + 44, y, 80, 24, 40, 20, 20, ICONS, 60, 40, new TextComponentString("Ko-fi"), null, b -> UIHelper.openURL(FiguraMod.Links.Kofi.url).run()));

        // texts
        this.addRenderableWidget(new Title(new FiguraText("gui.help.about").setStyle(color), middle, y += 28, labelWidth));

        this.addRenderableWidget(new Label(new FiguraText("gui.help.lua_version", new TextComponentString(LUA_VERSION).setStyle(color)), middle, y += lineHeight + 4, TextUtils.Alignment.CENTER));
        this.addRenderableWidget(new Label(new FiguraText("gui.help.figura_version", new TextComponentString(FiguraMod.VERSION.toString()).setStyle(color)), middle, y += lineHeight + 4, TextUtils.Alignment.CENTER));

        // back
        addRenderableWidget(new Button(middle - 60, height - 24, 120, 20, new FiguraText("gui.done"), null, bx -> onGuiClosed()));
    }

    @Override
    public void tick() {
        super.tick();

        if (FiguraMod.ticks % 5 == 0 && (kofi.isHovered())) {
            int x = (int) (Math.random() * kofi.getWidth()) + kofi.getX();
            int y = (int) (Math.random() * kofi.getHeight()) + kofi.getY();
            addRenderableOnly(new ParticleWidget(x, y, EnumParticleTypes.VILLAGER_HAPPY));
        }
    }

    private static class Title extends Label {

        private final int width;

        public Title(Object text, int x, int y, int width) {
            super(text, x, y, TextUtils.Alignment.CENTER);
            this.width = width;
        }

        @Override
        public void draw(Minecraft mc, int mouseX, int mouseY, float delta) {
            int x = getRawX();
            int y = getRawY();

            // lines
            int y0 = y + getHeight() / 2;
            int y1 = y0 + 1;

            int x0 = x - width / 2;
            int x1 = x - getWidth() / 2 - 4;
            UIHelper.fill(x0, y0, x1, y1, 0xFFFFFFFF);

            x0 = x + getWidth() / 2 + 4;
            x1 = x + width / 2;
            UIHelper.fill( 0, y0, x1, y1, 0xFFFFFFFF);

            // text
            super.draw(mc, mouseX, mouseY, delta);
        }
    }
}
