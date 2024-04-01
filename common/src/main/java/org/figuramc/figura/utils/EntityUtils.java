package org.figuramc.figura.utils;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import org.figuramc.figura.mixin.WorldClientInvoker;
import org.figuramc.figura.mixin.EntityAccessor;
import org.figuramc.figura.mixin.gui.PlayerTabOverlayAccessor;

import java.util.*;

public class EntityUtils {

    protected static Map<UUID, Integer> uuidToNetworkID = new HashMap<>();
    private static World level;
    public static Entity getEntityByUUID(UUID uuid) {
        if (Minecraft.getMinecraft().world == null)
            return null;
        // Invalidate if the level has changed
        if (level == null)
            level = Minecraft.getMinecraft().world;
        if (level != Minecraft.getMinecraft().world)
            uuidToNetworkID.clear();

        if (uuidToNetworkID.containsKey(uuid))
            return Minecraft.getMinecraft().world.getEntityByID(uuidToNetworkID.get(uuid));

        for (Entity entity : ((WorldClientInvoker) Minecraft.getMinecraft().world).getEntityList())
            if (uuid.equals(entity.getUniqueID())) {
                uuidToNetworkID.put(uuid, entity.getEntityId());
                return entity;
            }

        return null;
    }

    public static Entity getViewedEntity(float distance) {
        Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
        if (entity == null) return null;

        float tickDelta = Minecraft.getMinecraft().getRenderPartialTicks();
        Vec3d entityEye = entity.getPositionEyes(tickDelta);
        Vec3d viewVec = entity.getLook(tickDelta).scale(distance);
        AxisAlignedBB box = entity.getEntityBoundingBox().expand(viewVec.x, viewVec.y, viewVec.z).grow(1f, 1f, 1f);

        Vec3d raycastEnd = entityEye.add(viewVec);

        double raycastDistanceSquared; // Has to be squared for some reason, thanks minecraft for not making that clear
        RayTraceResult blockResult = ((EntityAccessor) entity).getWorld().rayTraceBlocks(entityEye, raycastEnd, false);
        if (blockResult != null)
            raycastDistanceSquared = blockResult.getBlockPos().distanceSq(entityEye.x, entityEye.y, entityEye.z);
        else
            raycastDistanceSquared = distance * distance;


        Entity entityHit = null;
        for (Entity currentEntity : entity.getEntityWorld().getEntitiesWithinAABBExcludingEntity(entity, box)) {
            AxisAlignedBB entityAABB = currentEntity.getEntityBoundingBox().grow(currentEntity.getCollisionBorderSize());
            RayTraceResult rayTraceResult = entityAABB.calculateIntercept(entityEye, raycastEnd);
            if (entityAABB.contains(entityEye)) {
                if (raycastDistanceSquared >= 0.0f) {
                    entityHit = currentEntity;
                    entityEye = rayTraceResult == null ? entityEye : rayTraceResult.hitVec;
                    raycastDistanceSquared = 0.0f;
                }
            } else if (rayTraceResult != null) {
                double eyeDistanceToHit = entityEye.distanceTo(new Vec3d(rayTraceResult.getBlockPos()));
                if (eyeDistanceToHit < raycastDistanceSquared || raycastDistanceSquared == 0.0f) {
                    if (currentEntity.getLowestRidingEntity() == entity.getLowestRidingEntity()) {
                        if (raycastDistanceSquared == 0.0f) {
                            entityHit = currentEntity;
                        }
                    } else {
                        entityHit = currentEntity;
                        raycastDistanceSquared = eyeDistanceToHit;
                    }
                }
            }
        }

        return entityHit;
    }

    public static NetworkPlayerInfo getPlayerInfo(UUID uuid) {
        NetHandlerPlayClient connection = Minecraft.getMinecraft().getConnection();
        return connection == null ? null : connection.getPlayerInfo(uuid);
    }

    public static String getNameForUUID(UUID uuid) {
        NetworkPlayerInfo player = getPlayerInfo(uuid);
        if (player != null)
            return player.getGameProfile().getName();

        Entity e = getEntityByUUID(uuid);
        if (e != null)
            return e.getName();

        return null;
    }

    public static Map<String, UUID> getPlayerList() {
        NetHandlerPlayClient connection = Minecraft.getMinecraft().getConnection();
        if (connection == null || connection.getPlayerInfoMap().isEmpty())
            return new HashMap<>();

        Map<String, UUID> playerList = new HashMap<>();

        for (NetworkPlayerInfo info : connection.getPlayerInfoMap()) {
            NetworkPlayerInfo player = connection.getPlayerInfo(info.getGameProfile().getId());
            if (player != null)
                playerList.put(player.getGameProfile().getName(), info.getGameProfile().getId());
        }

        return playerList;
    }

    public static List<NetworkPlayerInfo> getTabList() {
        NetHandlerPlayClient clientPacketListener = Minecraft.getMinecraft().getConnection();
        if (clientPacketListener == null)
            return new ArrayList<>();

        return PlayerTabOverlayAccessor.getPlayerOrdering().sortedCopy(clientPacketListener.getPlayerInfoMap());
    }

    public static boolean checkInvalidPlayer(UUID id) {
        if (id.version() != 4)
            return true;

        NetworkPlayerInfo playerInfo = getPlayerInfo(id);
        if (playerInfo == null)
            return false;

        GameProfile profile = playerInfo.getGameProfile();
        String name = profile.getName();
        return name != null && (name.trim().isEmpty() || name.charAt(0) == '\u0000');
    }

    public static boolean isEntityUpsideDown(EntityLivingBase livingEntity) {
        String string;
        if ((livingEntity instanceof EntityPlayer || livingEntity.hasCustomName()) && ("Dinnerbone".equals(string = TextFormatting.getTextWithoutFormattingCodes(livingEntity.getName())) || "Grumm".equals(string))) {
            return !(livingEntity instanceof EntityPlayer) || ((EntityPlayer)livingEntity).isWearing(EnumPlayerModelParts.CAPE);
        }
        return false;
    }
}
