package org.figuramc.figura.utils;

import com.google.common.collect.Maps;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.resources.IResourceManager;
import org.apache.commons.io.IOUtils;
import org.figuramc.figura.FiguraMod;

import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ResourceUtils {
    public static final UUID NIL_UUID = new UUID(0L, 0L);

    public static byte[] getResource(IResourceManager manager, ResourceLocation path) {
        try (InputStream is = manager.getResource(path).getInputStream()) {
            return IOUtils.toByteArray(is);
        } catch (Exception e) {
            FiguraMod.LOGGER.error("", e);
        }
        return null;
    }

    public static <T, U, R> BiFunction<T, U, R> memoize(final BiFunction<T, U, R> biFunction) {
        return new BiFunction<T, U, R>(){
            private final Map<Pair<T, U>, R> cache = Maps.newHashMap();

            @Override
            public R apply(T object, U object2) {
                return this.cache.computeIfAbsent(Pair.of(object, object2), pair -> biFunction.apply(pair.getFirst(), pair.getSecond()));
            }

            public String toString() {
                return "memoize/2[function=" + biFunction + ", size=" + this.cache.size() + "]";
            }
        };
    }

    public static <T, R> Function<T, R> memoize(final Function<T, R> function) {
        return new Function<T, R>(){
            private final Map<T, R> cache = Maps.newHashMap();

            @Override
            public R apply(T object) {
                return this.cache.computeIfAbsent(object, function);
            }

            public String toString() {
                return "memoize/1[function=" + function + ", size=" + this.cache.size() + "]";
            }
        };
    }

    public static String sanitizeName(String fileName, Function<Character, Boolean> characterValidator) {
        return fileName.toLowerCase(Locale.ROOT).chars().mapToObj(i -> characterValidator.apply((char)i) ? Character.toString((char)i) : "_").collect(Collectors.joining());
    }

    public static UUID uuidFromIntArray(int[] bits) {
        return new UUID((long)bits[0] << 32 | (long)bits[1] & 0xFFFFFFFFL, (long)bits[2] << 32 | (long)bits[3] & 0xFFFFFFFFL);
    }

    public static int[] uuidToIntArray(UUID uuid) {
        long l = uuid.getMostSignificantBits();
        long m = uuid.getLeastSignificantBits();
        return leastMostToIntArray(l, m);
    }

    private static int[] leastMostToIntArray(long most, long least) {
        return new int[]{(int)(most >> 32), (int)most, (int)(least >> 32), (int)least};
    }

    public static <T> T make(T object, Consumer<T> consumer) {
        consumer.accept(object);
        return object;
    }
}
