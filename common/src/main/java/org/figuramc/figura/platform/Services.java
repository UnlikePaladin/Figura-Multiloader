package org.figuramc.figura.platform;

import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.entries.EntryPointManager;
import org.figuramc.figura.utils.FiguraModMetadata;
import org.figuramc.figura.utils.FiguraResourceListener;

import java.util.ServiceLoader;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

// Service loaders are a built-in Java feature that allow us to locate implementations of an interface that vary from one
// environment to another. In the context of MultiLoader we use this feature to access a mock API in the common code that
// is swapped out for the platform specific implementation at runtime.
public class Services {
    public static final FiguraModMetadata FIGURA_MOD_METADATA = load(FiguraModMetadata.class);
    public static final FiguraResourceListener FIGURA_RESOURCE_LISTENER = load(FiguraResourceListener.class);
    public static final EntryPointManager ENTRYPOINT_MANAGER = load(EntryPointManager.class);

    // This code is used to load a service for the current environment. Your implementation of the service must be defined
    // manually by including a text file in META-INF/services named with the fully qualified class name of the service.
    // Inside the file you should write the fully qualified class name of the implementation to load for the platform. For
    // example our file on Forge points to ForgePlatformHelper while Fabric points to FabricPlatformHelper.
    public static <T> T load(Class<T> clazz) {

        T loadedService;
        try (Stream<T> services = StreamSupport.stream(ServiceLoader.load(clazz).spliterator(), false)) {
            loadedService = services.findFirst()
                    .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        }

        FiguraMod.LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}