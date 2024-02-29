package org.figuramc.figura.lua.api.entity;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.IInventoryChangedListener;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.management.UserListOpsEntry;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.NbtToLua;
import org.figuramc.figura.lua.ReadOnlyLuaTable;
import org.figuramc.figura.lua.api.world.ItemStackAPI;
import org.figuramc.figura.lua.docs.LuaMetamethodDoc;
import org.figuramc.figura.lua.docs.LuaMetamethodDoc.LuaMetamethodOverload;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaMethodOverload;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.math.vector.FiguraVec2;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.utils.EntityUtils;
import org.figuramc.figura.utils.LuaUtils;
import org.figuramc.figura.utils.MathUtils;
import org.figuramc.figura.utils.RegistryUtils;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@LuaWhitelist
@LuaTypeDoc(
        name = "EntityAPI",
        value = "entity"
)
public class EntityAPI<T extends Entity> {

    protected final UUID entityUUID;
    protected T entity; // We just do not care about memory anymore so, just have something not wrapped in a WeakReference

    private boolean thingy = true;
    private String cacheType;

    public EntityAPI(T entity) {
        this.entity = entity;
        entityUUID = entity.getUniqueID();
    }

    public static EntityAPI<?> wrap(Entity e) {
        if (e == null)
            return null;
        if (e instanceof EntityPlayer) {
            EntityPlayer p = (EntityPlayer) e;
            return new PlayerAPI(p);
        }
        if (e instanceof EntityLivingBase) {
            EntityLivingBase le = (EntityLivingBase) e;
            return new LivingEntityAPI<>(le);
        }
        return new EntityAPI<>(e);
    }

    protected final void checkEntity() {
        if (entity.addedToChunk || getLevel() != Minecraft.getMinecraft().world) {
            T newEntityInstance = (T) EntityUtils.getEntityByUUID(entityUUID);
            thingy = newEntityInstance != null;
            if (thingy)
                entity = newEntityInstance;
        }
    }

    protected World getLevel() {
        return entity.getEntityWorld();
    }

    public T getEntity() {
        return entity;
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.is_loaded")
    public boolean isLoaded() {
        checkEntity();
        return thingy;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload,
                    @LuaMethodOverload(
                            argumentTypes = Float.class,
                            argumentNames = "delta"
                    )
            },
            value = "entity.get_pos"
    )
    public FiguraVec3 getPos(Float delta) {
        checkEntity();
        if (delta == null) delta = 1f;
        double d = MathUtils.lerp((double)delta, entity.prevPosX, entity.posX);
        double e = MathUtils.lerp((double)delta, entity.prevPosY, entity.posY);
        double f = MathUtils.lerp((double)delta, entity.prevPosZ, entity.posZ);
        return FiguraVec3.of(d,e,f);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload,
                    @LuaMethodOverload(
                            argumentTypes = Float.class,
                            argumentNames = "delta"
                    )
            },
            value = "entity.get_rot"
    )
    public FiguraVec2 getRot(Float delta) {
        checkEntity();
        if (delta == null) delta = 1f;
        return FiguraVec2.of(MathUtils.lerp(delta, entity.prevRotationPitch, entity.rotationPitch), MathUtils.lerp(delta, entity.prevRotationYaw, entity.rotationYaw));
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.get_uuid")
    public String getUUID() {
        return entityUUID.toString();
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.get_type")
    public String getType() {
        checkEntity();
        return cacheType != null ? cacheType : (cacheType = RegistryUtils.getResourceLocationForRegistryObj(Entity.class, entity).toString());
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.get_velocity")
    public FiguraVec3 getVelocity() {
        checkEntity();
        return FiguraVec3.of(entity.posX - entity.prevPosX, entity.posY - entity.prevPosY, entity.posZ - entity.prevPosZ);
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.get_look_dir")
    public FiguraVec3 getLookDir() {
        checkEntity();
        return FiguraVec3.fromVec3(entity.getLookVec());
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.get_frozen_ticks")
    public int getFrozenTicks() {
        checkEntity();
        return 0;
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.get_max_air")
    public int getMaxAir() {
        checkEntity();
        return entity.getAir();
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.get_dimension_name")
    public String getDimensionName() {
        checkEntity();
        return DimensionType.getById(entity.dimension).toString();
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.get_pose")
    public String getPose() {
        checkEntity();
        return entity instanceof EntityLivingBase ? ((EntityLivingBase) entity).isElytraFlying() ? "FALL_FLYING" : (((EntityLivingBase) entity).isPlayerSleeping() ? "SLEEPING" : (entity.isSneaking() && !(entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.isFlying) ? "CROUCHING" : "STANDING")): entity.isSneaking() ? "CROUCHING" : "STANDING";
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.get_vehicle")
    public EntityAPI<?> getVehicle() {
        checkEntity();
        return wrap(entity.getRidingEntity());
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.is_on_ground")
    public boolean isOnGround() {
        checkEntity();
        return entity.onGround;
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.get_eye_height")
    public float getEyeHeight() {
        checkEntity();
        return entity.getEyeHeight();
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.get_bounding_box")
    public FiguraVec3 getBoundingBox() {
        checkEntity();
        AxisAlignedBB dim = entity.getCollisionBoundingBox();
        if (dim != null)
            return FiguraVec3.of(dim.maxX - dim.minX, dim.maxY - dim.minY, dim.maxZ - dim.minZ);
        return FiguraVec3.of();
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.get_name")
    public String getName() {
        checkEntity();
        return entity.getName();
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.is_wet")
    public boolean isWet() {
        checkEntity();
        return entity.isWet();
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.is_in_water")
    public boolean isInWater() {
        checkEntity();
        return entity.isInWater();
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.is_underwater")
    public boolean isUnderwater() {
        checkEntity();
        Block block = entity.getEntityWorld().getBlockState(entity.getPosition()).getBlock();
        return block == Blocks.WATER || block == Blocks.FLOWING_WATER;
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.is_in_lava")
    public boolean isInLava() {
        checkEntity();
        return entity.isInLava();
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.is_in_rain")
    public boolean isInRain() {
        checkEntity();
        BlockPos blockPos = entity.getPosition();
        return getLevel().isRainingAt(blockPos) || getLevel().isRainingAt(new BlockPos(blockPos.getX(), (int) entity.getCollisionBoundingBox().maxY, (int) entity.posZ));
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.has_avatar")
    public boolean hasAvatar() {
        checkEntity();
        return AvatarManager.getAvatar(entity) != null;
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.is_sprinting")
    public boolean isSprinting() {
        checkEntity();
        return entity.isSprinting();
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.get_eye_y")
    public double getEyeY() {
        checkEntity();
        return entity.getEyeHeight();
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.is_glowing")
    public boolean isGlowing() {
        checkEntity();
        return entity.isGlowing();
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.is_invisible")
    public boolean isInvisible() {
        checkEntity();
        return entity.isInvisible();
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.is_silent")
    public boolean isSilent() {
        checkEntity();
        return entity.isSilent();
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.is_sneaking")
    public boolean isSneaking() {
        checkEntity();
        return entity.isSneaking();
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.is_crouching")
    public boolean isCrouching() {
        checkEntity();
        return entity.isSneaking();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = int.class,
                    argumentNames = "index"
            ),
            value = "entity.get_item"
    )
    public ItemStackAPI getItem(int index) {
        checkEntity();
        if (--index < 0)
            return null;

        int i = 0;
        for (ItemStack item : entity.getEquipmentAndArmor()) {
            if (i == index)
                return ItemStackAPI.verify(item);
            i++;
        }

        return null;
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.get_nbt")
    public LuaTable getNbt() {
        checkEntity();
        NBTTagCompound tag = new NBTTagCompound();
        entity.writeToNBT(tag);
        return (LuaTable) NbtToLua.convert(tag);
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.is_on_fire")
    public boolean isOnFire() {
        checkEntity();
        return entity.canRenderOnFire();
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.is_alive")
    public boolean isAlive() {
        checkEntity();
        return entity.isEntityAlive();
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.get_permission_level")
    public int getPermissionLevel() {
        checkEntity();
        if (entity instanceof EntityPlayer && Minecraft.getMinecraft().getIntegratedServer() != null){
            UserListOpsEntry userlistopsentry = Minecraft.getMinecraft().getIntegratedServer().getPlayerList().getOppedPlayers().getEntry(((EntityPlayer) entity).getGameProfile());
            return userlistopsentry.getPermissionLevel();
        }
        return entity.canUseCommand(4, "") ? 4 : entity.canUseCommand(3, "") ? 3 : entity.canUseCommand(2, "") ? 2 : entity.canUseCommand(1, "") ? 1 : 0;
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.get_passengers")
    public List<EntityAPI<?>> getPassengers() {
        checkEntity();

        List<EntityAPI<?>> list = new ArrayList<>();
        for (Entity passenger : entity.getPassengers())
            list.add(wrap(passenger));
        return list;
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.get_controlling_passenger")
    public EntityAPI<?> getControllingPassenger() {
        checkEntity();
        return wrap(entity.getControllingPassenger());
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.get_controlled_vehicle")
    public EntityAPI<?> getControlledVehicle() {
        checkEntity();
        return wrap(entity.getLowestRidingEntity());
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.has_container")
    public boolean hasContainer() {
        checkEntity();
        return entity instanceof IInventory;
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.has_inventory")
    public boolean hasInventory() {
        checkEntity();
        return entity instanceof IInventoryChangedListener;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload,
                    @LuaMethodOverload(
                            argumentTypes = Boolean.class,
                            argumentNames = "ignoreLiquids"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Boolean.class, Double.class},
                            argumentNames = {"ignoreLiquids", "distance"}
                    )
            },
            value = "entity.get_targeted_block"
    )
    public Object[] getTargetedBlock(boolean ignoreLiquids, Double distance) {
        checkEntity();
        if (distance == null) distance = 20d;
        distance = Math.max(Math.min(distance, 20), -20);
        Vec3d startVec = entity.getPositionEyes(1f);
        Vec3d look = entity.getLook(1f);
        Vec3d endVec = startVec.add(new Vec3d(look.x * distance, look.y * distance, look.z * distance));

        RayTraceResult result = entity.getEntityWorld().rayTraceBlocks(startVec, endVec, !ignoreLiquids);
        return LuaUtils.parseBlockHitResult(result);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload,
                    @LuaMethodOverload(
                            argumentTypes = Double.class,
                            argumentNames = "distance"
                    )
            },
            value = "entity.get_targeted_entity"
    )
    public Object[] getTargetedEntity(Double distance) {
        checkEntity();
        if (distance == null) distance = 20d;
        distance = Math.max(Math.min(distance, 20), 0);

        Vec3d eyePosition = entity.getPositionEyes(1f);
        Vec3d look = this.entity.getLook(1f);
        Vec3d endPosition = eyePosition.add(new Vec3d(look.x * distance, look.y * distance, look.z * distance));

        RayTraceResult result = entity.world.rayTraceBlocks(eyePosition, endPosition, false);

        if (result != null)
            distance = result.getBlockPos().distanceSq(eyePosition.x, eyePosition.y, eyePosition.z);

        Vec3d endPos = eyePosition.add(new Vec3d(look.x * distance, look.y * distance, look.z * distance));
        Vec3d scaled = look.scale(distance);
        AxisAlignedBB aABB = entity.getEntityBoundingBox().expand(scaled.x, scaled.y, scaled.z).grow(1d);

        Entity entityHit = null;
        Vec3d hitPosition = null;

        for (Entity currentEntity : entity.getEntityWorld().getEntitiesWithinAABBExcludingEntity(entity, aABB)) {
            AxisAlignedBB entityAABB = currentEntity.getEntityBoundingBox().grow(currentEntity.getCollisionBorderSize());
            RayTraceResult rayTraceResult = entityAABB.calculateIntercept(eyePosition, endPos);
            if (entityAABB.contains(eyePosition)) {
                if (distance >= 0.0) {
                    entityHit = currentEntity;
                    hitPosition = rayTraceResult == null ? eyePosition : rayTraceResult.hitVec;
                    distance = 0.0;
                }
            } else if (rayTraceResult != null) {
                double eyeDistanceToHit = eyePosition.distanceTo(new Vec3d(rayTraceResult.getBlockPos()));
                if (eyeDistanceToHit < distance || distance == 0.0) {
                    if (currentEntity.getLowestRidingEntity() == entity.getLowestRidingEntity()) {
                        if (distance == 0.0) {
                            entityHit = currentEntity;
                            hitPosition = rayTraceResult.hitVec;
                        }
                    } else {
                        entityHit = currentEntity;
                        hitPosition = rayTraceResult.hitVec;
                        distance = eyeDistanceToHit;
                    }
                }
            }
        }

        if (entityHit != null && hitPosition != null) {
            RayTraceResult traceResult = new RayTraceResult(entityHit, hitPosition);
            return new Object[]{EntityAPI.wrap(traceResult.entityHit), FiguraVec3.fromBlockPos(traceResult.getBlockPos())};
        }
        return null;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload,
                    @LuaMethodOverload(
                            argumentTypes = String.class,
                            argumentNames = "key"
                    )
            },
            value = "entity.get_variable"
    )
    public LuaValue getVariable(String key) {
        checkEntity();
        Avatar a = AvatarManager.getAvatar(entity);
        LuaTable table = a == null || a.luaRuntime == null ? new LuaTable() : a.luaRuntime.avatar_meta.storedStuff;
        table = new ReadOnlyLuaTable(table);
        return key == null ? table : table.get(key);
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.is_living")
    public boolean isLiving() {
        return this instanceof LivingEntityAPI<?>;
    }

    @LuaWhitelist
    @LuaMethodDoc("entity.is_player")
    public boolean isPlayer() {
        return this instanceof PlayerAPI;
    }

    @LuaWhitelist
    @LuaMetamethodDoc(
            overloads = @LuaMetamethodOverload(
                    types = {boolean.class, EntityAPI.class, EntityAPI.class}
            )
    )
    public boolean __eq(EntityAPI<?> rhs) {
        return this.entity.equals(rhs.entity);
    }

    @LuaWhitelist
    @LuaMetamethodDoc(
            overloads = @LuaMetamethodOverload(
                    types = {String.class, EntityAPI.class}
            )
    )
    public String __tostring() {
        return toString();
    }

    @Override
    public String toString() {
        checkEntity();
        return (entity.hasCustomName() ? entity.getCustomNameTag() + " (" + getType() + ")" : getType()) + " (Entity)";
    }
}
