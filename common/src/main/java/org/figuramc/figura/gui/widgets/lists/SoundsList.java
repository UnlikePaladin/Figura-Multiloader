package org.figuramc.figura.gui.widgets.lists;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.gui.widgets.AbstractContainerElement;
import org.figuramc.figura.gui.widgets.Label;
import org.figuramc.figura.gui.widgets.ParentedButton;
import org.figuramc.figura.lua.api.sound.LuaSound;
import org.figuramc.figura.lua.api.sound.SoundAPI;
import org.figuramc.figura.utils.FiguraIdentifier;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.MathUtils;
import org.figuramc.figura.utils.TextUtils;
import org.figuramc.figura.utils.ui.UIHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SoundsList extends AbstractList {

    private final List<SoundElement> sounds = new ArrayList<>();

    private final Avatar owner;
    private SoundElement selected;

    public SoundsList(int x, int y, int width, int height, Avatar owner) {
        super(x, y, width, height);
        this.owner = owner;
        updateList();

        Label noOwner, noSounds;
        this.children.add(noOwner = new Label(new FiguraText("gui.error.no_avatar").setStyle(new Style().setColor(TextFormatting.YELLOW)), x + width / 2, y + height / 2, TextUtils.Alignment.CENTER, 0));
        this.children.add(noSounds = new Label(new FiguraText("gui.error.no_sounds").setStyle(new Style().setColor(TextFormatting.YELLOW)), x + width / 2, y + height / 2, TextUtils.Alignment.CENTER, 0));
        noOwner.centerVertically = noSounds.centerVertically = true;

        noOwner.setVisible(owner == null);
        noSounds.setVisible(!noOwner.isVisible() && sounds.isEmpty());
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY, float delta) {
        // background and scissors
        UIHelper.renderSliced(getX(), getY(), getWidth(), getHeight(), UIHelper.OUTLINE_FILL);
        UIHelper.setupScissor(getX() + scissorsX, getY() + scissorsY, getWidth() + scissorsWidth, getHeight() + scissorsHeight);

        if (!sounds.isEmpty())
            updateEntries();

        // children
        super.draw(mc, mouseX, mouseY, delta);

        // reset scissor
        UIHelper.disableScissor();
    }

    private void updateEntries() {
        // scrollbar
        int totalHeight = -4;
        for (SoundElement sound : sounds)
            totalHeight += sound.getHeight() + 8;
        int entryHeight = sounds.isEmpty() ? 0 : totalHeight / sounds.size();

        scrollBar.setVisible(totalHeight > getHeight());
        scrollBar.setScrollRatio(entryHeight, totalHeight - getHeight());

        // render list
        int xOffset = scrollBar.isVisible() ? 4 : 11;
        int yOffset = scrollBar.isVisible() ? (int) -(MathUtils.lerp(scrollBar.getScrollProgress(), -4, totalHeight - getHeight())) : 4;
        for (SoundElement sound : sounds) {
            sound.setX(getX() + xOffset);
            sound.setY(getY() + yOffset);
            yOffset += sound.getHeight() + 8;
        }
    }

    private void updateList() {
        // clear old widgets
        sounds.forEach(children::remove);

        // add new sounds
        if (owner == null)
            return;

        for (Map.Entry<String, byte[]> entry : owner.customSounds.entrySet()) {
            SoundElement sound = new SoundElement(getWidth() - 22, entry.getKey(), entry.getValue(), this, owner);
            sounds.add(sound);
            children.add(sound);
        }

        sounds.sort(SoundElement::compareTo);

        if (!sounds.isEmpty())
            selected = sounds.get(0);
    }

    public LuaSound getSound() {
        return selected != null ? selected.getSound() : null;
    }

    private static class SoundElement extends AbstractContainerElement implements Comparable<SoundElement> {

        private final ITextComponent size;
        private final String name;
        private final byte[] sound;
        private final Avatar owner;
        private final SoundsList parent;

        private final ParentedButton play, stop;

        public SoundElement(int width, String name, byte[] sound, SoundsList parent, Avatar owner) {
            super(0, 0, width, 20);
            this.name = name;
            this.sound = sound;
            this.owner = owner;
            this.parent = parent;

            int len = owner.nbt.getCompoundTag("sounds").getByteArray(name).length;
            this.size = new TextComponentString("(" + MathUtils.asFileSize(len) + ")").setStyle(new Style().setColor(TextFormatting.GRAY));

            // play button
            children.add(0, play = new ParentedButton(0, 0, 20, 20, 0, 0, 20, new FiguraIdentifier("textures/gui/play.png"), 60, 20, new FiguraText("gui.sound.play"), this, button -> {}) {
                @Override
                public void playPressedSound(SoundHandler soundManager) {
                    Vec3d vec =  Minecraft.getMinecraft().player == null ? new Vec3d(0, 0, 0) : Minecraft.getMinecraft().player.getPositionVector();
                    getSound().pos(vec.x, vec.y, vec.z).play();
                }
            });

            // stop button
            children.add(stop = new ParentedButton(0, 0, 20, 20, 0, 0, 20, new FiguraIdentifier("textures/gui/stop.png"), 60, 20, new FiguraText("gui.sound.stop"), this,
                    button -> SoundAPI.getSoundEngine().figura$stopSound(owner.owner, name))
            );
        }

        @Override
        public void draw(Minecraft mc, int mouseX, int mouseY, float delta) {
            if (!this.isVisible()) return;

            int x = getX();
            int y = getY();
            int width = getWidth();
            int height = getHeight();

            // selected outline
            if (parent.selected == this)
                UIHelper.fillOutline(x - 1, y - 1, width + 2, height + 2, 0xFFFFFFFF);

            // vars
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            int textY = y + height / 2 - font.FONT_HEIGHT / 2;

            // hovered arrow
            setHovered(mouseOver(mouseX, mouseY));
            if (isHovered()) font.drawString(HOVERED_ARROW.getFormattedText(), x + 4, textY, 0xFFFFFF);

            // render name
            font.drawString(this.name, x + 16, textY, 0xFFFFFF);

            // render size
            font.drawString(size.getFormattedText(), x + width - 96 - font.getStringWidth(size.getFormattedText()), textY, 0xFFFFFF);

            // render children
            super.draw(mc, mouseX, mouseY, delta);
        }

        @Override
        public boolean mouseOver(double mouseX, double mouseY) {
            return this.parent.isInsideScissors(mouseX, mouseY) && super.mouseOver(mouseX, mouseY);
        }

        @Override
        public boolean mouseButtonClicked(int mouseX, int mouseY, int button) {
            boolean clicked = super.mouseButtonClicked(mouseX, mouseY, button);
            if (!clicked) {
                if (mouseOver(mouseX, mouseY)) {
                    parent.selected = this;
                    return true;
                }
            }
            return clicked;
        }

        @Override
        public void setX(int x) {
            super.setX(x);
            play.setX(x + getWidth() - 64);
            stop.setX(x + getWidth() - 40);
        }

        @Override
        public void setY(int y) {
            super.setY(y);
            play.setY(y);
            stop.setY(y);
        }

        public LuaSound getSound() {
            return new LuaSound(sound, name, owner);
        }

        @Override
        public int compareTo(SoundElement o) {
            return this.name.compareTo(o.name);
        }
    }
}
