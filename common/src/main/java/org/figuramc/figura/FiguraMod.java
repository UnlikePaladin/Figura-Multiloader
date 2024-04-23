package org.figuramc.figura;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.local.CacheAvatarLoader;
import org.figuramc.figura.avatar.local.LocalAvatarFetcher;
import org.figuramc.figura.avatar.local.LocalAvatarLoader;
import org.figuramc.figura.backend2.NetworkStuff;
import org.figuramc.figura.compat.GeckoLibCompat;
import org.figuramc.figura.compat.SimpleVCCompat;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.ducks.extensions.StyleExtension;
import org.figuramc.figura.entries.EntryPointManager;
import org.figuramc.figura.font.Emojis;
import org.figuramc.figura.lua.FiguraLuaPrinter;
import org.figuramc.figura.lua.docs.FiguraDocsManager;
import org.figuramc.figura.mixin.TileEntitySkullAccessor;
import org.figuramc.figura.permissions.PermissionManager;
import org.figuramc.figura.platform.Services;
import org.figuramc.figura.resources.FiguraRuntimeResources;
import org.figuramc.figura.utils.*;
import org.figuramc.figura.wizards.AvatarWizard;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class FiguraMod {

    public static final String MOD_ID = "figura";
    public static final String MOD_NAME = "Figura";
    public static final FiguraModMetadata METADATA = Services.FIGURA_MOD_METADATA.getMetadataForMod(MOD_ID);
    public static final Version VERSION = new Version(PlatformUtils.getFiguraModVersionString());
    public static final Calendar CALENDAR = Calendar.getInstance();
    public static final Path GAME_DIR = PlatformUtils.getGameDir().normalize();
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);
    public static final float VERTEX_OFFSET = -0.0005f;

    public static int ticks;
    public static Entity extendedPickEntity;
    public static ITextComponent splashText;
    public static boolean parseMessages = true;
    public static boolean processingKeybind;

    /* For some reason, the mod menu entrypoint (or something) is able to call this before the Config
    class can initialize, meaning Configs.DEBUG_MODE can be null when this is called.... Weird */
    @SuppressWarnings("all")
    public static boolean debugModeEnabled() {
        return Configs.DEBUG_MODE != null && Configs.DEBUG_MODE.value;
    }

    public static void onClientInit() {
        // init managers
        Services.ENTRYPOINT_MANAGER.init();
        PermissionManager.init();
        LocalAvatarFetcher.init();
        CacheAvatarLoader.init();
        FiguraDocsManager.init();
        FiguraRuntimeResources.init();

        GeckoLibCompat.init();
        SimpleVCCompat.init();
    }

    public static List<FiguraResourceListener> getResourceListeners() {
        List<FiguraResourceListener> listeners = new ArrayList<>();
        listeners.add(LocalAvatarLoader.AVATAR_LISTENER);
        listeners.add(Emojis.RESOURCE_LISTENER);
        listeners.add(AvatarWizard.RESOURCE_LISTENER);
        listeners.add(AvatarManager.RESOURCE_RELOAD_EVENT);
        return listeners;
    }


    public static void tick() {
        pushProfiler("network");
        NetworkStuff.tick();
        popPushProfiler("files");
        LocalAvatarLoader.tick();
        LocalAvatarFetcher.tick();
        popPushProfiler("avatars");
        AvatarManager.tickLoadedAvatars();
        popPushProfiler("chatPrint");
        FiguraLuaPrinter.printChatFromQueue();
        popPushProfiler("emojiAnim");
        Emojis.tickAnimations();
        popProfiler();
        ticks++;
    }

    // -- Helper Functions -- //

    // debug print
    public static void debug(String str, Object... args) {
        if (FiguraMod.debugModeEnabled()) LOGGER.info("[DEBUG] " + str, args);
        else LOGGER.debug(str, args);
    }

    // mod root directory
    public static Path getFiguraDirectory() {
        String config = Configs.MAIN_DIR.value;
        Path p = config.trim().isEmpty() ? GAME_DIR.resolve(MOD_ID) : Paths.get(config);
        return IOUtils.createDirIfNeeded(p);
    }

    // mod cache directory
    public static Path getCacheDirectory() {
        return IOUtils.getOrCreateDir(getFiguraDirectory(), Configs.LOCAL_ASSETS.value ? "local_cache" : "cache");
    }

    // get local player uuid
    public static UUID getLocalPlayerUUID() {
        Entity player = Minecraft.getMinecraft().player;
        return player != null ? player.getUniqueID() : Minecraft.getMinecraft().getSession().getProfile().getId();
    }

    public static boolean isLocal(UUID other) {
        return getLocalPlayerUUID().equals(other);
    }

    /**
     * Sends a chat message right away. Use when you know your message is safe.
     * If your message is unsafe, (generated by a user), use luaSendChatMessage instead.
     *
     * @param message - text to send
     */
    public static void sendChatMessage(ITextComponent message) {
        if (Minecraft.getMinecraft().ingameGUI != null) {
            parseMessages = false;
            Minecraft.getMinecraft().ingameGUI.getChatGUI().addToSentMessages(TextUtils.replaceTabs(message).getFormattedText());
            parseMessages = true;
        } else {
            LOGGER.info(message.getFormattedText());
        }
    }

    /**
     * Converts a player name to UUID using minecraft internal functions.
     *
     * @param playerName - the player name
     * @return - the player's uuid or null
     */
    public static UUID playerNameToUUID(String playerName) {
        PlayerProfileCache cache = TileEntitySkullAccessor.getProfileCache();
        if (cache == null) return null;

        GameProfile profile = cache.getGameProfileForUsername(playerName);
        return profile == null ? null : profile.getId();
    }

    public static Style getAccentColor() {
        Avatar avatar = AvatarManager.getAvatarForPlayer(getLocalPlayerUUID());
        int color = avatar != null ? ColorUtils.rgbToInt(ColorUtils.userInputHex(avatar.color, ColorUtils.Colors.AWESOME_BLUE.vec)) : ColorUtils.Colors.AWESOME_BLUE.hex;
        return ((StyleExtension)new Style()).setRGBColor(color);
    }

    // -- profiler -- //

    public static void pushProfiler(String name) {
        Minecraft.getMinecraft().mcProfiler.startSection(name);
    }

    public static void pushProfiler(Avatar avatar) {
        Minecraft.getMinecraft().mcProfiler.startSection(avatar.entityName.trim().isEmpty() ? avatar.owner.toString() : avatar.entityName);
    }

    public static void popPushProfiler(String name) {
        Minecraft.getMinecraft().mcProfiler.endStartSection(name);
    }

    public static void popProfiler() {
        Minecraft.getMinecraft().mcProfiler.endSection();
    }

    public static <T> T popReturnProfiler(T var) {
        Minecraft.getMinecraft().mcProfiler.endSection();
        return var;
    }

    public static void popProfiler(int times) {
        Profiler profiler = Minecraft.getMinecraft().mcProfiler;
        for (int i = 0; i < times; i++)
            profiler.endSection();
    }

    public enum Links {
        Wiki("https://wiki.figuramc.org/", ColorUtils.Colors.AWESOME_BLUE.style),
        Kofi("https://ko-fi.com/skyrina", ColorUtils.Colors.KOFI.style),
        OpenCollective("https://opencollective.com/figura", ColorUtils.Colors.KOFI.style),
        Discord("https://discord.figuramc.org/", ColorUtils.Colors.DISCORD.style),
        Github("https://github.com/FiguraMC/Figura", ColorUtils.Colors.GITHUB.style),
        Modrinth("https://modrinth.com/mod/figura", ColorUtils.Colors.MODRINTH.style),
        Curseforge("https://www.curseforge.com/minecraft/mc-mods/figura", ColorUtils.Colors.CURSEFORGE.style),
        LuaManual("https://www.lua.org/manual/5.2/manual.html", ColorUtils.Colors.LUA_LOG.style);

        public final String url;
        public final Style style;

        Links(String url, Style style) {
            this.url = url;
            this.style = style;
        }
    }
}
