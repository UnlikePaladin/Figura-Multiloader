package org.figuramc.figura.lua.api;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.platform.Window;
import com.mojang.math.Vector3f;
import net.minecraft.SharedConstants;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.Registry;
import net.minecraft.core.SerializableUUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.util.MouseHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.phys.Vec3;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.utils.*;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.lua.LuaNotNil;
import org.figuramc.figura.lua.api.entity.EntityAPI;
import org.figuramc.figura.lua.api.entity.ViewerAPI;
import org.figuramc.figura.lua.docs.FiguraListDocs;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaMethodOverload;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.math.vector.FiguraVec2;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.mixin.gui.GuiIngameAccessor;
import org.figuramc.figura.mixin.gui.PlayerTabOverlayAccessor;
import org.lwjgl.util.vector.Vector3f;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@LuaWhitelist
@LuaTypeDoc(
        name = "ClientAPI",
        value = "client"
)
public class ClientAPI {

    public static final ClientAPI INSTANCE = new ClientAPI();
    private static final HashMap<String, Boolean> LOADED_MODS = new HashMap<>();
    private static final boolean HAS_IRIS = PlatformUtils.isModLoaded("iris") || PlatformUtils.isModLoaded("oculus"); // separated to avoid indexing the list every frame
    public static final Supplier<Boolean> OPTIFINE_LOADED = Suppliers.memoize(() ->
    {
        try
        {
            Class.forName("net.optifine.Config");
            return true;
        }
        catch (ClassNotFoundException ignored)
        {
            return false;
        }
    });
    public static boolean hasOptifineShader() {
        try
        {
            Field shaderPackLoadedField = Class.forName("net.optifine.shaders.Shaders").getField("shaderPackLoaded");
            Class<?> shaderClass = shaderPackLoadedField.getType();
            if (shaderClass == boolean.class)
                return shaderPackLoadedField.getBoolean(null);
        }
        catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException ignored) {}
        return false;
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_fps")
    public static int getFPS() {
        String s = getFPSString();
        if (s.isEmpty())
            return 0;
        return Minecraft.getDebugFPS();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_fps_string")
    public static String getFPSString() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.debug;
    }

    @LuaWhitelist
    @LuaMethodDoc("client.is_paused")
    public static boolean isPaused() {
        return Minecraft.getMinecraft().isGamePaused();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_version")
    public static String getVersion() {
        return Minecraft.getMinecraft().getVersion();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_version_name")
    public static String getVersionName() {
        return Minecraft.getMinecraft().getVersionType();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.is_snapshot")
    public static boolean isSnapshot() {
        return false;
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_client_brand")
    public static String getClientBrand() {
        return ClientBrandRetriever.getClientModName();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_server_brand")
    public static String getServerBrand() {
        if (Minecraft.getMinecraft().player == null)
            return null;

        return Minecraft.getMinecraft().getIntegratedServer() == null ? Minecraft.getMinecraft().player.getServerBrand() : "Integrated";
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_chunk_statistics")
    public static String getChunkStatistics() {
        return Minecraft.getMinecraft().renderGlobal.getDebugInfoRenders();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_entity_statistics")
    public static String getEntityStatistics() {
        return Minecraft.getMinecraft().renderGlobal.getDebugInfoEntities();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_sound_statistics")
    public static String getSoundStatistics() {
        return Minecraft.getMinecraft().getSoundHandler().getSoundManager().getDebugString();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_entity_count")
    public static int getEntityCount() {
        if (Minecraft.getMinecraft().world == null)
            return 0;

        return Minecraft.getMinecraft().world.loadedEntityList.size();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_particle_count")
    public static String getParticleCount() {
        return Minecraft.getMinecraft().effectRenderer.getStatistics();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_current_effect")
    public static String getCurrentEffect() {
        if (Minecraft.getMinecraft().entityRenderer.getShaderGroup() == null)
            return null;

        return Minecraft.getMinecraft().entityRenderer.getShaderGroup().getShaderGroupName();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_java_version")
    public static String getJavaVersion() {
        return System.getProperty("java.version");
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_used_memory")
    public static long getUsedMemory() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_max_memory")
    public static long getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_allocated_memory")
    public static long getAllocatedMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.is_window_focused")
    public static boolean isWindowFocused() {
        return Minecraft.getMinecraft().inGameHasFocus;
    }

    @LuaWhitelist
    @LuaMethodDoc("client.is_hud_enabled")
    public static boolean isHudEnabled() {
        return Minecraft.isGuiEnabled();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.is_debug_overlay_enabled")
    public static boolean isDebugOverlayEnabled() {
        return Minecraft.getMinecraft().gameSettings.showDebugInfo;
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_window_size")
    public static FiguraVec2 getWindowSize() {
        ScaledResolution window = new ScaledResolution(Minecraft.getMinecraft());
        return FiguraVec2.of(Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_fov")
    public static double getFOV() {
        return Minecraft.getMinecraft().gameSettings.fovSetting;
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_system_time")
    public static long getSystemTime() {
        return System.currentTimeMillis();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_mouse_pos")
    public static FiguraVec2 getMousePos() {
        MouseHelper mouse = Minecraft.getMinecraft().mouseHelper;
        return FiguraVec2.of(mouse.xpos(), mouse.ypos());
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_scaled_window_size")
    public static FiguraVec2 getScaledWindowSize() {
        ScaledResolution window = new ScaledResolution(Minecraft.getMinecraft());
        return FiguraVec2.of(window.getScaledWidth(), window.getScaledHeight());
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_gui_scale")
    public static double getGuiScale() {
        return Minecraft.getMinecraft().gameSettings.guiScale;
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_camera_pos")
    public static FiguraVec3 getCameraPos() {
        BlockPos pos = Minecraft.getMinecraft().getRenderViewEntity().getPosition();
        return FiguraVec3.fromBlockPos(pos);
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_camera_rot")
    public static FiguraVec3 getCameraRot() {
        double f = 180d / Math.PI;
        return MathUtils.quaternionToYXZ(Minecraft.getInstance().gameRenderer.getMainCamera().rotation()).multiply(f, -f, f); //degrees, and negate y
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_camera_dir")
    public static FiguraVec3 getCameraDir() {
        return FiguraVec3.fromVec3f(new Vector3f((float) Minecraft.getMinecraft().getRenderViewEntity().getLookVec().x, (float) Minecraft.getMinecraft().getRenderViewEntity().getLookVec().y, (float) Minecraft.getMinecraft().getRenderViewEntity().getLookVec().z));
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "text"
            ),
            value = "client.get_text_width"
    )
    public static int getTextWidth(@LuaNotNil String text) {
        return TextUtils.getWidth(TextUtils.splitText(TextUtils.tryParseJson(text), "\n"), Minecraft.getInstance().font);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "text"
            ),
            value = "client.get_text_height"
    )
    public static int getTextHeight(String text) {
        return TextUtils.getHeight(TextUtils.splitText(TextUtils.tryParseJson(text), "\n"), Minecraft.getInstance().font);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = String.class,
                            argumentNames = "text"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {String.class, Integer.class, Boolean.class},
                            argumentNames = {"text", "maxWidth", "wrap"}
                    )
            },
            value = "client.get_text_dimensions"
    )
    public static FiguraVec2 getTextDimensions(@LuaNotNil String text, int maxWidth, Boolean wrap) {
        ITextComponent component = TextUtils.tryParseJson(text);
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        List<ITextComponent> list = TextUtils.formatInBounds(component, font, maxWidth, wrap == null || wrap);
        int x = TextUtils.getWidth(list, font);
        int y = TextUtils.getHeight(list, font);
        return FiguraVec2.of(x, y);
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_active_lang")
    public static String getActiveLang() {
        return Minecraft.getMinecraft().gameSettings.language;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "modID"
            ),
            value = "client.is_mod_loaded"
    )
    public static boolean isModLoaded(String id) {
        if (Objects.equals(id, "optifine") || Objects.equals(id, "optifabric"))
            return OPTIFINE_LOADED.get();

        LOADED_MODS.putIfAbsent(id, PlatformUtils.isModLoaded(id));
        return LOADED_MODS.get(id);
    }

    @LuaWhitelist
    @LuaMethodDoc("client.has_shader_pack_mod")
    public static boolean hasShaderPackMod() {
        return HAS_IRIS || OPTIFINE_LOADED.get();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.has_shader_pack")
    public static boolean hasShaderPack() {
        return HAS_IRIS && net.irisshaders.iris.api.v0.IrisApi.getInstance().isShaderPackInUse() || OPTIFINE_LOADED.get() && hasOptifineShader();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_shader_pack_name")
    public static String getShaderPackName() {
        try {
            if (HAS_IRIS) {
                return net.coderbot.iris.Iris.getCurrentPackName();
            } else if (OPTIFINE_LOADED.get()) {
                Field shaderNameField = Class.forName("net.optifine.shaders.Shaders").getField("currentShaderName");
                Class<?> shaderClass = shaderNameField.getType();
                if (shaderClass == String.class)
                    return (String) shaderNameField.get(null);
            }
        }catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException ignored) {
        }
        return "";
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "path"
            ),
            value = "client.has_resource"
    )
    public static boolean hasResource(@LuaNotNil String path) {
        ResourceLocation resource = LuaUtils.parsePath(path);
        try {
            return Minecraft.getMinecraft().getResourceManager().getResource(resource) != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_active_resource_packs")
    public static List<String> getActiveResourcePacks() {
        List<String> list = new ArrayList<>();

        for (ResourcePackRepository.Entry pack : Minecraft.getMinecraft().getResourcePackRepository().getRepositoryEntries())
            list.add(pack.getResourcePackName());

        return list;
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_figura_version")
    public static String getFiguraVersion() {
        return FiguraMod.VERSION.toString();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = {String.class, String.class},
                    argumentNames = {"version1", "version2"}
            ),
            value = "client.compare_versions")
    public static int compareVersions(@LuaNotNil String ver1, @LuaNotNil String ver2) {
        Version v1 = new Version(ver1);
        Version v2 = new Version(ver2);

        if (v1.invalid)
            throw new LuaError("Cannot parse version " + "\"" + ver1 + "\"");
        if (v2.invalid)
            throw new LuaError("Cannot parse version " + "\"" + ver2 + "\"");

        return v1.compareTo(v2);
    }

    @LuaWhitelist
    @LuaMethodDoc("client.generate_uuid")
    public static int[] generateUUID(){
        return ResourceUtils.uuidToIntArray(UUID.randomUUID());
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = {Integer.class, Integer.class, Integer.class, Integer.class},
                    argumentNames = {"a", "b", "c", "d"}
            ),
            value = "client.int_uuid_to_string")
    public static String intUUIDToString(int a, int b, int c, int d) {
        try {
            UUID uuid = ResourceUtils.uuidFromIntArray(new int[]{a, b, c, d});
            return uuid.toString();
        } catch (Exception ignored) {
            throw new LuaError("Failed to parse uuid");
        }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "uuid"
            ),
            value = "client.uuid_to_int_array")
    public static int[] uuidToIntArray(String uuid) {
        try {
            UUID id = UUID.fromString(uuid);
            return ResourceUtils.uuidToIntArray(id);
        } catch (Exception ignored) {
            throw new LuaError("Failed to parse uuid");
        }
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_viewer")
    public static ViewerAPI getViewer() {
        return new ViewerAPI(Minecraft.getMinecraft().player);
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_camera_entity")
    public static EntityAPI<?> getCameraEntity() {
        return EntityAPI.wrap(Minecraft.getMinecraft().getRenderViewEntity());
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_server_data")
    public static Map<String, String> getServerData() {
        Map<String, String> map = new HashMap<>();

        IntegratedServer iServer = Minecraft.getMinecraft().getIntegratedServer();
        if (iServer != null) {
            map.put("name", iServer.getWorldName());
            map.put("ip", iServer.getServerHostname());
            map.put("motd", iServer.getMOTD());
            return map;
        }

        ServerData mServer = Minecraft.getMinecraft().getCurrentServerData();
        if (mServer != null) {
            if (mServer.serverName != null)
                map.put("name", mServer.serverName);
            if (mServer.serverIP != null)
                map.put("ip", mServer.serverIP);
            if (mServer.serverMOTD != null)
                map.put("motd", mServer.serverMOTD);
        }

        return map;
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_date")
    public static Map<String, Object> getDate() {
        Map<String, Object> map = new HashMap<>();

        Calendar calendar = FiguraMod.CALENDAR;
        Date date = new Date();
        calendar.setTime(date);

        map.put("day", calendar.get(Calendar.DAY_OF_MONTH));
        map.put("month", calendar.get(Calendar.MONTH) + 1);
        map.put("year", calendar.get(Calendar.YEAR));
        map.put("hour", calendar.get(Calendar.HOUR_OF_DAY));
        map.put("minute", calendar.get(Calendar.MINUTE));
        map.put("second", calendar.get(Calendar.SECOND));
        map.put("millisecond", calendar.get(Calendar.MILLISECOND));
        map.put("week", calendar.get(Calendar.WEEK_OF_YEAR));
        map.put("year_day", calendar.get(Calendar.DAY_OF_YEAR));
        map.put("week_day", calendar.get(Calendar.DAY_OF_WEEK));
        map.put("daylight_saving", calendar.getTimeZone().inDaylightTime(date));

        SimpleDateFormat format = new SimpleDateFormat("Z|zzzz|G|MMMM|EEEE", Locale.US);
        String[] f = format.format(date).split("\\|");

        map.put("timezone", f[0]);
        map.put("timezone_name", f[1]);
        map.put("era", f[2]);
        map.put("month_name", f[3]);
        map.put("day_name", f[4]);

        return map;
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_frame_time")
    public static double getFrameTime() {
        return Minecraft.getMinecraft().getRenderPartialTicks();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_actionbar")
    public static ITextComponent getActionbar() {
        Gui gui = Minecraft.getMinecraft().ingameGUI;
        return ((GuiIngameAccessor) gui).getActionbarTime() > 0 ? new TextComponentString(((GuiIngameAccessor) gui).getActionbar()) : null;
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_title")
    public static ITextComponent getTitle() {
        Gui gui = Minecraft.getMinecraft().ingameGUI;
        return ((GuiIngameAccessor) gui).getTime() > 0 ? new TextComponentString(((GuiIngameAccessor) gui).getTitle()) : null;
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_subtitle")
    public static ITextComponent getSubtitle() {
        Gui gui = Minecraft.getMinecraft().ingameGUI;
        return ((GuiIngameAccessor) gui).getTime() > 0 ? new TextComponentString(((GuiIngameAccessor) gui).getSubtitle()) : null;
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_scoreboard")
    public static Map<String, Map<String, Object>> getScoreboard() {
        Map<String, Map<String, Object>> map = new HashMap<>();

        assert Minecraft.getMinecraft().world != null;
        Scoreboard scoreboard = Minecraft.getMinecraft().world.getScoreboard();

        Map<String, ScoreObjective> objectives = new HashMap<>();

        // sidebars for different team colours
        assert Minecraft.getMinecraft().player != null;
        ScorePlayerTeam playerTeam = scoreboard.getPlayersTeam(Minecraft.getMinecraft().player.getGameProfile().getName());
        if (playerTeam != null) {
            int id = playerTeam.getColor().getColorIndex();
            if (id >= 0) {
                objectives.put("sidebar_team_" + playerTeam.getColor().getFriendlyName(), scoreboard.getObjectiveInDisplaySlot(3 + id));
            }
        }

        objectives.put("list", scoreboard.getObjectiveInDisplaySlot(0));
        objectives.put("sidebar", scoreboard.getObjectiveInDisplaySlot(1));
        objectives.put("below_name", scoreboard.getObjectiveInDisplaySlot(2));

        for (Map.Entry<String, ScoreObjective> entry : objectives.entrySet()) {
            String key = entry.getKey();
            ScoreObjective objective = entry.getValue();

            if (objective != null) {
                Map<String, Object> objectiveMap = new HashMap<>();

                objectiveMap.put("name", objective.getName());
                objectiveMap.put("display_name", objective.getDisplayName());
                objectiveMap.put("criteria", objective.getCriteria().getName());
                objectiveMap.put("render_type", objective.getRenderType().getRenderType());

                Map<String, Integer> scoreMap = new HashMap<>();
                for (Score score : scoreboard.getSortedScores(objective)) {
                    scoreMap.put(score.getPlayerName(), score.getScorePoints());
                }

                objectiveMap.put("scores", scoreMap);

                map.put(key, objectiveMap);
            }
        }

        return map;
    }

    @LuaWhitelist
    @LuaMethodDoc("client.list_atlases")
    public static List<String> listAtlases() {
        return Collections.emptyList();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "path"
            ),
            value = "client.get_atlas"
    )
    public static TextureAtlasAPI getAtlas(@LuaNotNil String atlas) {
        ResourceLocation path = LuaUtils.parsePath(atlas);
        try {
            return new TextureAtlasAPI(Minecraft.getMinecraft().getTextureMapBlocks().getModelManager().getAtlas(path));
        } catch (Exception ignored) {
            return null;
        }
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_tab_list")
    public static Map<String, Object> getTabList() {
        Map<String, Object> map = new HashMap<>();
        PlayerTabOverlayAccessor accessor = (PlayerTabOverlayAccessor) Minecraft.getMinecraft().ingameGUI.getTabList();

        // header
        ITextComponent header = accessor.getHeader();
        if (header != null) {
            map.put("header", header.getFormattedText());
            map.put("headerJson", header);
        }

        // players
        List<String> list = new ArrayList<>();
        for (NetworkPlayerInfo entry : EntityUtils.getTabList())
            list.add(entry.getDisplayName() != null ? entry.getDisplayName().getFormattedText() : entry.getGameProfile().getName());
        map.put("players", list);

        // footer
        ITextComponent footer = accessor.getFooter();
        if (footer != null) {
            map.put("footer", footer.getFormattedText());
            map.put("footerJson", footer);
        }

        return map;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(argumentTypes = String.class, argumentNames = "text"),
                    @LuaMethodOverload(argumentTypes = {String.class, LuaValue.class}, argumentNames = {"text", "args"})
            },
            value = "client.get_translated_string"
    )
    public static String getTranslatedString(@LuaNotNil String text, LuaValue args) {
        ITextComponent component;

        if (args == null) {
            component = new TextComponentTranslation(text);
        } else if (!args.istable()) {
            component = new TextComponentTranslation(text, args.tojstring());
        } else {
            int len = args.length();
            Object[] arguments = new Object[len];

            for (int i = 0; i < len; i++)
                arguments[i] = args.get(i + 1).tojstring();

            component = new TextComponentTranslation(text, arguments);
        }

        return component.getFormattedText();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(argumentTypes = String.class, argumentNames = "registryName"),
            },
            value = "client.get_registry"
    )
    public static List<String> getRegistry(@LuaNotNil String registryName) {
        Registry<?> registry = Registry.REGISTRY.get(new ResourceLocation(registryName));

        if (registry != null) {
            return registry.keySet().stream()
                    .map(ResourceLocation::toString)
                    .collect(Collectors.toList());
        } else {
            throw new LuaError("Registry " + registryName + " does not exist");
        }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(argumentTypes = String.class, argumentNames = "enumName"),
            },
            value = "client.getEnum"
    )
    public static List<String> getEnum(@LuaNotNil String enumName) {
        try {
            return FiguraListDocs.getEnumValues(enumName);
        } catch (Exception e) {
            throw new LuaError("Enum " + enumName + " does not exist");
        }
    }

    @Override
    public String toString() {
        return "ClientAPI";
    }
}
