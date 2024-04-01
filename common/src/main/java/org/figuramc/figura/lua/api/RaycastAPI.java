package org.figuramc.figura.lua.api;

import com.google.common.base.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.ducks.ArmorStandAccessor;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.api.entity.EntityAPI;
import org.figuramc.figura.lua.api.world.WorldAPI;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaMethodOverload;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.utils.LuaUtils;
import org.figuramc.figura.utils.Pair;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.ArrayList;
import java.util.Optional;

@LuaWhitelist
@LuaTypeDoc(
        name = "RaycastAPI",
        value = "raycast"
)
public class RaycastAPI {
    
    private final Avatar owner;

    public RaycastAPI(Avatar owner) {
        this.owner = owner;
    }

    
    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = {FiguraVec3.class, FiguraVec3.class, String.class, String.class},
                            argumentNames = {"start", "end", "blockCastType", "fluidCastType"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class, FiguraVec3.class, String.class, String.class},
                            argumentNames = {"startX", "startY", "startZ", "end", "blockCastType", "fluidCastType"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {FiguraVec3.class, Double.class, Double.class, Double.class, String.class, String.class},
                            argumentNames = {"start", "endX", "endY", "endZ", "blockCastType", "fluidCastType"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, String.class, String.class},
                            argumentNames = {"startX", "startY", "startZ", "endX", "endY", "endZ", "blockCastType", "fluidCastType"}
                    )
                }
            ,
            value = "raycast.block"
    )
    public Object[] block(Object x, Object y, Object z, Object w, Object t, Object h, String blockCastType, String fluidCastType) {
        FiguraVec3 start, end;
        Pair<Pair<FiguraVec3, FiguraVec3>, Object[]> parseResult = LuaUtils.parse2Vec3(
            "block", 
            new Class<?>[]{String.class, String.class}, 
            x, y, z, w, t, h, blockCastType, fluidCastType
        );

        start = parseResult.getFirst().getFirst();
        end = parseResult.getFirst().getSecond();

        blockCastType = (String)parseResult.getSecond()[0];
        fluidCastType = (String)parseResult.getSecond()[1];

        boolean ignoreBlockWithoutBoundingBox;
        try{
            ignoreBlockWithoutBoundingBox = blockCastType != null && blockCastType.contains("collider");
        }
        catch(IllegalArgumentException e){
            throw new LuaError("Invalid blockRaycastType provided");
        }

        boolean stopOnFluid;
        try{
            stopOnFluid = fluidCastType != null && !fluidCastType.contains("none");
        }
        catch(IllegalArgumentException e){
            throw new LuaError("Invalid fluidRaycastType provided");
        }

        // TODO : check if this works, need to see if returnlastuncollidable works
        RayTraceResult result = WorldAPI.getCurrentWorld().rayTraceBlocks(start.asVec3(), end.asVec3(), ignoreBlockWithoutBoundingBox, stopOnFluid, false);
        return LuaUtils.parseBlockHitResult(result);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = {FiguraVec3.class, FiguraVec3.class, LuaFunction.class},
                            argumentNames = {"start", "end", "predicate"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class, FiguraVec3.class, LuaFunction.class},
                            argumentNames = {"startX", "startY", "startZ", "end", "predicate"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {FiguraVec3.class, Double.class, Double.class, Double.class, LuaFunction.class},
                            argumentNames = {"start", "endX", "endY", "endZ", "predicate"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, LuaFunction.class},
                            argumentNames = {"startX", "startY", "startZ", "endX", "endY", "endZ", "predicate"}
                    )
            }
            ,
            value = "raycast.entity"
    )
    public Object[] entity(Object x, Object y, Object z, Object w, Object t, Double h, LuaFunction predicate) {
        FiguraVec3 start, end;

        Pair<Pair<FiguraVec3, FiguraVec3>, Object[]> pair = LuaUtils.parse2Vec3(
            "entity", 
            new Class<?>[]{LuaFunction.class},
            x, y, z, w, t, h, predicate);

        start = pair.getFirst().getFirst();
        end = pair.getFirst().getSecond();

        final LuaFunction fn = (LuaFunction)pair.getSecond()[0];

        Predicate<Entity> entityPredicate = (entity) -> {
            if (fn == null) return true;
            LuaValue result = fn.invoke(this.owner.luaRuntime.typeManager.javaToLua(EntityAPI.wrap(entity))).arg1();
            if ((result.isboolean() && result.checkboolean() == false) || result.isnil())
                return false;
            return true;
        };
        World world = WorldAPI.getCurrentWorld();
        EntityArmorStand marker = new EntityArmorStand(WorldAPI.getCurrentWorld());
        ((ArmorStandAccessor)marker).figura$setMarker(true);

        double distance = Double.MAX_VALUE;
        Entity entityHit = null;
        Vec3d posHit = null;
        for (Entity currentEntity : world.getEntitiesInAABBexcluding(marker, new AxisAlignedBB(start.asVec3(), end.asVec3()), entityPredicate)) {
            AxisAlignedBB aABB = currentEntity.getEntityBoundingBox().grow(currentEntity.getCollisionBorderSize());
            RayTraceResult rayTraceResult = aABB.calculateIntercept(start.asVec3(), end.asVec3());
            if (aABB.contains(start.asVec3())) {
                if (distance >= 0.0) {
                    entityHit = currentEntity;
                    posHit = rayTraceResult == null ? start.asVec3() : rayTraceResult.hitVec;
                    distance = 0.0;
                }
            } else if (rayTraceResult != null) {
                double eyeDistanceToHit = start.asVec3().distanceTo(new Vec3d(rayTraceResult.getBlockPos()));
                if (eyeDistanceToHit < distance || distance == 0.0) {
                    if (currentEntity.getLowestRidingEntity() == marker.getLowestRidingEntity()) {
                        if (distance == 0.0) {
                            entityHit = currentEntity;
                            posHit = rayTraceResult.hitVec;
                        }
                    } else {
                        entityHit = currentEntity;
                        posHit = rayTraceResult.hitVec;
                        distance = eyeDistanceToHit;
                    }
                }
            }
        }

        if (entityHit != null && posHit != null)
            return new Object[]{EntityAPI.wrap(entityHit), FiguraVec3.fromVec3(posHit)};

        return null;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = {FiguraVec3.class, FiguraVec3.class, LuaTable.class},
                            argumentNames = {"start", "end", "aabbs"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class, FiguraVec3.class, LuaTable.class},
                            argumentNames = {"startX", "startY", "startZ", "end", "aabbs"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {FiguraVec3.class, Double.class, Double.class, Double.class, LuaTable.class},
                            argumentNames = {"start", "endX", "endY", "endZ", "aabbs"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, LuaTable.class},
                            argumentNames = {"startX", "startY", "startZ", "endX", "endY", "endZ", "aabbs"}
                    )
            }
            ,
            value = "raycast.aabb"
    )
    public Object[] aabb(Object x, Object y, Object z, Object w, Object t, Object h, LuaTable aabbs) {
        Vec3d start, end;

        Pair<Pair<FiguraVec3, FiguraVec3>, Object[]> pair = LuaUtils.parse2Vec3(
            "aabb", 
            new Class<?>[]{LuaTable.class},
            x, y, z, w, t, h, aabbs
        );

        start = pair.getFirst().getFirst().asVec3();
        end = pair.getFirst().getSecond().asVec3();

        aabbs = (LuaTable)pair.getSecond()[0];
        if (aabbs == null)
            throw new LuaError("Illegal argument to aabb(): Expected LuaTable, recieved nil");
        
        ArrayList<AxisAlignedBB> aabbList = new ArrayList<AxisAlignedBB>();
        for (int i=1;i<=aabbs.length();i++){
            LuaValue arg = aabbs.get(i);
            if (!arg.istable())
                throw new LuaError("Illegal argument at array index " + i + ": Expected table, recieved " + arg.typename() + " ("+arg.toString()+")");

            LuaValue min = arg.get(1);
            if (!min.isuserdata(FiguraVec3.class))
                throw new LuaError("Illegal argument to AABB at array index "+ i +" at index 1: Expected Vector3, recieved " + min.typename() + " ("+min.toString()+")");

            LuaValue max = arg.get(2);
            if (!max.isuserdata(FiguraVec3.class))
                throw new LuaError("Illegal argument to AABB at array index "+ i +" at index 2: Expected Vector3, recieved " + max.typename() + " ("+max.toString()+")");

            aabbList.add(new AxisAlignedBB(
                ((FiguraVec3)min.checkuserdata(FiguraVec3.class)).asVec3(), 
                ((FiguraVec3)max.checkuserdata(FiguraVec3.class)).asVec3()
            ));
        }

        // Modified from ProjectileUtil.getEntityHitResult to utilize arbitrary AABBs, and the custom clipAABB function
        // I was unable to figure out how the BlockState clipping worked, which would have been better.
        {
            double d = Double.MAX_VALUE;
            int index = -1;
            Pair<Vec3d, EnumFacing> result = null;
            
            for(int i = 0; i < aabbList.size(); i++) {
                AxisAlignedBB box = aabbList.get(i);
                Optional<Pair<Vec3d, EnumFacing>> optional = clipAABB(box, start, end);
                if (box.contains(start)) {
                    if (d >= 0.0) {
                        index = i+1;
                        result = optional.orElse(Pair.of(start, null));
                        d = 0.0;
                    }
                } else if (optional.isPresent()) {
                    Vec3d position = optional.get().getFirst();
                    double e = start.squareDistanceTo(position);
                    if (e < d || d == 0.0) {
                        index = i+1;
                        result = optional.get();
                        d = e;
                    }
                }
            }

            if (index == -1) {
                return null;
            }

            return new Object[]{
                aabbs.get(index), 
                FiguraVec3.fromVec3(result.getFirst()), 
                result.getSecond()!=null ? result.getSecond().getName() : null,
                index
            };
        }
    }

    // Modified from AABB.clip(Vec3 min, Vec3 max) to also return the side hit
    public Optional<Pair<Vec3d, EnumFacing>> clipAABB(AxisAlignedBB aabb, Vec3d min, Vec3d max) {
        double[] ds = new double[]{1.0};
        double d = max.x - min.x;
        double e = max.y - min.y;
        double f = max.z - min.z;
        RayTraceResult result = aabb.calculateIntercept(min, max);
        if (result == null || result.sideHit == null) {
           return Optional.empty();
        } else {
           double g = ds[0];
           return Optional.of(Pair.of(min.addVector(g * d, g * e, g * f), result.sideHit));
        }
     }
}
