package org.figuramc.figura.avatar.local;

import net.minecraft.util.Util;
import net.minecraft.nbt.*;
import net.minecraft.util.ResourceLocation;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.UserData;
import org.figuramc.figura.gui.FiguraToast;
import org.figuramc.figura.parsers.AvatarMetadataParser;
import org.figuramc.figura.parsers.BlockbenchModelParser;
import org.figuramc.figura.parsers.LuaScriptParser;
import org.figuramc.figura.platform.Services;
import org.figuramc.figura.utils.FiguraResourceListener;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.IOUtils;
import org.figuramc.figura.utils.NbtType;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 * class used to load avatars from a file
 * and used for hot-swapping
 */
public class LocalAvatarLoader {

    public static final boolean IS_WINDOWS = Util.getOSType() == Util.EnumOS.WINDOWS;
    private static final HashMap<Path, WatchKey> KEYS = new HashMap<>();

    private static CompletableFuture<Void> tasks;
    private static Path lastLoadedPath;
    private static LoadState loadState = LoadState.UNKNOWN;
    private static String loadError;

    private static WatchService watcher;

    public static final HashMap<ResourceLocation, NBTTagCompound> CEM_AVATARS = new HashMap<>();
    public static final FiguraResourceListener AVATAR_LISTENER = Services.FIGURA_RESOURCE_LISTENER.createResourceListener("cem", manager -> {
        CEM_AVATARS.clear();
        AvatarManager.clearCEMAvatars();
        for (String space : manager.getResourceDomains()) {
            try {
                manager.getAllResources(new ResourceLocation(space,"cem")).forEach(location -> {
                ResourceLocation resource = location.getResourceLocation();
                if (resource.getResourcePath().endsWith(".moon")) {
                    // id
                    String[] split = resource.getResourcePath().split("/");
                    if (!(split.length <= 1)) {

                        String namespace = split[split.length - 2];
                        String path = split[split.length - 1];
                        ResourceLocation id = new ResourceLocation(namespace, path.substring(0, path.length() - 5));

                        // nbt
                        NBTTagCompound nbt;
                        try {
                            nbt = CompressedStreamTools.readCompressed(manager.getResource(resource).getInputStream());

                            // insert
                            FiguraMod.LOGGER.info("Loaded CEM model for " + id);
                            CEM_AVATARS.put(id, nbt);
                        } catch (Exception e) {
                            FiguraMod.LOGGER.error("Failed to load " + id + " avatar", e);
                        }
                    }
                }
            });
            } catch (IOException e) {
                FiguraMod.LOGGER.error("Failed to load in CEM avatars from {}, stacktrace {}", space, e.getMessage());
                continue;
            }
            }
        });

    static {
        try {
            watcher = FileSystems.getDefault().newWatchService();
        } catch (Exception e) {
            FiguraMod.LOGGER.error("Failed to initialize the watcher service", e);
        }
    }

    protected static void async(Runnable toRun) {
        if (tasks == null || tasks.isDone()) {
            tasks = CompletableFuture.runAsync(toRun);
        } else {
            tasks.thenRun(toRun);
        }
    }

    /**
     * Loads an NbtCompound from the specified path
     *
     * @param path - the file/folder for loading the avatar
     */
    public static void loadAvatar(Path path, UserData target) {
        loadError = null;
        loadState = LoadState.UNKNOWN;
        resetWatchKeys();
        try {
            path = path == null ? null : path.getFileSystem() == FileSystems.getDefault() ? Paths.get(path.toFile().getCanonicalPath()) : path.normalize();
        } catch (IOException e) {
        }
        lastLoadedPath = path;

        if (path == null || target == null)
            return;

        addWatchKey(path, KEYS::put);

        Path finalPath = path;
        async(() -> {
            try {
                // load as folder
                NBTTagCompound nbt = new NBTTagCompound();

                // scripts
                loadState = LoadState.SCRIPTS;
                loadScripts(finalPath, nbt);

                // custom sounds
                loadState = LoadState.SOUNDS;
                loadSounds(finalPath, nbt);

                // models
                NBTTagCompound textures = new NBTTagCompound();
                NBTTagList animations = new NBTTagList();
                BlockbenchModelParser modelParser = new BlockbenchModelParser();

                loadState = LoadState.MODELS;
                NBTTagCompound models = loadModels(finalPath, finalPath, modelParser, textures, animations, "");
                models.setString("name", "models");

                // metadata
                loadState = LoadState.METADATA;
                String metadata = IOUtils.readFile(finalPath.resolve("avatar.json"));
                nbt.setTag("metadata", AvatarMetadataParser.parse(metadata, IOUtils.getFileNameOrEmpty(finalPath)));
                AvatarMetadataParser.injectToModels(metadata, models);
                AvatarMetadataParser.injectToTextures(metadata, textures);

                // return :3
                if (!models.hasNoTags())
                    nbt.setTag("models", models);
                if (!textures.hasNoTags())
                    nbt.setTag("textures", textures);
                if (!animations.hasNoTags())
                    nbt.setTag("animations", animations);
                NBTTagCompound metadataTag = nbt.getCompoundTag("metadata");
                if (metadataTag.hasKey("resources_paths")) {
                    loadResources(nbt, metadataTag.getTagList("resources_paths", NbtType.STRING.getValue()), finalPath);
                    metadataTag.removeTag("resource_paths");
                }

                // load
                target.loadAvatar(nbt);
            } catch (Throwable e) {
                loadError = e.getMessage();
                FiguraMod.LOGGER.error("Failed to load avatar from " + finalPath, e);
                FiguraToast.sendToast(new FiguraText("toast.load_error"), new FiguraText("gui.load_error." + LocalAvatarLoader.getLoadState()), FiguraToast.ToastType.ERROR);
            }
        });
    }
    private static void loadResources(NBTTagCompound nbt, NBTTagList pathsTag, Path parentPath) {
        ArrayList<PathMatcher> pathMatchers = new ArrayList<>();
        FileSystem fs = FileSystems.getDefault();
        for (int i = 0; i < pathsTag.tagCount(); i++) {
            pathMatchers.add(fs.getPathMatcher("glob:".concat(pathsTag.getStringTagAt(i))));
        }
        Map<String, Path> pathMap = new HashMap<>();
        matchPathsRecursive(pathMap, parentPath, parentPath, pathMatchers);
        NBTTagCompound resourcesTag = new NBTTagCompound();
        for (String p:
                pathMap.keySet()) {
            try (FileInputStream fis = new FileInputStream(pathMap.get(p).toFile())) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                GZIPOutputStream gos = new GZIPOutputStream(baos);
                int i;
                while ((i = fis.read()) != -1) {
                    gos.write(i);
                }
                gos.close();
                resourcesTag.setTag(unixifyPath(p), new NBTTagByteArray(baos.toByteArray()));
                baos.close();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        nbt.setTag("resources", resourcesTag);
    }

    private static String unixifyPath(String original) {
        Path p = Paths.get(original);
        String[] components = new String[p.getNameCount()];
        for (int i = 0; i < components.length; i++) {
            components[i] = p.getName(i).toString();
        }
        return String.join("/", components);
    }

    private static void matchPathsRecursive(Map<String, Path> pathMap, Path parent, Path current, ArrayList<PathMatcher> matchers) {
        File f = current.toFile();
        if (f.isFile()) {
            Path relative = parent.toAbsolutePath().relativize(current.toAbsolutePath()).normalize();
            for (PathMatcher m :
                    matchers) {
                if (m.matches(relative)) {
                    pathMap.put(relative.toString(), current);
                    break;
                }
            }
        }
        else {
            for (File fl :
                    current.toFile().listFiles()) {
                matchPathsRecursive(pathMap, parent, fl.toPath(), matchers);
            }
        }
    }

    private static void loadScripts(Path path, NBTTagCompound nbt) throws IOException {
        List<Path> scripts = IOUtils.getFilesByExtension(path, ".lua");
        if (scripts.size() > 0) {
            NBTTagCompound scriptsNbt = new NBTTagCompound();
            String pathRegex = path.toString().isEmpty() ? "\\Q\\E" : Pattern.quote(path + path.getFileSystem().getSeparator());
            for (Path script : scripts) {
                String name = script.toString()
                        .replaceFirst(pathRegex, "")
                        .replaceAll("[/\\\\]", ".");
                name = name.substring(0, name.length() - 4);
                scriptsNbt.setTag(name, LuaScriptParser.parseScript(name, IOUtils.readFile(script)));
            }
            nbt.setTag("scripts", scriptsNbt);
        }
    }

    private static void loadSounds(Path path, NBTTagCompound nbt) throws IOException {
        List<Path> sounds = IOUtils.getFilesByExtension(path, ".ogg");
        if (sounds.size() > 0) {
            NBTTagCompound soundsNbt = new NBTTagCompound();
            String pathRegex = Pattern.quote(path.toString().isEmpty() ? path.toString() : path + path.getFileSystem().getSeparator());
            for (Path sound : sounds) {
                String name = sound.toString()
                        .replaceFirst(pathRegex, "")
                        .replaceAll("[/\\\\]", ".");
                name = name.substring(0, name.length() - 4);
                soundsNbt.setByteArray(name, IOUtils.readFileBytes(sound));
            }
            nbt.setTag("sounds", soundsNbt);
        }
    }

    private static NBTTagCompound loadModels(Path avatarFolder, Path currentFile, BlockbenchModelParser parser, NBTTagCompound textures, NBTTagList animations, String folders) throws Exception {
        NBTTagCompound result = new NBTTagCompound();
        List<Path> subFiles = IOUtils.listPaths(currentFile);
        NBTTagList children = new NBTTagList();
        if (subFiles != null)
            for (Path file : subFiles) {
                if (IOUtils.isHidden(file))
                    continue;
                String name = IOUtils.getFileNameOrEmpty(file);
                if (Files.isDirectory(file)) {
                    NBTTagCompound subfolder = loadModels(avatarFolder, file, parser, textures, animations, folders + name + ".");
                    if (!subfolder.hasNoTags()) {
                        subfolder.setString("name", name);
                        BlockbenchModelParser.parseParent(name, subfolder);
                        children.appendTag(subfolder);
                    }
                } else if (file.toString().toLowerCase().endsWith(".bbmodel")) {
                    BlockbenchModelParser.ModelData data = parser.parseModel(avatarFolder, file, IOUtils.readFile(file), name.substring(0, name.length() - 8), folders);
                    children.appendTag(data.modelNbt());
                    data.animationList().forEach(animations::appendTag);

                    NBTTagCompound dataTag = data.textures();
                    if (dataTag.hasNoTags())
                        continue;

                    if (textures.hasNoTags()) {
                        textures.setTag("data", new NBTTagList());
                        textures.setTag("src", new NBTTagCompound());
                    }
                    for (int i = 0; i < dataTag.getTagList("data", NbtType.COMPOUND.getValue()).tagCount(); i++) {
                        textures.getTagList("data", NbtType.COMPOUND.getValue()).appendTag(dataTag.getTagList("data", NbtType.COMPOUND.getValue()).getCompoundTagAt(i));
                    }
                    textures.getCompoundTag("src").merge(dataTag.getCompoundTag("src"));
                }
            }

        if (children.tagCount() > 0)
            result.setTag("chld", children);

        return result;
    }

    /**
     * Tick the watched key for hotswapping avatars
     */
    public static void tick() {
        WatchEvent<?> event = null;
        boolean reload = false;

        for (Map.Entry<Path, WatchKey> entry : KEYS.entrySet()) {
            WatchKey key = entry.getValue();
            if (!key.isValid())
                continue;

            for (WatchEvent<?> watchEvent : key.pollEvents()) {
                WatchEvent.Kind<?> kind = watchEvent.kind();
                if (kind == StandardWatchEventKinds.OVERFLOW)
                    continue;

                event = watchEvent;
                Path path = entry.getKey().resolve((Path) event.context());
                String name = IOUtils.getFileNameOrEmpty(path);

                if (IOUtils.isHidden(path) || !(Files.isDirectory(path) || name.matches("(.*(\\.lua|\\.bbmodel|\\.ogg|\\.png)$|avatar\\.json)")))
                    continue;

                if (kind == StandardWatchEventKinds.ENTRY_CREATE && !IS_WINDOWS)
                    addWatchKey(path, KEYS::put);

                reload = true;
                break;
            }

            if (reload)
                break;
        }

        // reload avatar
        if (reload) {
            FiguraMod.debug("Detected file changes in the Avatar directory (" + event.context().toString() + "), reloading!");
            AvatarManager.loadLocalAvatar(lastLoadedPath);
        }
    }

    public static void resetWatchKeys() {
        lastLoadedPath = null;
        for (WatchKey key : KEYS.values())
            key.cancel();
        KEYS.clear();
    }

    /**
     * register new watch keys
     *
     * @param path the path to register the watch key
     * @param consumer a consumer that will process the watch key and its path
     */
    protected static void addWatchKey(Path path, BiConsumer<Path, WatchKey> consumer) {
        if (watcher == null || path == null || path.getFileSystem() != FileSystems.getDefault())
            return;

        if (!Files.isDirectory(path) || IOUtils.isHidden(path))
            return;

        try {
            WatchEvent.Kind<?>[] events = {StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY};
            WatchKey key = null;
            if (IS_WINDOWS) {
                Class c = Class.forName("com.sun.nio.file.ExtendedWatchEventModifier");
                for (Object obj : c.getEnumConstants()) {
                    try {
                        Method m = c.getMethod("value", null);
                        Object modifier = m.invoke(obj, null);
                        if (modifier instanceof WatchEvent.Modifier) {
                            key = path.register(watcher, events, (WatchEvent.Modifier) obj);
                        } else {
                            key = path.register(watcher, events);
                        }
                        break;
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                        FiguraMod.LOGGER.warn("Could not find tree file modifier, avatar list might not refresh automatically!");
                        key = path.register(watcher, events);
                        break;
                    }
                }
            } else {
                key = path.register(watcher, events);
            }
            consumer.accept(path, key);

            List<Path> children = IOUtils.listPaths(path);
            if (children == null || IS_WINDOWS)
                return;

            for (Path child : children)
                addWatchKey(child, consumer);
        } catch (Exception e) {
            FiguraMod.LOGGER.error("Failed to register watcher for " + path, e);
        }
    }

    public static Path getLastLoadedPath() {
        return lastLoadedPath;
    }

    public static String getLoadState() {
        return loadState.name().toLowerCase();
    }

    public static String getLoadError() {
        return loadError;
    }

    private enum LoadState {
        UNKNOWN,
        SCRIPTS,
        SOUNDS,
        MODELS,
        METADATA
    }
}
