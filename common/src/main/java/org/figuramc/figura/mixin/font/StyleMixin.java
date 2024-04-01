package org.figuramc.figura.mixin.font;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import org.figuramc.figura.ducks.extensions.StyleExtension;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Style.class)
public abstract class StyleMixin implements StyleExtension {

    @Shadow private TextFormatting color;
    @Shadow private Boolean bold;
    @Shadow private Boolean italic;
    @Shadow private Boolean underlined;
    @Shadow private Boolean strikethrough;
    @Shadow private Boolean obfuscated;
    @Shadow private ClickEvent clickEvent;
    @Shadow private HoverEvent hoverEvent;
    @Shadow private String insertion;

    @Shadow public abstract @Nullable TextFormatting getColor();

    @Unique
    private ResourceLocation figura$Font;
    @Unique
    private Integer figura$color;

    @Override
    public Style setFont(ResourceLocation location) {
        figura$Font = location;
        return (Style) (Object)this;
    }

    @Override
    public ResourceLocation getFont() {
        return figura$Font;
    }

    @Override
    public Style setRGBColor(int rgb) {
        this.figura$color = rgb;
        return (Style) (Object)this;
    }

    @Override
    public Integer getRGBColor() {
        if (figura$color == null && this.getColor() != null)
            return ((FontRendererAccessor)Minecraft.getMinecraft().fontRenderer).getColors()[this.getColor().getColorIndex()];
        if (figura$color == null)
            return 0xFFFFF;
        return figura$color;
    }

    @Override
    public Style applyStyleToStyle(Style style) {
        return ((StyleExtension)((StyleExtension)new Style().setColor(this.color != null ? this.color : style.getColor()).setBold(this.bold != null ? this.bold : style.getBold()).setItalic(this.italic != null ? this.italic : style.getItalic()).setUnderlined(this.underlined != null ? this.underlined : style.getUnderlined()).setStrikethrough(this.strikethrough != null ? this.strikethrough : style.getStrikethrough()).setObfuscated(this.obfuscated != null ? this.obfuscated : style.getObfuscated()).setClickEvent(this.clickEvent != null ? this.clickEvent : style.getClickEvent()).setHoverEvent(this.hoverEvent != null ? this.hoverEvent : style.getHoverEvent()).setInsertion(this.insertion != null ? this.insertion : style.getInsertion())).setFont(this.figura$Font != null ? this.figura$Font : ((StyleExtension)style).getFont())).setRGBColor(this.figura$color != null ? this.figura$color : ((StyleExtension)style).getRGBColor());
    }
}