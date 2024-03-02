package org.figuramc.figura.mixin.gui;

import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.Badges;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.font.Emojis;
import org.figuramc.figura.lua.api.nameplate.NameplateCustomization;
import org.figuramc.figura.permissions.Permissions;
import org.figuramc.figura.utils.EntityUtils;
import org.figuramc.figura.utils.Pair;
import org.figuramc.figura.utils.TextUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Mixin(GuiNewChat.class)
public class GuiNewChatMixin {

    @Unique private Integer color;
    @Unique private int currColor;

    @ModifyVariable(at = @At("HEAD"), method = "setChatLine", ordinal = 0, argsOnly = true)
    private ITextComponent addMessage(ITextComponent message, ITextComponent msg, int k, int timestamp, boolean refresh) {
        // do not change the message on refresh
        if (refresh) return message;

        color = null;
        if (AvatarManager.panic)
            return message;

        // receive event
        Avatar localPlayer = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());
        if (localPlayer != null) {
            String json = ITextComponent.Serializer.componentToJson(message);

            Pair<String, Integer> event = localPlayer.chatReceivedMessageEvent(message.getUnformattedText(), json);
            if (event != null) {
                String newMessage = event.getFirst();
                if (newMessage == null)
                    return null;
                if (!json.equals(newMessage)) {
                    TextUtils.allowScriptEvents = true;
                    message = TextUtils.tryParseJson(newMessage);
                    TextUtils.allowScriptEvents = false;
                }
                color = event.getSecond();
            }
        }

        // stop here if we should not parse messages
        if (!FiguraMod.parseMessages)
            return message;

        // emojis
        if (Configs.EMOJIS.value > 0)
            message = Emojis.applyEmojis(message);

        // nameplates
        int config = Configs.CHAT_NAMEPLATE.value;
        if (config == 0)
            return message;

        message = TextUtils.parseLegacyFormatting(message);

        Map<String, UUID> players = EntityUtils.getPlayerList();
        String owner = null;

        String msgString = message.getUnformattedText();
        String[] split = msgString.split("\\W+");
        for (String s : split) {
            if (players.containsKey(s)) {
                owner = s;
                break;
            }
        }

        // iterate over ALL online players
        for (Map.Entry<String, UUID> entry : players.entrySet()) {
            String name = entry.getKey();

            if (!msgString.toLowerCase().contains(name.toLowerCase())) // player is not here
                continue;

            UUID uuid = entry.getValue();
            boolean isOwner = name.equals(owner);

            ITextComponent playerName = new TextComponentString(name);

            // apply customization
            Avatar avatar = AvatarManager.getAvatarForPlayer(uuid);
            NameplateCustomization custom = avatar == null || avatar.luaRuntime == null ? null : avatar.luaRuntime.nameplate.CHAT;

            if (custom == null && config < 2) // no customization and no possible badges to append
                continue;

            ITextComponent replacement = custom != null && custom.getJson() != null && avatar.permissions.get(Permissions.NAMEPLATE_EDIT) == 1 ?
                    TextUtils.replaceInText(custom.getJson().createCopy(), "\n|\\\\n", " ") : playerName;

            // name
            replacement = TextUtils.replaceInText(replacement, "\\$\\{name\\}", playerName);

            // badges
            ITextComponent emptyReplacement = Badges.appendBadges(replacement, uuid, config > 1 && owner == null);

            // trim
            emptyReplacement = TextUtils.trim(emptyReplacement);

            // modify message
            String quotedName = "(?i)\\b" + Pattern.quote(name) + "\\b";
            message = TextUtils.replaceInText(message, quotedName, emptyReplacement, (s, style) -> true, isOwner ? 1 : 0, Integer.MAX_VALUE);

            // sender badges
            if (config > 1 && isOwner) {
                // badges
                ITextComponent temp = Badges.appendBadges(replacement, uuid, true);
                // trim
                temp = TextUtils.trim(temp);
                // modify message, only first
                message = TextUtils.replaceInText(message, quotedName, temp, (s, style) -> true, 1);
            }
        }

        return message;
    }
}