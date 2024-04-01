package org.figuramc.figura.gui.widgets.avatar;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.local.LocalAvatarFetcher;
import org.figuramc.figura.font.Emojis;
import org.figuramc.figura.gui.widgets.Button;
import org.figuramc.figura.gui.widgets.lists.AvatarList;
import org.figuramc.figura.mixin.font.FontRendererAccessor;
import org.figuramc.figura.utils.FiguraIdentifier;
import org.figuramc.figura.utils.FileTexture;
import org.figuramc.figura.utils.TextUtils;
import org.figuramc.figura.utils.ui.UIHelper;

public class AvatarWidget extends AbstractAvatarWidget {

    public static final ResourceLocation MISSING_ICON = new FiguraIdentifier("textures/gui/unknown_icon.png");

    public AvatarWidget(int depth, int width, LocalAvatarFetcher.AvatarPath avatar, AvatarList parent) {
        super(depth, width, 24, avatar, parent);

        AvatarWidget instance = this;
        ITextComponent description = Emojis.applyEmojis(new TextComponentString(avatar.getDescription()));
        this.button = new Button(getX(), getY(), width, 24, getName(), null, button -> {
            AvatarManager.loadLocalAvatar(avatar.getPath());
            AvatarList.selectedEntry = avatar.getTheActualPathForThis();
        }) {
            @Override
            public void drawWidget(Minecraft mc, int mouseX, int mouseY, float delta) {
                super.drawWidget(mc, mouseX, mouseY, delta);

                // selected border
                if (instance.isOf(AvatarList.selectedEntry))
                    UIHelper.fillOutline(getX(), getY(), getWidth(), getHeight(), 0xFFFFFFFF);
            }

            @Override
            protected void renderText(Minecraft mc, float delta) {
                // variables
                FontRenderer font = mc.fontRenderer;

                int space = SPACING * depth;
                int width = this.getWidth() - 26 - space;
                int x = getX() + 2 + space;
                int y = getY() + 2;

                // icon
                FileTexture texture = avatar.getIcon();
                ResourceLocation icon = texture == null ? MISSING_ICON : texture.getLocation();
                UIHelper.renderTexture(x, y, 20, 20, icon);

                // name
                ITextComponent parsedName = TextUtils.trimToWidthEllipsis(font, getMessage(), width, TextUtils.ELLIPSIS.createCopy().setStyle(getMessage().getStyle()));
                font.drawStringWithShadow(parsedName.getFormattedText(), x + 22, y, -1);

                // description
                ITextComponent parsedDescription = TextUtils.trimToWidthEllipsis(font, description, width, TextUtils.ELLIPSIS.createCopy().setStyle(description.getStyle()));
                font.drawStringWithShadow(parsedDescription.getFormattedText(), x + 22, y + font.FONT_HEIGHT + 1, ((FontRendererAccessor)font).getColors()[TextFormatting.GRAY.getColorIndex()]);

                // tooltip
                if (parsedName != getMessage() || parsedDescription != description) {
                    ITextComponent tooltip = instance.getName();
                    if (!description.getFormattedText().trim().isEmpty())
                        tooltip = tooltip.createCopy().appendText("\n\n").appendSibling(description);
                    setTooltip(tooltip);
                }
            }

            @Override
            public boolean mouseOver(double mouseX, double mouseY) {
                return parent.isInsideScissors(mouseX, mouseY) && super.mouseOver(mouseX, mouseY);
            }

            @Override
            public void setHovered(boolean hovered) {
                if (!hovered && UIHelper.getContext() == context && context.isVisible())
                    hovered = true;

                super.setHovered(hovered);
            }
        };

        this.button.shouldHaveBackground(false);
        children.add(this.button);
    }
}
