package org.figuramc.figura.entries.forge;

import net.minecraftforge.fml.common.discovery.ASMDataTable;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.entries.EntryPointManager;
import org.figuramc.figura.entries.annotations.FiguraAPIPlugin;
import org.figuramc.figura.entries.annotations.FiguraPermissionsPlugin;
import org.figuramc.figura.entries.annotations.FiguraScreenPlugin;
import org.figuramc.figura.entries.annotations.FiguraVanillaPartPlugin;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EntryPointManagerImpl extends EntryPointManager {
    public static final Map<String, Class<?>> nameToAnnotationClass = new HashMap<>();
    private static ASMDataTable dataTable;
    static {
        nameToAnnotationClass.put("figura_api", FiguraAPIPlugin.class);
        nameToAnnotationClass.put("figura_permissions", FiguraPermissionsPlugin.class);
        nameToAnnotationClass.put("figura_screen", FiguraScreenPlugin.class);
        nameToAnnotationClass.put("figura_vanilla_part", FiguraVanillaPartPlugin.class);
    }

    @Override
    public <T> Set<T> load(String name, Class<T> clazz) {
        Set<T> ret = new HashSet<>();
        Class<?> annotationClass = nameToAnnotationClass.get(name);
        if (annotationClass != null) {
            Set<ASMDataTable.ASMData> data = dataTable.getAll(annotationClass.getCanonicalName());
            for (ASMDataTable.ASMData asmData : data) {
                try {
                    Class<?> asmClass = Class.forName(asmData.getClassName());
                    Class<? extends T> asmInstanceClass = asmClass.asSubclass(clazz);
                    Constructor<? extends T> constructor = asmInstanceClass.getDeclaredConstructor();
                    T instance = constructor.newInstance();
                    ret.add(instance);
                } catch (ReflectiveOperationException | LinkageError e) {
                    FiguraMod.LOGGER.error("Failed to load entrypoint: {}", asmData.getClassName(), e);
                }
            }
        }
        return ret;
    }

    public void setASMDataTable(ASMDataTable table) {
        dataTable = table;
    }
}
