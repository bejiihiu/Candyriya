package kz.bejiihiu.candyriya.boot.neoforge.mod;

import cpw.mods.jarhandling.impl.JarContentsImpl;
import kz.bejiihiu.candyriya.api.Unsafe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Filter out packages already provided by Candyriya.
 * For duplicate modules, the packages will be removed
 * before modules are removed.
 * For duplicate packages in non-duplicate library modules,
 * only the packages will be removed.
 * For duplicate packages in non-duplicate mod modules,
 * the packages won't be removed
 * Duplicated shaded mods are like modules, and are removed
 * later by JarInJarFilter.
 */
public class CandyriyaJarContentsImplFilter {
    // Use unsafe to bypass JPMS accessibility check
    private static final MethodHandles.Lookup LOOKUP = Unsafe.lookup();
    private static VarHandle PACKAGES;
    private static Set<String> serviceLayerPackages;
    private static final Logger LOGGER = LogManager.getLogger("Candyriya");

    static {
        try {
            PACKAGES = LOOKUP.findVarHandle(JarContentsImpl.class, "packages", Set.class);
        } catch (ReflectiveOperationException e) {
            LOGGER.error("Candyriya failed to filter JarContents. This may cause dependency conflicts with some mods!", e);
        }
        serviceLayerPackages = CandyriyaJarContentsImplFilter.class
                .getModule()
                .getLayer()
                .modules()
                .stream()
                .flatMap(it -> it.getPackages().stream())
                .collect(Collectors.toSet());
    }

    /*
     * The result of getPackages() is cached
     * Through modifying the cache, we modify the result of getPackages()
     * Note: ModJarMetadata use getPackagesExcluding(String...),
     * which bypass the cache. This won't work for ModJarMetadata.
     */
    public static void filter(JarContentsImpl impl) {
        if (PACKAGES != null) {
            impl.getPackages();
            Set<String> raw = (Set<String>)PACKAGES.get(impl);
            Set<String> result = raw.stream()
                    .filter(CandyriyaJarContentsImplFilter::test)
                    .collect(Collectors.toSet());
            PACKAGES.set(impl, result);
        }
    }

    public static boolean test(String pkg) {
        return !serviceLayerPackages.contains(pkg);
    }
}
