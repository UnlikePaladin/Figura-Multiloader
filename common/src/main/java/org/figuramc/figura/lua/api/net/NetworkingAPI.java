package org.figuramc.figura.lua.api.net;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.docs.LuaFieldDoc;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaMethodOverload;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.permissions.Permissions;
import org.figuramc.figura.utils.ColorUtils;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

@LuaWhitelist
@LuaTypeDoc(
        name = "NetworkingAPI",
        value = "net"
)
public class NetworkingAPI {
    private static FileOutputStream logFileOutputStream;
    private static final String NETWORKING_DISABLED_ERROR_TEXT = "Networking is disabled in config";
    private static final String NO_PERMISSION_ERROR_TEXT = "This avatar doesn't have networking permissions";
    private static final String NETWORKING_DISALLOWED_FOR_LINK_ERROR = "Networking is not for link %s";
    final Avatar owner;
    @LuaWhitelist
    @LuaFieldDoc("net.http")
    public final HttpRequestsAPI http;
    @LuaWhitelist
    @LuaFieldDoc("net.socket")
    public final SocketAPI socket;

    public NetworkingAPI(Avatar owner) {
        this.owner = owner;
        http = new HttpRequestsAPI(this);
        socket = new SocketAPI(this);
    }

    public void securityCheck(String link) throws RuntimeException {
        if (!Configs.ALLOW_NETWORKING.value)
            throw new LuaError(NETWORKING_DISABLED_ERROR_TEXT);
        if (owner.permissions.get(Permissions.NETWORKING) < 1) {
            owner.noPermissions.add(Permissions.NETWORKING);
            throw new LuaError(NO_PERMISSION_ERROR_TEXT);
        }
        if (!isLinkAllowed(link)) {
            throw new LinkNotAllowedException(String.format(NETWORKING_DISALLOWED_FOR_LINK_ERROR, link));
        }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "net.is_networking_allowed",
            overloads = @LuaMethodOverload(
                    returnType = Boolean.class
            )
    )
    public boolean isNetworkingAllowed() {
        return Configs.ALLOW_NETWORKING.value && owner.permissions.get(Permissions.NETWORKING) >= 1;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "net.is_link_allowed",
            overloads = @LuaMethodOverload(
                    argumentNames = "link",
                    argumentTypes = String.class,
                    returnType = Boolean.class
            )
    )
    public boolean isLinkAllowed(String link) {
        RestrictionLevel level = RestrictionLevel.getById(Configs.NETWORKING_RESTRICTION.value);
        if (level == null) return false;
        ArrayList<Filter> filters = Configs.NETWORK_FILTER.getFilters();
        switch (level) {
            case WHITELIST:
                return filters.stream().anyMatch(f -> f.matches(link));
            case BLACKLIST:
                return filters.stream().noneMatch(f -> f.matches(link));
            case NONE:
                return true;
            default:
                throw new IllegalArgumentException();
        }
    }

    void log(LogSource source, ITextComponent text) {
        // 0 - FILE, 1 - FILE + LOGGER, 2 - FILE + LOGGER + CHAT, 3 - NONE
        int log = Configs.LOG_NETWORKING.value;
        if (log == 3) return;
        ITextComponent finalText =
                new TextComponentString(String.format("[networking:%s:%s] ", source.name().toLowerCase(),owner.entityName))
                        .setStyle(ColorUtils.Colors.LUA_PING.style)
                        .appendSibling(text.createCopy().setStyle(new Style().setColor(TextFormatting.WHITE)));
        String logTextString = finalText.getUnformattedText();
        switch (log) {
            case 2:
                FiguraMod.sendChatMessage(finalText);
                break;
            case 1:
                FiguraMod.LOGGER.info(logTextString);
                break;
        }
        if (logFileOutputStream == null) prepareLogStream();
        try {
            LocalTime t = LocalTime.now();
            writeToLogStream(String.format("[%02d:%02d:%02d] [INFO] %s\n", t.getHour(), t.getMinute(),
                    t.getSecond(), finalText.getUnformattedText()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void error(LogSource source, ITextComponent text) {
        // 0 - FILE, 1 - FILE + LOGGER, 2 - FILE + LOGGER + CHAT, 3 - NONE
        int log = Configs.LOG_NETWORKING.value;
        if (log == 3) return;
        ITextComponent finalText =
                new TextComponentString(String.format("[networking:%s:%s] ", source.name().toLowerCase(),owner.entityName))
                        .setStyle(ColorUtils.Colors.LUA_ERROR.style)
                        .appendSibling(text.createCopy().setStyle(new Style().setColor(TextFormatting.WHITE)));
        String logTextString = finalText.getUnformattedText();
        switch (log) {
            case 2:
                FiguraMod.sendChatMessage(finalText);
                break;
            case 1:
                FiguraMod.LOGGER.error(logTextString);
                break;
        }
        if (logFileOutputStream == null) prepareLogStream();
        try {
            LocalTime t = LocalTime.now();
            writeToLogStream(String.format("[%02d:%02d:%02d] [ERROR] %s\n", t.getHour(), t.getMinute(),
                    t.getSecond(), finalText.getUnformattedText()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeToLogStream(String s) throws IOException {
        logFileOutputStream.write(s.getBytes(StandardCharsets.UTF_8));
        logFileOutputStream.flush();
    }

    private static void prepareLogStream() {
        try {
            Path p = FiguraMod.getFiguraDirectory().resolve("logs");
            File folder = p.toFile();
            folder.mkdirs();
            LocalDate d = LocalDate.now();
            File logFile = p.resolve(
                    String.format("%d-%02d-%02d.log", d.getYear(), d.getMonthValue(), d.getDayOfMonth())
            ).toFile();
            logFileOutputStream = new FileOutputStream(logFile, true);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Filter {

        private String filterSource;
        private FilterMode filterMode;

        public Filter(String source, FilterMode mode) {
            setSource(source.trim());
            setMode(mode);
        }

        public String getSource() {
            return filterSource;
        }

        public void setSource(String filterSource) {
            this.filterSource = filterSource;
        }

        public FilterMode getMode() {
            return filterMode;
        }

        public void setMode(FilterMode filterMode) {
            this.filterMode = filterMode;
        }

        public boolean matches(String s) {
            switch (filterMode) {
                case EQUALS:
                    return s.trim().equals(filterSource);
                case CONTAINS:
                    return s.trim().contains(filterSource);
                case STARTS_WITH:
                    return s.trim().startsWith(filterSource);
                case ENDS_WITH:
                    return s.trim().endsWith(filterSource);
                case REGEX:
                    return s.trim().matches(filterSource);
                default:
                    throw new IllegalArgumentException();
            }
        }

        public enum FilterMode {
            EQUALS(0),
            CONTAINS(1),
            STARTS_WITH(2),
            ENDS_WITH(3),
            REGEX(4);
            private final int id;
            FilterMode(int id) {
                this.id = id;
            }

            public int getId() {
                return id;
            }

            public static FilterMode getById(int id) {
                for (FilterMode t :
                        FilterMode.values()) {
                    if (t.id == id) return t;
                }
                return null;
            }
        }
    }

    public enum RestrictionLevel {
        WHITELIST(0),
        BLACKLIST(1),
        NONE(2);
        private final int id;
        RestrictionLevel(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static RestrictionLevel getById(int id) {
            for (RestrictionLevel t :
                    RestrictionLevel.values()) {
                if (t.id == id) return t;
            }
            return null;
        }
    }

    enum LogSource {
        HTTP, SOCKET
    }

    @LuaWhitelist
    public Object __index(LuaValue key) {
        if (!key.isstring()) return null;
        switch (key.tojstring()) {
            case "http":
                return http;
            case "socket":
                return socket;
            default:
                return null;
        }
    }

    static class LinkNotAllowedException extends RuntimeException {
        public final LuaError luaError;
        public LinkNotAllowedException(String message) {
            luaError = new LuaError(message);
        }

        @Override
        public String toString() {
            return "LinkNotAllowedException";
        }
    }

    @Override
    public String toString() {
        return "NetworkingAPI";
    }
}