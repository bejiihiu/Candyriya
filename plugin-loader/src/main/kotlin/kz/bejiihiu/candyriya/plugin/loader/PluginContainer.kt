package kz.bejiihiu.candyriya.plugin.loader

import java.nio.file.Path
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kz.bejiihiu.candyriya.plugin.DefaultPluginScheduler
import kz.bejiihiu.candyriya.plugin.Plugin
import kz.bejiihiu.candyriya.plugin.PluginDescription
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

/**
 * Holds a loaded plugin instance + its isolated resources.
 * Each plugin gets its own virtual-thread executor (per-plugin thread model).
 */
public class PluginContainer(
    public val description: PluginDescription,
    public val instance: Plugin,
    public val classLoader: PluginClassLoader,
    public val jarPath: Path,
    public val dataDirectory: Path,
    public val executor: ExecutorService,
    public val scheduler: DefaultPluginScheduler,
    public val logger: Logger = LogManager.getLogger("candiriya-plugin-${description.id}")
) : AutoCloseable {

    public enum class State { LOADED, ENABLED, DISABLED, FAILED }

    @Volatile
    public var state: State = State.LOADED
        private set

    /**
     * Creates a single-thread executor for this plugin.
     * Uses virtual threads when [useVirtual] is true (Java 21+), otherwise platform thread.
     */
    public companion object {
        public fun createExecutor(pluginId: String, useVirtual: Boolean): ExecutorService {
            return if (useVirtual) {
                try {
                    val factory = Thread.ofVirtual().name("candiriya-plugin-$pluginId-", 0).factory()
                    Executors.newThreadPerTaskExecutor(factory)
                } catch (_: Exception) {
                    // fallback — shouldn't happen on Java 21
                    Executors.newSingleThreadExecutor { r ->
                        Thread(r, "candiriya-plugin-$pluginId").apply { isDaemon = true }
                    }
                }
            } else {
                Executors.newSingleThreadExecutor { r ->
                    Thread(r, "candiriya-plugin-$pluginId").apply { isDaemon = true }
                }
            }
        }
    }

    public fun markEnabled() {
        state = State.ENABLED
    }

    public fun markDisabled() {
        state = State.DISABLED
    }

    public fun markFailed() {
        state = State.FAILED
    }

    override fun close() {
        try {
            scheduler.cancelAll()
        } catch (_: Exception) {}
        try {
            executor.shutdownNow()
        } catch (_: Exception) {}
        try {
            classLoader.close()
        } catch (_: Exception) {}
    }
}

