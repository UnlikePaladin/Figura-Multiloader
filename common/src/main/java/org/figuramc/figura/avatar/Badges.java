package org.figuramc.figura.avatar;

import net.minecraft.util.text.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.event.HoverEvent;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.ducks.extensions.StyleExtension;
import org.figuramc.figura.lua.api.sound.SoundAPI;
import org.figuramc.figura.permissions.PermissionManager;
import org.figuramc.figura.permissions.Permissions;
import org.figuramc.figura.utils.*;
import org.figuramc.figura.utils.ui.UIHelper;

import java.util.BitSet;
import java.util.Optional;
import java.util.UUID;

public class Badges {

    public static final ResourceLocation FONT = new FiguraIdentifier("badges");

    public static ITextComponent fetchBadges(UUID id) {
        if (PermissionManager.get(id).getCategory() == Permissions.Category.BLOCKED)
            return new TextComponentString("");

        // get user data
        Pair<BitSet, BitSet> pair = AvatarManager.getBadges(id);
        if (pair == null)
            return new TextComponentString("");
        Style style = ((StyleExtension)new Style().setColor(TextFormatting.WHITE).setObfuscated(false)).setFont(FONT);

        ITextComponent badges = new TextComponentString("").setStyle(style);

        // avatar badges
        Avatar avatar = AvatarManager.getAvatarForPlayer(id);
        if (avatar != null) {

            // -- loading -- //

            if (!avatar.loaded)
                badges.appendSibling(new TextComponentString(Integer.toHexString(Math.abs(FiguraMod.ticks) % 16)));

            // -- mark -- // 

            else if (avatar.nbt != null) {
                // mark
                mark: {
                    // pride (mark skins)
                    BitSet prideSet = pair.first();
                    Pride[] pride = Pride.values();
                    for (int i = pride.length - 1; i >= 0; i--) {
                        if (prideSet.get(i)) {
                            badges.appendSibling(pride[i].badge);
                            break mark;
                        }
                    }

                    // mark fallback
                    badges.appendSibling(System.DEFAULT.badge.createCopy().setStyle(((StyleExtension)new Style()).setRGBColor(ColorUtils.rgbToInt(ColorUtils.userInputHex(avatar.color)))));
                }

                // error
                if (avatar.scriptError) {
                    if (avatar.errorText == null)
                        badges.appendSibling(System.ERROR.badge);
                    else
                        badges.appendSibling(System.ERROR.badge.createCopy().setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, System.ERROR.desc.createCopy().appendText("\n\n").appendSibling(avatar.errorText)))));
                }

                // version
                if (avatar.versionStatus > 0)
                    badges.appendSibling(System.WARNING.badge);

                // permissions
                if (!avatar.noPermissions.isEmpty()) {
                    ITextComponent badge = System.PERMISSIONS.badge.createCopy();
                    ITextComponent desc = System.PERMISSIONS.desc.createCopy().appendText("\n");
                    for (Permissions t : avatar.noPermissions)
                        desc.appendText("\n• ").appendSibling(new FiguraText("badges.no_permissions." + t.name.toLowerCase()));

                    badges.appendSibling(badge.setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, desc))));
                }
            }
        }

        // -- special -- //
        if (avatar != null) {
            // special badges
            BitSet specialSet = pair.second();
            Special[] specialValues = Special.values();
            for (int i = specialValues.length - 1; i >= 0; i--) {
                if (specialSet.get(i)) {
                    Special special = specialValues[i];
                    Integer color = special.color;
                    if (avatar.badgeToColor.containsKey(special.name().toLowerCase())) {
                        color = ColorUtils.rgbToInt(ColorUtils.userInputHex(avatar.badgeToColor.get(special.name().toLowerCase())));
                    }
                    ITextComponent badge = color != null ? special.badge.createCopy().setStyle(((StyleExtension)new Style()).setRGBColor(color)) : special.badge;
                    badges.appendSibling(badge);
                }
            }
        }

        // -- extra -- // 


        // sound
        if (avatar != null && Configs.SOUND_BADGE.value) {
            if (avatar.lastPlayingSound > 0) {
                badges.appendSibling(System.SOUND.badge);
            } else if (SoundAPI.getSoundEngine().figura$isPlaying(id)) {
                avatar.lastPlayingSound = 20;
                badges.appendSibling(System.SOUND.badge);
            }
        }


        // -- return -- // 
        return badges;
    }

    public static ITextComponent noBadges4U(ITextComponent text) {
        return TextUtils.replaceInText(text, "[-*/+=❗❌\uD83D\uDEE1★☆❤文✒\uD83D\uDDFF0-9a-f]", TextUtils.UNKNOWN, (s, style) -> ((StyleExtension)style).getFont().equals(FONT) || ((StyleExtension)style).getFont().equals(UIHelper.UI_FONT), Integer.MAX_VALUE);
    }

    public static Pair<BitSet, BitSet> emptyBadges() {
        return Pair.of(new BitSet(Pride.values().length), new BitSet(Special.values().length));
    }

    public static boolean hasCustomBadges(ITextComponent text) {
        return text.getFormattedText().contains("${badges}") || text.getFormattedText().contains("${segdab}");
    }

    public static ITextComponent appendBadges(ITextComponent text, UUID id, boolean allow) {
        ITextComponent badges = allow ? fetchBadges(id) : new TextComponentString("");
        boolean custom = hasCustomBadges(text);

        // no custom badges text
        if (!custom)
            return badges.getFormattedText().trim().isEmpty() ? text : text.createCopy().appendText(" ").appendSibling(badges);

        text = TextUtils.replaceInText(text, "\\$\\{badges\\}(?s)", badges);
        text = TextUtils.replaceInText(text, "\\$\\{segdab\\}(?s)", TextUtils.reverse(badges));

        return text;
    }

    public enum System {
        DEFAULT("△"),
        PERMISSIONS("\uD83D\uDEE1"),
        WARNING("❗"),
        ERROR("❌"),
        SOUND("\uD83D\uDD0A");

        public final ITextComponent badge;
        public final ITextComponent desc;

        System(String unicode) {
            this.desc = new FiguraText("badges.system." + this.name().toLowerCase());
            this.badge = new TextComponentString(unicode).setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, desc)));
        }
    }

    public enum Pride {
        AGENDER("ᚠ"),
        AROACE("ᚡ"),
        AROMANTIC("ᚢ"),
        ASEXUAL("ᚣ"),
        BIGENDER("ᚤ"),
        BISEXUAL("ᚥ"),
        DEMIBOY("ᚦ"),
        DEMIGENDER("ᚧ"),
        DEMIGIRL("ᚨ"),
        DEMIROMANTIC("ᚩ"),
        DEMISEXUAL("ᚪ"),
        DISABILITY("ᚫ"),
        FINSEXUAL("ᚬ"),
        GAYMEN("ᚭ"),
        GENDERFAE("ᚮ"),
        GENDERFLUID("ᚯ"),
        GENDERQUEER("ᚰ"),
        INTERSEX("ᚱ"),
        LESBIAN("ᚲ"),
        NONBINARY("ᚳ"),
        PANSEXUAL("ᚴ"),
        PLURAL("ᚵ"),
        POLYSEXUAL("ᚶ"),
        PRIDE("ᚷ"),
        TRANSGENDER("ᚸ");

        public final ITextComponent badge;
        public final ITextComponent desc;

        Pride(String unicode) {
            this.desc = new FiguraText("badges.pride." + this.name().toLowerCase());
            this.badge = new TextComponentString(unicode).setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, desc)));
        }
    }

    public enum Special {
        DEV("★"),
        DISCORD_STAFF("☆", ColorUtils.Colors.DISCORD.hex),
        CONTEST("☆", ColorUtils.Colors.AWESOME_BLUE.hex),
        DONATOR("❤", ColorUtils.Colors.AWESOME_BLUE.hex),
        TRANSLATOR("文"),
        TEXTURE_ARTIST("✒"),
        IMMORTALIZED("\uD83D\uDDFF");

        public final ITextComponent badge;
        public final ITextComponent desc;
        public final Integer color;

        Special(String unicode) {
            this(unicode, null);
        }

        Special(String unicode, Integer color) {
            this.desc = new FiguraText("badges.special." + this.name().toLowerCase());
            Style style = new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, desc));
            if (color != null) style = ((StyleExtension)style).setRGBColor(color);
            this.color = color;
            this.badge = new TextComponentString(unicode).setStyle(style);
        }
    }
}
