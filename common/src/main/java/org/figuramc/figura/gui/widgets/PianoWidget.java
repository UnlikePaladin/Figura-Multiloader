package org.figuramc.figura.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.ducks.extensions.StyleExtension;
import org.figuramc.figura.lua.api.sound.LuaSound;
import org.figuramc.figura.utils.ui.UIHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PianoWidget extends AbstractContainerElement {

    private static final int TOTAL_KEYS = 29; // includes "missing" black keys
    private static final String[] NOTES = {"F", "G", "A", "B", "C", "D", "E"};

    private final List<Key> keys = new ArrayList<>();
    private final Supplier<LuaSound> soundSupplier;
    private Key hovered;
    public boolean pressed;

    public PianoWidget(int x, int y, int width, int height, Supplier<LuaSound> soundSupplier) {
        super(x, y, width, height);
        this.soundSupplier = soundSupplier;

        double keyWidth = (width - 2) / Math.ceil(TOTAL_KEYS / 2d);
        float j = 0f;

        List<Key> sharpKeys = new ArrayList<>();
        for (int i = 1, note = 0, count = 0; i <= TOTAL_KEYS; i++, j += 0.5f) {
            // skip "empty" black keys
            if (i % 2 == 1 && (i % 7 == 0 || (i + 1) % 7 == 0))
                continue;

            // variables
            boolean isSharp = i % 2 == 1;
            int keyX = x + 1 + (int) (keyWidth * j);
            if (!isSharp) note = (note + 1) % NOTES.length;

            // create key
            Key key = new Key(keyX, y + 1, (int) Math.round(keyWidth), isSharp ? height / 2 : height - 2, NOTES[note] + (isSharp ? "#" : ""), (float) Math.pow(2, (count - 12) / 12f), isSharp, this);

            // add key
            count++;
            if (!isSharp) children.add(key);
            else sharpKeys.add(key);
            keys.add(key);
        }

        children.addAll(sharpKeys);
    }

    @Override
    public void draw(Minecraft minecraft, int mouseX, int mouseY, float delta) {
        this.setHovered(this.mouseOver(mouseX, mouseY));

        // background
        UIHelper.renderSliced(getX(), getY(), getWidth(), getHeight(), UIHelper.OUTLINE_FILL);

        Key lastHovered = hovered;

        // define visible key
        for (Key key : keys)
            key.setHovered(key.mouseOver(mouseX, mouseY));

        // render children
        super.draw(minecraft, mouseX, mouseY, delta);

        if (pressed && hovered != lastHovered && hovered != null)
            hovered.run();
    }

    @Override
    public boolean mouseOver(double mouseX, double mouseY) {
        return false;
    }

    private static class Key extends ParentedButton {
        private final PianoWidget parent;
        private final float pitch;
        private final boolean isSharp;

        public Key(int x, int y, int width, int height, String key, float pitch, boolean isSharp, PianoWidget parent) {
            super(x, y, width, height, new TextComponentString(key), parent, button -> {});
            this.parent = parent;
            this.pitch = pitch;
            this.isSharp = isSharp;
        }

        @Override
        public void playPressedSound(SoundHandler soundManager) {
            LuaSound sound = parent.soundSupplier.get();
            if (sound != null) {
                Vec3d vec =  Minecraft.getMinecraft().player == null ? new Vec3d(0, 0, 0) : Minecraft.getMinecraft().player.getPositionVector();
                sound.pos(vec.x, vec.y, vec.z).pitch(pitch).play();
            } else {
                soundManager.playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.BLOCK_NOTE_HARP, pitch));
            }
        }

        @Override
        public void draw(Minecraft minecraft, int mouseX, int mouseY, float delta) {
            if (!this.isVisible())
                return;

            // render button
            this.drawWidget(minecraft, mouseX, mouseY, delta);
        }

        @Override
        public void drawWidget(Minecraft mc, int mouseX, int mouseY, float delta) {
            UIHelper.fillRounded(getX(), getY(), getWidth(), getHeight(), (isSharp ? 0 : 0xFFFFFF) + (0xFF << 24));
            UIHelper.renderSliced(getX(), getY(), getWidth(), getHeight(), UIHelper.OUTLINE);

            if (this.isHovered())
                UIHelper.fillRounded(getX(), getY(), getWidth(), getHeight(), (((StyleExtension)FiguraMod.getAccentColor()).getRGBColor()) + (0xA0 << 24));

            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            ITextComponent message = getMessage();
            int x = getX() + getWidth() / 2 - font.getStringWidth(message.getFormattedText()) / 2;
            int y = getY() + getHeight() / 2 - font.FONT_HEIGHT / 2;
            if (!isSharp)
                y += getHeight() / 4;
            font.drawString(message.getFormattedText(), x, y, (isSharp ? 0xFFFFFF : 0));
        }

        @Override
        public boolean mouseOver(double mouseX, double mouseY) {
            boolean over = super.mouseOver(mouseX, mouseY);

            // checking against this
            if (parent.hovered == this) {
                parent.hovered = over ? this : null;
                return over;
            }

            // not hovered, skip
            if (!over) return false;

            // checking against no one
            if (parent.hovered == null) {
                parent.hovered = this;
                return true;
            }

            // checking against sharp
            if (parent.hovered.isSharp) {
                return false;
            }

            // checking against whites
            if (this.isSharp) {
                parent.hovered.setHovered(false);
                parent.hovered = this;
                return true;
            }

            return false;
        }
    }
}
