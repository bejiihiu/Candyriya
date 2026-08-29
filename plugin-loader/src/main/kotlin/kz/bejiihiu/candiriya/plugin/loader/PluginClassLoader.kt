package kz.bejiihiu.candiriya.plugin.loader

import java.net.URL
import java.net.URLClassLoader

/**
 * Hybrid child-first ClassLoader.
 *
 * - `sharedPrefixes` (e.g. `kotlin.`, `net.kyori.`, `com.google.common.`) are always parent-first
 *   so all plugins share one copy of adventure/guava/kotlin-stdlib and don't OOM.
 * - everything else is child-first — plugin's own classes + deps win.
 * - `isolated=false` mode is handled by `PluginManager` — it reuses one loader for several jars,
 *   here each instance still behaves hybrid.
 *
 * Why hybrid: pure child-first breaks `kotlin.Metadata` + adventure singleton hell,
 * pure parent-first breaks dependency conflicts (two plugins with different okhttp).
 */
public class PluginClassLoader(
    urls: Array<URL>,
    parent: ClassLoader,
    private val sharedPrefixes: List<String> = DEFAULT_SHARED
) : URLClassLoader(urls, parent) {

    public companion object {
        // api stays shared so all plugins see same Adventure/singletons
        public val DEFAULT_SHARED: List<String> = listOf(
            "kotlin.",
            "kotlinx.",
            "net.kyori.",
            "com.google.common.",
            "org.apache.logging.log4j.",
            "org.slf4j.",
            "kz.bejiihiu.candiriya.plugin.",
            "java.",
            "jdk.",
            "sun."
        )
    }

    @Throws(ClassNotFoundException::class)
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        synchronized(getClassLoadingLock(name)) {
            // 1) already loaded?
            findLoadedClass(name)?.let {
                if (resolve) resolveClass(it)
                return it
            }
            val isShared = sharedPrefixes.any { name.startsWith(it) }
            if (isShared) {
                // parent-first for shared
                try {
                    val c = parent.loadClass(name)
                    if (resolve) resolveClass(c)
                    return c
                } catch (_: ClassNotFoundException) {
                    // fallback to self
                }
            }
            // child-first: try self
            try {
                val c = findClass(name)
                if (resolve) resolveClass(c)
                return c
            } catch (_: ClassNotFoundException) {
                // not in plugin jar
            }
            // delegate to parent last (or if shared already tried and failed)
            val c = parent.loadClass(name)
            if (resolve) resolveClass(c)
            return c
        }
    }
}
