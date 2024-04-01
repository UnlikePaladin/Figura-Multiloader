package org.figuramc.figura.lua.api.world;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.lua.LuaNotNil;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.ReadOnlyLuaTable;
import org.figuramc.figura.lua.api.entity.EntityAPI;
import org.figuramc.figura.lua.api.entity.PlayerAPI;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaMethodOverload;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.math.vector.FiguraVec2;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.utils.EntityUtils;
import org.figuramc.figura.utils.FiguraFlattenerUtils;
import org.figuramc.figura.utils.LuaUtils;
import org.figuramc.figura.utils.Pair;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;

import java.util.*;

@LuaWhitelist
@LuaTypeDoc(
        name = "WorldAPI",
        value = "world"
)
public class WorldAPI {

    public static final WorldAPI INSTANCE = new WorldAPI();

    public static WorldClient getCurrentWorld() {
        return Minecraft.getMinecraft().world;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "pos"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            value = "world.get_biome"
    )
    public static BiomeAPI getBiome(Object x, Double y, Double z) {
        FiguraVec3 pos = LuaUtils.parseVec3("getBiome", x, y, z);
        return new BiomeAPI(getCurrentWorld().getBiome(pos.asBlockPos()), pos.asBlockPos());
    }

    @SuppressWarnings("deprecation")
    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "pos"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            value = "world.get_block_state"
    )
    public static BlockStateAPI getBlockState(Object x, Double y, Double z) {
        FiguraVec3 pos = LuaUtils.parseVec3("getBlockState", x, y, z);
        BlockPos blockPos = pos.asBlockPos();
        World world = getCurrentWorld();
        if (!world.isBlockLoaded(blockPos))
            return new BlockStateAPI(Blocks.AIR.getDefaultState(), blockPos);
        return new BlockStateAPI(world.getBlockState(blockPos), blockPos);
    }
    @SuppressWarnings("deprecation")
    @LuaWhitelist
    @LuaMethodDoc(
        overloads = {
            @LuaMethodOverload(
                argumentTypes = FiguraVec3.class,
                argumentNames = "pos"
            ),
            @LuaMethodOverload(
                argumentTypes = {Double.class, Double.class, Double.class},
                argumentNames = {"x", "y", "z"}
            )
        },
        value = "world.is_chunk_loaded"
    )
    public static boolean isChunkLoaded(Object x, Double y, Double z) {
        FiguraVec3 pos = LuaUtils.parseVec3("getBlockState", x, y, z);
        BlockPos blockPos = pos.asBlockPos();
        World world = getCurrentWorld();
        return world.isBlockLoaded(blockPos);
    }

    @SuppressWarnings("deprecation")
    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = {FiguraVec3.class, FiguraVec3.class},
                            argumentNames = {"min", "max"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class, FiguraVec3.class},
                            argumentNames = {"minX", "minY", "minZ", "max"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {FiguraVec3.class, Double.class, Double.class, Double.class},
                            argumentNames = {"min", "maxX", "maxY", "maxZ"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class, Double.class, Double.class, Double.class},
                            argumentNames = {"minX", "minY", "minZ", "maxX", "maxY", "maxZ"}
                    )
            },
            value = "world.get_blocks"
    )
    public static List<BlockStateAPI> getBlocks(Object x, Object y, Double z, Double w, Double t, Double h) {
        Pair<FiguraVec3, FiguraVec3> pair = LuaUtils.parse2Vec3("getBlocks", x, y, z, w, t, h, 1);
        List<BlockStateAPI> list = new ArrayList<>();

        BlockPos min = pair.getFirst().asBlockPos();
        BlockPos max = pair.getSecond().asBlockPos();
        max = new BlockPos(
                Math.min(min.getX() + 8, max.getX()),
                Math.min(min.getY() + 8, max.getY()),
                Math.min(min.getZ() + 8, max.getZ())
        );

        World world = getCurrentWorld();
        if (!world.isAreaLoaded(min, max))
            return list;

        BlockPos.getAllInBox(min, max).forEach(blockPos -> {
            BlockPos pos = new BlockPos(blockPos);
            list.add(new BlockStateAPI(world.getBlockState(pos), pos));
        });
        return list;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "pos"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            value = "world.get_redstone_power"
    )
    public static int getRedstonePower(Object x, Double y, Double z) {
        FiguraVec3 pos = LuaUtils.parseVec3("getRedstonePower", x, y, z);
        BlockPos blockPos = pos.asBlockPos();
        if (getCurrentWorld().getChunkFromBlockCoords(blockPos) == null)
            return 0;
        return getCurrentWorld().getStrongPower(blockPos);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "pos"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            value = "world.get_strong_redstone_power"
    )
    public static int getStrongRedstonePower(Object x, Double y, Double z) {
        FiguraVec3 pos = LuaUtils.parseVec3("getStrongRedstonePower", x, y, z);
        BlockPos blockPos = pos.asBlockPos();
        if (getCurrentWorld().getChunkFromBlockCoords(blockPos) == null)
            return 0;
        return getCurrentWorld().getStrongPower(blockPos);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload,
                    @LuaMethodOverload(
                            argumentTypes = Double.class,
                            argumentNames = "delta"
                    )
            },
            value = "world.get_time"
    )
    public static double getTime(double delta) {
        return getCurrentWorld().getTotalWorldTime() + delta;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload,
                    @LuaMethodOverload(
                            argumentTypes = Double.class,
                            argumentNames = "delta"
                    )
            },
            value = "world.get_time_of_day"
    )
    public static double getTimeOfDay(double delta) {
        return getCurrentWorld().getWorldTime() + delta;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload,
            value = "world.get_moon_phase"
    )
    public static int getMoonPhase() {
        return getCurrentWorld().getMoonPhase();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload,
                    @LuaMethodOverload(
                            argumentTypes = Double.class,
                            argumentNames = "delta"
                    )
            },
            value = "world.get_rain_gradient"
    )
    public static double getRainGradient(Float delta) {
        if (delta == null) delta = 1f;
        return getCurrentWorld().getRainStrength(delta);
    }

    @LuaWhitelist
    @LuaMethodDoc("world.is_thundering")
    public static boolean isThundering() {
        return getCurrentWorld().isThundering();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "pos"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            value = "world.get_light_level"
    )
    public static Integer getLightLevel(Object x, Double y, Double z) {
        FiguraVec3 pos = LuaUtils.parseVec3("getLightLevel", x, y, z);
        BlockPos blockPos = pos.asBlockPos();
        World world = getCurrentWorld();
        if (world.getChunkFromBlockCoords(blockPos) == null)
            return null;
        world.calculateInitialSkylight();
        return world.getChunkFromBlockCoords(blockPos).getLightSubtracted(blockPos, world.getSkylightSubtracted());
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "pos"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            value = "world.get_sky_light_level"
    )
    public static Integer getSkyLightLevel(Object x, Double y, Double z) {
        FiguraVec3 pos = LuaUtils.parseVec3("getSkyLightLevel", x, y, z);
        BlockPos blockPos = pos.asBlockPos();
        World world = getCurrentWorld();
        if (world.getChunkFromBlockCoords(blockPos) == null)
            return null;
        return world.getLightFor(EnumSkyBlock.SKY, blockPos);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "pos"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            value = "world.get_block_light_level"
    )
    public static Integer getBlockLightLevel(Object x, Double y, Double z) {
        FiguraVec3 pos = LuaUtils.parseVec3("getBlockLightLevel", x, y, z);
        BlockPos blockPos = pos.asBlockPos();
        World world = getCurrentWorld();
        if (world.getChunkFromBlockCoords(blockPos) == null)
            return null;
        return world.getLightFor(EnumSkyBlock.BLOCK, blockPos);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = {FiguraVec2.class, String.class},
                            argumentNames = {"pos", "heightmap"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, String.class},
                            argumentNames = {"x", "z", "heightmap"}
                    )
            },
            value = "world.get_height"
    )
    public static Integer getHeight(Object x, Double z, String heightmap) {
        FiguraVec2 pos = LuaUtils.parseVec2("getHeight", x, z);
        World world = getCurrentWorld();

        BlockPos blockPos = new BlockPos((int) pos.x(), 0, (int) pos.y());
        if (world.getChunkFromBlockCoords(blockPos) == null)
            return null;

        return world.getHeight((int) pos.x(), (int) pos.y());
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "pos"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            value = "world.is_open_sky"
    )
    public static Boolean isOpenSky(Object x, Double y, Double z) {
        FiguraVec3 pos = LuaUtils.parseVec3("isOpenSky", x, y, z);
        BlockPos blockPos = pos.asBlockPos();
        World world = getCurrentWorld();
        if (world.getChunkFromBlockCoords(blockPos) == null)
            return null;
        return world.canSeeSky(blockPos);
    }

    @LuaWhitelist
    @LuaMethodDoc("world.get_dimension")
    public static String getDimension() {
        World world = getCurrentWorld();
        return world.provider.getDimensionType().getName();
    }

    @LuaWhitelist
    @LuaMethodDoc("world.get_players")
    public static Map<String, EntityAPI<?>> getPlayers() {
        HashMap<String, EntityAPI<?>> playerList = new HashMap<>();
        for (EntityPlayer player : getCurrentWorld().playerEntities)
            playerList.put(player.getName(), PlayerAPI.wrap(player));
        return playerList;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "UUID"
            ),
            value = "world.get_entity"
    )
    public static EntityAPI<?> getEntity(@LuaNotNil String uuid) {
        try {
            return EntityAPI.wrap(EntityUtils.getEntityByUUID(UUID.fromString(uuid)));
        } catch (Exception ignored) {
            throw new LuaError("Invalid UUID");
        }
    }

    @LuaWhitelist
    @LuaMethodDoc("world.avatar_vars")
    public static Map<String, LuaTable> avatarVars() {
        HashMap<String, LuaTable> varList = new HashMap<>();
        for (Avatar avatar : AvatarManager.getLoadedAvatars()) {
            LuaTable tbl = avatar.luaRuntime == null ? new LuaTable() : avatar.luaRuntime.avatar_meta.storedStuff;
            varList.put(avatar.owner.toString(), new ReadOnlyLuaTable(tbl));
        }
        return varList;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = String.class,
                            argumentNames = "block"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {String.class, FiguraVec3.class},
                            argumentNames = {"block", "pos"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {String.class, Double.class, Double.class, Double.class},
                            argumentNames = {"block", "x", "y", "z"}
                    )
            },
            value = "world.new_block"
    )
    public static BlockStateAPI newBlock(@LuaNotNil String string, Object x, Double y, Double z) {
        BlockPos pos = LuaUtils.parseVec3("newBlock", x, y, z).asBlockPos();
        try {
            World level = getCurrentWorld();
            IBlockState block = FiguraFlattenerUtils.stateUnflattinator2000(new ResourceLocation(string));
            return new BlockStateAPI(block, pos);
        } catch (Exception e) {
            throw new LuaError("Could not parse block state from string: " + string);
        }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = String.class,
                            argumentNames = "item"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {String.class, Integer.class},
                            argumentNames = {"item", "count"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {String.class, Integer.class, Integer.class},
                            argumentNames = {"item", "count", "damage"}
                    )
            },
            value = "world.new_item"
    )
    public static ItemStackAPI newItem(@LuaNotNil String string, Integer count, Integer damage) {
        try {
            World level = getCurrentWorld();
            ItemStack item = FiguraFlattenerUtils.theStackUnflattinator2000(new ResourceLocation(string));
            if (count != null)
                item.setCount(count);
            if (damage != null)
                item.setItemDamage(damage);
            return new ItemStackAPI(item);
        } catch (Exception e) {
            throw new LuaError("Could not parse item stack from string: " + string);
        }
    }

    @LuaWhitelist
    @LuaMethodDoc("world.exists")
    public static boolean exists() {
        return getCurrentWorld() != null;
    }

    @LuaWhitelist
    @LuaMethodDoc("world.get_build_height")
    public static int[] getBuildHeight() {
        World world = getCurrentWorld();
        return new int[]{0, world.getHeight()};
    }

    @LuaWhitelist
    @LuaMethodDoc("world.get_spawn_point")
    public static FiguraVec3 getSpawnPoint() {
        World world = getCurrentWorld();
        return FiguraVec3.fromBlockPos(new BlockPos(world.getSpawnPoint().getX(), world.getSpawnPoint().getY(), world.getSpawnPoint().getZ()));
    }

    @Override
    public String toString() {
        return "WorldAPI";
    }
}
