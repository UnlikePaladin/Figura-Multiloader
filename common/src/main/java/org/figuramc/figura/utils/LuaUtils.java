package org.figuramc.figura.utils;

import com.google.gson.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import org.figuramc.figura.lua.api.json.FiguraJsonSerializer;
import org.figuramc.figura.lua.api.world.BlockStateAPI;
import org.figuramc.figura.lua.api.world.ItemStackAPI;
import org.figuramc.figura.lua.api.world.WorldAPI;
import org.figuramc.figura.math.vector.FiguraVec2;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.math.vector.FiguraVec4;
import org.figuramc.figura.math.vector.FiguraVector;
import org.figuramc.figura.mixin.CommandReplaceItemAccessor;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.ArrayList;
import java.util.Arrays;

public class LuaUtils {

    /**
     * This is a generic vector parsing function that also parses the arguments after the vectors, allowing vectors to be at the beginning of the function signature
     * @param methodName The name of the function that is calling this function. Used for readable errors.
     * @param vectorSizes The sizes of Vectors to parse. The number of vectors is determined by the size of the array.
     * @param defaultValues When a Vector or a Vector argument is nil, it will be filled in with the value in this array at the correct index.
     * @param expectedReturns An array of Classes for what the extra arguments are supposed to be. Used for readable errors.
     * @param args The arguments of the function, passed in as varargs. 
     * @return The new args list with multi-number-argument Vectors being returned as real Vectors.
     */
    public static Object[] parseVec(String methodName, int[] vectorSizes, double[] defaultValues, Class<?>[] expectedReturns, Object ...args) {
        ArrayList<Object> ret = new ArrayList<Object>(args.length);
        int i=0;
        for(int size : vectorSizes) {
            if (args[i] instanceof FiguraVector){
                FiguraVector vec = (FiguraVector) args[i];
                if(vec.size()!=size)
                    throw new LuaError("Illegal argument at position " + (i + 1) + " to " + methodName + "(): Expected Vector" + size + ", recieved Vector" + vec.size());
                ret.add(vec);
                i += 1;
            }
            else if (args[i]==null || args[i] instanceof Number) {
                double[] vec = new double[size];
                for (int o=0;o<size;o++){
                    if (args[i + o] instanceof Number) {
                        Number n = (Number) args[i + o];
                        vec[o]=n.doubleValue();
                    } else if(args[i + o] == null)
                        vec[o]=defaultValues[o];
                    else
                        throw new LuaError("Illegal argument at position " + (i + o + 1) + " to " + methodName + "():" + 
                            " Expected Number, recieved " + args[i+o].getClass().getSimpleName() + " (" + args[i+o] + ")"
                        );
                }
                switch (size) {
                    case 2:
                        ret.add(FiguraVec2.of(vec[0], vec[1]));
                        break;
                    case 3:
                        ret.add(FiguraVec3.of(vec[0], vec[1], vec[2]));
                        break;
                    case 4:
                        ret.add(FiguraVec4.of(vec[0], vec[1], vec[2], vec[3]));
                        break;
                    default:
                        throw new IllegalArgumentException("Illegal vector size: " + size);
                }
                i += size;
            }
            else if(args[i]==null) {
                switch (size) {
                    case 2:
                        ret.add(FiguraVec2.of(defaultValues[0], defaultValues[1]));
                        break;
                    case 3:
                        ret.add(FiguraVec3.of(defaultValues[0], defaultValues[1], defaultValues[2]));
                        break;
                    case 4:
                        ret.add(FiguraVec4.of(defaultValues[0], defaultValues[1], defaultValues[2], defaultValues[3]));
                        break;
                    default:
                        throw new IllegalArgumentException("Illegal vector size: " + size);
                }
                i += 1;
            }
            else
                throw new LuaError("Illegal argument at position " + (i + 1) + " to " + methodName + "():" + 
                    " Expected Vector" + size + " or Number, recieved " + args[i].getClass().getSimpleName() + " (" + args[i] + ")"
                );
        }
        for(int o = i; o < args.length; o++) {
            if(args[o] != null && (o-i) < expectedReturns.length && !expectedReturns[o-i].isAssignableFrom(args[o].getClass()))
                throw new LuaError("Illegal argument at position " + (o + 1) + " to " + methodName + "():" + 
                    " Expected " + expectedReturns[o-i].getSimpleName() + ", recieved " + args[o].getClass().getSimpleName() + " (" + args[o] + ")"
                );
            ret.add(args[o]);
        }
        return ret.toArray();
    }

    public static Object[] parseVec(String methodName, int[] vectorSizes, Class<?>[] expectedReturns, Object ...args) {
        return parseVec(methodName, vectorSizes, new double[]{0,0,0,0}, expectedReturns, args);
    }

    public static FiguraVec2 parseVec2(String methodName, Object x, Number y) {
        return parseVec2(methodName, x, y, 0, 0);
    }

    public static FiguraVec2 parseVec2(String methodName, Object x, Number y, double defaultX, double defaultY) {
        if (x instanceof FiguraVec2) {
            FiguraVec2 vec = (FiguraVec2) x;
            return vec.copy();
        }
        if (x == null || x instanceof Number) {
            if (x == null) x = defaultX;
            if (y == null) y = defaultY;
            return FiguraVec2.of(((Number) x).doubleValue(), y.doubleValue());
        }
        throw new LuaError("Illegal argument to " + methodName + "(): " + x.getClass().getSimpleName());
    }

    /**
     * This code gets repeated SO MUCH that I decided to put it in the utils class.
     * @param x Either the x coordinate of a vector, or a vector itself.
     * @param y The y coordinate of a vector, used if the first parameter was a number.
     * @param z The z coordinate of a vector, used if the first parameter was a number.
     * @return A FiguraVec3 representing the data passed in.
     */
    public static FiguraVec3 parseVec3(String methodName, Object x, Number y, Number z) {
        return parseVec3(methodName, x, y, z, 0, 0, 0);
    }

    public static FiguraVec3 parseVec3(String methodName, Object x, Number y, Number z, double defaultX, double defaultY, double defaultZ) {
        if (x instanceof FiguraVec3) {
            FiguraVec3 vec = (FiguraVec3) x;
            return vec.copy();
        }
        if (x == null || x instanceof Number) {
            if (x == null) x = defaultX;
            if (y == null) y = defaultY;
            if (z == null) z = defaultZ;
            return FiguraVec3.of(((Number) x).doubleValue(), y.doubleValue(), z.doubleValue());
        }
        throw new LuaError("Illegal argument to " + methodName + "(): " + x.getClass().getSimpleName());
    }

    public static FiguraVec3 parseOneArgVec(String methodName, Object x, Number y, Number z, double defaultArg) {
        double d = x instanceof Number ? ((Number) x).doubleValue() : defaultArg;
        return parseVec3(methodName, x, y, z, d, d, d);
    }

    public static FiguraVec3 nullableVec3(String methodName, Object x, Number y, Number z) {
        return x == null ? null : parseVec3(methodName, x, y, z);
    }

    public static Pair<FiguraVec3, FiguraVec3> parse2Vec3(String methodName, Object x, Object y, Number z, Object w, Number t, Number h, int xIndex) {
        FiguraVec3 a, b;

        if (x instanceof FiguraVec3) {
            FiguraVec3 vec1 = (FiguraVec3) x;
            a = vec1.copy();
            if (y instanceof FiguraVec3) {
                FiguraVec3 vec2 = (FiguraVec3) y;
                b = vec2.copy();
            } else if (y == null || y instanceof Number) {
                if (w == null || w instanceof Number) {
                    b = parseVec3(methodName, y, z, (Number) w);
                } else {
                    throw new LuaError("Illegal argument at position" + xIndex+3 + "to " + methodName + "(): " + w);
                }
            } else {
                throw new LuaError("Illegal argument at position "+ xIndex+1 + " to " + methodName + "(): " + y);
            }
        } else if (x instanceof Number && y == null || y instanceof Number) {
            a = parseVec3(methodName, x, (Number) y, z);
            if (w instanceof FiguraVec3) {
                FiguraVec3 vec1 = (FiguraVec3) w;
                b = vec1.copy();
            } else if (w == null || w instanceof Number) {
                b = parseVec3(methodName, w, t, h);
            } else {
                throw new LuaError("Illegal argument at position "+ xIndex+3 + " to " + methodName + "(): " + w);
            }
        } else {
            throw new LuaError("Illegal argument at position "+ xIndex + " to " + methodName + "(): " + x);
        }

        return Pair.of(a, b);
    }

    // These functions allow having vector parsing at the beggining of the function, taking into account other arguments.
    public static Pair<FiguraVec3, Object[]> parseVec3(String methodName, Class<?>[] expectedReturns, Object ...args) {
        Object[] parsed = parseVec(methodName, new int[]{3}, expectedReturns, args);
        return Pair.of((FiguraVec3)parsed[0], Arrays.copyOfRange(parsed, 1, parsed.length));
    }

    public static Pair<Pair<FiguraVec3, FiguraVec3>, Object[]> parse2Vec3(String methodName, Class<?>[] expectedReturns, Object ...args) {
        Object[] parsed = parseVec(methodName, new int[]{3,3}, expectedReturns, args);
        return Pair.of(
            Pair.of((FiguraVec3)parsed[0], (FiguraVec3)parsed[1]), 
            Arrays.copyOfRange(parsed, 2, parsed.length)
        );
    }

    public static FiguraVec4 parseVec4(String methodName, Object x, Number y, Number z, Number w, double defaultX, double defaultY, double defaultZ, double defaultW) {
        if (x instanceof FiguraVec3) {
            FiguraVec3 vec = (FiguraVec3) x;
            return FiguraVec4.of(vec.x, vec.y, vec.z, defaultW);
        }
        if (x instanceof FiguraVec4) {
            FiguraVec4 vec = (FiguraVec4) x;
            return vec.copy();
        }
        if (x == null || x instanceof Number) {
            if (x == null) x = defaultX;
            if (y == null) y = defaultY;
            if (z == null) z = defaultZ;
            if (w == null) w = defaultW;
            return FiguraVec4.of(((Number) x).doubleValue(), y.doubleValue(), z.doubleValue(), w.doubleValue());
        }
        throw new LuaError("Illegal argument to " + methodName + "(): " + x.getClass().getSimpleName());
    }

    public static ItemStack parseItemStack(String methodName, Object item) {
        if (item == null)
            return ItemStack.EMPTY;
        else if (item instanceof ItemStackAPI) {
            ItemStackAPI wrapper = (ItemStackAPI) item;
            return wrapper.itemStack;
        } else if (item instanceof String) {
            String string = (String) item;
            try {
                World level = WorldAPI.getCurrentWorld();
                String[] strings = string.split(" ");
                Item itemOb = CommandBase.getItemByText(null, strings[0]);
                // TODO implement meta convertion, for blocks and items
                return new ItemStack(itemOb, 1);
            } catch (Exception e) {
                throw new LuaError("Could not parse item stack from string: " + string);
            }
        }

        throw new LuaError("Illegal argument to " + methodName + "(): " + item);
    }

    public static IBlockState parseBlockState(String methodName, Object block) {
        if (block == null)
            return Blocks.AIR.getDefaultState();
        else if (block instanceof BlockStateAPI) {
            BlockStateAPI wrapper = (BlockStateAPI) block;
            return wrapper.blockState;
        } else if (block instanceof String) {
            String string = (String) block;
            try {
                World level = WorldAPI.getCurrentWorld();
                //TODO: Fix this, i need to check how the strings are split
                String[] strings = string.split(" ");
                return FiguraFlattenerUtils.stateUnflattinator2000(new ResourceLocation(strings[0]));
            } catch (Exception e) {
                throw new LuaError("Could not parse block state from string: " + string);
            }
        }

        throw new LuaError("Illegal argument to " + methodName + "(): " + block);
    }

    public static ResourceLocation parsePath(String path) {
        try {
            return new ResourceLocation(path);
        } catch (Exception e) {
            throw new LuaError(e.getMessage());
        }
    }

    public static Object[] parseBlockHitResult(RayTraceResult hitResult) {
        if (hitResult != null && hitResult.typeOfHit == RayTraceResult.Type.BLOCK) {
            BlockPos pos = hitResult.getBlockPos();
            return new Object[]{new BlockStateAPI(WorldAPI.getCurrentWorld().getBlockState(pos), pos), FiguraVec3.fromVec3(hitResult.hitVec), hitResult.sideHit.getName()};
        }
        return null;
    }

    public static int parseSlot(Object slot, InventoryPlayer inventory) {
        if (slot instanceof String) {
            String s = (String) slot;
            try {
                if (!CommandReplaceItemAccessor.figura$getShortcuts().containsKey(s)) {
                    throw new CommandException("commands.generic.parameter.invalid", s);
                } else {
                    return CommandReplaceItemAccessor.figura$getShortcuts().get(s);
                }
            } catch (Exception e) {
                throw new LuaError("Unable to get slot \"" + slot + "\"");
            }
        } else if (slot instanceof Integer) {
            int i = (Integer) slot;
            if (i == -1 && inventory != null) {
                for (int index = 0; index < inventory.mainInventory.size(); ++index) {
                    if (!inventory.mainInventory.get(index).isEmpty()) continue;
                    return index;
                }
                return -1;
            } else {
                return i;
            }
        } else {
            throw new LuaError("Invalid type for getSlot: " + slot.getClass().getSimpleName());
        }
    }

    public static JsonElement asJsonValue(LuaValue value) {
        if (value.isnil()) return JsonNull.INSTANCE;
        if (value.isboolean()) return new JsonPrimitive(value.checkboolean());
        if (value instanceof LuaString) {
            LuaString s = (LuaString) value;
            return new JsonPrimitive(s.checkjstring());
        }
        if (value.isint()) return new JsonPrimitive(value.checkint());
        if (value.isnumber()) return new JsonPrimitive(value.checkdouble());
        if (value.istable()) {
            LuaTable table = value.checktable();

            // If it's an "array" (uses numbers as keys)
            if (checkTableArray(table)) {
                JsonArray arr = new JsonArray();
                LuaValue[] keys = table.keys();
                int arrayLength = keys[keys.length-1].checkint();
                for(int i = 1; i <= arrayLength; i++) {
                    arr.add(asJsonValue(table.get(i)));
                }
                return arr;
            }
            // Otherwise, if it's a proper key-value table
            else {
                JsonObject object = new JsonObject();
                for (LuaValue key : table.keys()) {
                    object.add(key.tojstring(), asJsonValue(table.get(key)));
                }
                return object;
            }
        }
        if (value.isuserdata() && value.checkuserdata() instanceof FiguraJsonSerializer.JsonValue) {
            FiguraJsonSerializer.JsonValue val = (FiguraJsonSerializer.JsonValue) value.checkuserdata();
            return val.getElement();
        }
        // Fallback for things that shouldn't be converted (like functions)
        return null;
    }

    public static boolean checkTableArray(LuaTable table) {
        for (LuaValue key : table.keys()) {
            if (!key.isnumber()) return false;
        }

        return true;
    }
}
