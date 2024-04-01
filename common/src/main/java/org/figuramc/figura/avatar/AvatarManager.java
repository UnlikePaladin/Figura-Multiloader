package org.figuramc.figura.avatar;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.local.LocalAvatarLoader;
import org.figuramc.figura.backend2.NetworkStuff;
import org.figuramc.figura.commands.FiguraCommands;
import org.figuramc.figura.gui.FiguraToast;
import org.figuramc.figura.gui.widgets.lists.AvatarList;
import org.figuramc.figura.lua.api.particle.ParticleAPI;
import org.figuramc.figura.lua.api.sound.SoundAPI;
import org.figuramc.figura.platform.Services;
import org.figuramc.figura.utils.*;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages all the avatars that are currently loaded in memory, and also
 * handles getting the avatars of entities. If an entity does not have a loaded avatar,
 * the AvatarManager will fetch the avatar and cache it.
 */
public class AvatarManager {

    private static final Map<UUID, UserData> LOADED_USERS = new ConcurrentHashMap<>();
    private static final Set<UUID> FETCHED_USERS = new HashSet<>();

    private static final Map<Entity, Avatar> LOADED_CEM = new ConcurrentHashMap<>();

    public static final FiguraResourceListener RESOURCE_RELOAD_EVENT = Services.FIGURA_RESOURCE_LISTENER.createResourceListener("resource_reload_event", manager -> executeAll("resourceReloadEvent", Avatar::resourceReloadEvent));

    public static boolean localUploaded = true; // init as true :3
    public static boolean panic = false;

    // -- panic mode -- //

    public static void togglePanic() {
        AvatarManager.panic = !AvatarManager.panic;
        FiguraToast.sendToast(new FiguraText(AvatarManager.panic ? "toast.panic_enabled" : "toast.panic_disabled"), FiguraToast.ToastType.WARNING);
        SoundAPI.getSoundEngine().figura$stopAllSounds();
        ParticleAPI.getParticleEngine().figura$clearParticles(null);
    }

    // -- avatar events -- //

    public static void tickLoadedAvatars() {
        if (panic)
            return;

        // tick the avatars
        for (UserData user : LOADED_USERS.values()) {
            Avatar avatar = user.getMainAvatar();
            if (avatar != null) {
                FiguraMod.pushProfiler(avatar);
                avatar.tick();
                FiguraMod.popProfiler();
            }
        }

        // CEM
        if (LOADED_CEM.isEmpty())
            return;

        // unload entities
        Set<Entity> toBeRemoved = new HashSet<>();

        for (Entity entity : LOADED_CEM.keySet())
            if (entity != null && !Minecraft.getMinecraft().world.loadedEntityList.contains(entity))
                toBeRemoved.add(entity);

        for (Entity entity : toBeRemoved)
            LOADED_CEM.remove(entity);

        // tick entities
        for (Avatar avatar : LOADED_CEM.values()) {
            if (avatar != null) {
                FiguraMod.pushProfiler(avatar);
                avatar.tick();
                FiguraMod.popProfiler();
            }
        }
    }

    public static void executeAll(String src, Consumer<Avatar> consumer) {
        if (panic) return;

        FiguraMod.pushProfiler(FiguraMod.MOD_ID);
        FiguraMod.pushProfiler(src);

        for (UserData user : LOADED_USERS.values()) {
            Avatar avatar = user.getMainAvatar();
            if (avatar != null) {
                FiguraMod.pushProfiler(avatar);
                consumer.accept(avatar);
                FiguraMod.popProfiler();
            }
        }

        for (Avatar avatar : LOADED_CEM.values()) {
            if (avatar != null) {
                FiguraMod.pushProfiler(avatar);
                consumer.accept(avatar);
                FiguraMod.popProfiler();
            }
        }

        FiguraMod.popProfiler(2);
    }

    // -- avatar getters -- //

    // player will also attempt to load from network, if possible
    public static Avatar getAvatarForPlayer(UUID player) {
        if (panic || Minecraft.getMinecraft().world == null)
            return null;

        fetchBackend(player);

        UserData user = LOADED_USERS.get(player);
        return user == null ? null : user.getMainAvatar();
    }

    private static Avatar getAvatarForEntity(Entity entity) {
        // get loaded
        Avatar loaded = LOADED_CEM.get(entity);
        if (loaded != null)
            return loaded;

        // new avatar
        ResourceLocation type = EntityList.getKey(entity);
        NBTTagCompound nbt = LocalAvatarLoader.CEM_AVATARS.get(type);
        return nbt == null ? null : loadEntityAvatar(entity, nbt);
    }

    // tries to get data from an entity
    public static Avatar getAvatar(Entity entity) {
        if (panic || Minecraft.getMinecraft().world == null || entity == null)
            return null;

        UUID uuid = entity.getUniqueID();

        // load from player (fetch backend) if is a player
        if (entity instanceof EntityPlayer){
            Avatar avatar = getAvatarForPlayer(uuid);
            if (avatar != null)
                return avatar;
        }

        // otherwise check for CEM
        return getAvatarForEntity(entity);
    }

    // get a loaded avatar without fetching backend or creating a new one
    public static Avatar getLoadedAvatar(UUID owner) {
        if (panic || Minecraft.getMinecraft().world == null)
            return null;

        UserData user = LOADED_USERS.get(owner);
        return user == null ? null : user.getMainAvatar();
    }

    // get all main loaded avatars
    public static List<Avatar> getLoadedAvatars() {
        List<Avatar> list = new ArrayList<>();
        for (UserData user : LOADED_USERS.values()) {
            Avatar avatar = user.getMainAvatar();
            if (avatar != null && avatar.nbt != null)
                list.add(avatar);
        }
        return list;
    }

    // -- avatar management -- //

    // removes an loaded avatar
    public static void clearAvatars(UUID id) {
        FETCHED_USERS.remove(id);

        UserData user = LOADED_USERS.get(id);
        if (user != null) user.clear();

        NetworkStuff.clear(id);
        FiguraMod.debug("Cleared avatars of " + id);
    }

    public static void clearCEMAvatars() {
        for (Avatar avatar : LOADED_CEM.values())
            avatar.clean();
        LOADED_CEM.clear();
    }

    // clears ALL loaded avatars, including local
    public static void clearAllAvatars() {
        for (UUID id : LOADED_USERS.keySet())
            clearAvatars(id);

        LOADED_USERS.clear();
        FETCHED_USERS.clear();
        clearCEMAvatars();

        localUploaded = true;
        AvatarList.selectedEntry = null;
        LocalAvatarLoader.loadAvatar(null, null);
        FiguraMod.LOGGER.info("Cleared all avatars");
    }

    // reloads an avatar
    public static void reloadAvatar(UUID id) {
        if (!localUploaded && FiguraMod.isLocal(id))
            loadLocalAvatar(LocalAvatarLoader.getLastLoadedPath());
        else
            clearAvatars(id);
    }

    // load the local player avatar
    public static void loadLocalAvatar(Path path) {
        UUID id = FiguraMod.getLocalPlayerUUID();

        // clear
        clearAvatars(id);
        FETCHED_USERS.add(id);

        // load
        UserData user = LOADED_USERS.computeIfAbsent(id, UserData::new);
        LocalAvatarLoader.loadAvatar(path, user);

        // mark as not uploaded
        localUploaded = false;
    }

    // load CEM avatar
    public static Avatar loadEntityAvatar(Entity entity, NBTTagCompound nbt) {
        Avatar targetAvatar = new Avatar(entity);
        targetAvatar.load(nbt);
        LOADED_CEM.put(entity, targetAvatar);
        return targetAvatar;
    }

    // set an user's avatar
    public static void setAvatar(UUID id, NBTTagCompound nbt) {
        try {
            UserData user = LOADED_USERS.computeIfAbsent(id, UserData::new);
            user.clear();
            user.loadAvatar(nbt);
        } catch (Exception e) {
            FiguraMod.LOGGER.error("Failed to set avatar for " + id, e);
        }
    }

    // get avatar from the backend
    private static void fetchBackend(UUID id) {
        if (FETCHED_USERS.contains(id))
            return;

        FETCHED_USERS.add(id);

        if (EntityUtils.checkInvalidPlayer(id)) {
            FiguraMod.debug("Voiding userdata for " + id);
            return;
        }

        UserData user = LOADED_USERS.computeIfAbsent(id, UserData::new);

        FiguraMod.debug("Getting userdata for " + id);
        NetworkStuff.getUser(user);
    }

    // -- badges -- //

    public static Pair<BitSet, BitSet> getBadges(UUID id) {
        UserData user = LOADED_USERS.get(id);
        if (user == null)
            return null;

        Pair<BitSet, BitSet> badges = user.getBadges();
        if (badges != null)
            return badges;

        badges = Badges.emptyBadges();
        user.loadBadges(badges);
        return badges;
    }

    // -- command -- //
    public static class SetAvatarCommand extends FiguraCommands.FiguraSubCommand {

        public SetAvatarCommand() {
            super("set_avatar");
        }

        @Override
        public void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] args) throws CommandException {
            if (args.length == 2) {
                String t = args[1];
                String s = args[0];

                UUID sourceUUID, targetUUID;
                try {
                    sourceUUID = UUID.fromString(s);
                    targetUUID = UUID.fromString(t);
                } catch (Exception e) {
                    ((FiguraClientCommandSource) iCommandSender).figura$sendError(new TextComponentString("Failed to parse uuids"));
                    return;
                }

                UserData user = LOADED_USERS.get(sourceUUID);
                Avatar avatar = user == null ? null : user.getMainAvatar();
                if (avatar == null || avatar.nbt == null) {
                    ((FiguraClientCommandSource) iCommandSender).figura$sendError(new TextComponentString("No source Avatar found"));
                    return;
                }

                if (LOADED_USERS.get(targetUUID) != null) {
                    setAvatar(targetUUID, avatar.nbt);
                    ((FiguraClientCommandSource) iCommandSender).figura$sendError(new TextComponentString("Set avatar for " + t));
                    return;
                }

                Entity targetEntity = EntityUtils.getEntityByUUID(targetUUID);
                if (targetEntity == null) {
                    ((FiguraClientCommandSource) iCommandSender).figura$sendError(new TextComponentString("Target entity not found"));
                    return;
                }

                loadEntityAvatar(targetEntity, avatar.nbt);
            }
        }
    }
}
