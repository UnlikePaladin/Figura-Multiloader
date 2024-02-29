package org.figuramc.figura.lua;

import net.minecraft.nbt.*;
import org.figuramc.figura.mixin.NBTTagLongArrayAccessor;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.HashMap;
import java.util.function.Function;

public class NbtToLua {

    private static final HashMap<Class<?>, Function<NBTBase, LuaValue>> CONVERTERS = new HashMap<Class<?>, Function<NBTBase, LuaValue>>() {{
        // primitive types
        put(NBTTagByte.class, tag -> LuaValue.valueOf(((NBTTagByte) tag).getByte()));
        put(NBTTagShort.class, tag -> LuaValue.valueOf(((NBTTagShort) tag).getShort()));
        put(NBTTagInt.class, tag -> LuaValue.valueOf(((NBTTagInt) tag).getInt()));
        put(NBTTagLong.class, tag -> LuaValue.valueOf(((NBTTagLong) tag).getLong()));
        put(NBTTagFloat.class, tag -> LuaValue.valueOf(((NBTTagFloat) tag).getFloat()));
        put(NBTTagDouble.class, tag -> LuaValue.valueOf(((NBTTagDouble) tag).getDouble()));

        // compound special :D
        put(NBTTagCompound.class, tag -> {
            LuaTable table = new LuaTable();
            NBTTagCompound compound = (NBTTagCompound) tag;

            for (String key : compound.getKeySet())
                table.set(key, convert(compound.getTag(key)));

            return table;
        });

        // collection types
        put(NBTTagByteArray.class, tag -> fromCollectionByte((NBTTagByteArray) tag));
        put(NBTTagIntArray.class, tag -> fromCollectionInt((NBTTagIntArray) tag));
        put(NBTTagLongArray.class, tag -> fromCollectionLong((NBTTagLongArray) tag));
        put(NBTTagList.class, tag -> fromCollectionList((NBTTagList) tag));
    }};

    private static LuaValue fromCollectionByte(NBTTagByteArray tag) {
        LuaTable table = new LuaTable();

        int i = 1;
        for (byte children : tag.getByteArray()) {
            table.set(i, LuaValue.valueOf(children));
            i++;
        }

        return table;
    }

    private static LuaValue fromCollectionInt(NBTTagIntArray tag) {
        LuaTable table = new LuaTable();

        int i = 1;
        for (int children : tag.getIntArray()) {
            table.set(i, LuaValue.valueOf(children));
            i++;
        }

        return table;
    }

    private static LuaValue fromCollectionLong(NBTTagLongArray tag) {
        LuaTable table = new LuaTable();

        int i = 1;
        for (long children : ((NBTTagLongArrayAccessor)tag).getData()) {
            table.set(i, LuaValue.valueOf(children));
            i++;
        }

        return table;
    }

    private static LuaValue fromCollectionList(NBTTagList tag) {
        LuaTable table = new LuaTable();

        int i = 1;
        for (int j = 0; j < tag.tagCount(); j++){
            table.set(i, convert(tag.get(j)));
            i++;
        }

        return table;
    }

    public static LuaValue convert(NBTBase tag) {
        if (tag == null)
            return null;

        Class<?> clazz = tag.getClass();
        Function<NBTBase, LuaValue> builder = CONVERTERS.get(clazz);
        if (builder == null)
            return LuaValue.valueOf(tag.toString());

        return builder.apply(tag);
    }
}
