package kz.bejiihiu.candiriya.plugin

import java.nio.file.Path
import org.apache.logging.log4j.Logger

/**
 * Base contract for every Candiriya plugin.
 *
 * Implement this in your `main` class pointed by `plugin.json#main`.
 * Lifecycle is single-threaded per plugin (its own executor), so you
 * don't need to synchronize `onEnable`/`onDisable`.
 *
 * ```kotlin
 * class MyPlugin : Plugin {
 *   override fun onEnable(ctx: PluginContext) {
 *     ctx.logger.info("hello from {}", ctx.description.id)
 *   }
 * }
 * ```
 *
 * Future Velocity bridge: a single Candiriya plugin can embed a
 * `VelocityPluginManager` — just add `velocity-api` as `compileOnly`
 * and delegate inside `onEnable`. Core does NOT know about Velocity
 * types, so you can do it without forking proxy.
 */
public interface Plugin {
    /**
     * Called after jar is loaded but before proxy started accepting connections.
     * Use for config parsing, registering listeners/commands.
     * Runs on plugin's dedicated thread, timeout 10s (see `PluginManager`).
     */
    public fun onLoad() {}

    /**
     * Called when proxy transitions `STARTING -> RUNNING`.
     * Register everything here. [context] gives you access to proxy APIs.
     */
    public fun onEnable(context: PluginContext) {}

    /**
     * Called on `STOPPING`. Unregister listeners, flush data.
     * Scheduler tasks owned by this plugin are auto-cancelled AFTER this returns.
     * Runs on plugin's thread as well.
     */
    public fun onDisable() {}
}

/**
 * Passed to [Plugin.onEnable]. Holds all per-plugin handles.
 *
 * Do NOT store this beyond `onDisable` — references to server become stale.
 */
public interface PluginContext {
    /** Parsed `plugin.json` */
    public val description: PluginDescription

    /** Dedicated logger `candiriya-plugin-<id>` */
    public val logger: Logger

    /** Folder `plugins/<id>/` — created on load */
    public val dataDirectory: Path

    /** Proxy facade — players, servers, config */
    public val server: ProxyServer

    /** Event bus scoped to this plugin (auto-unregistered on disable) */
    public val events: EventBus

    /** Scheduler that tags every task with this plugin's id */
    public val scheduler: PluginScheduler

    /** Command manager — wraps core `CommandManager` with ownership tracking */
    public val commands: PluginCommandManager

    /** Permission manager passthrough */
    public val permissions: PermissionRegistry

    /** Messaging channels (plugin messages `candiriya:channel`) */
    public val messaging: PluginMessaging
}
