package kz.bejiihiu.candyriya.plugin

import java.nio.file.Path
import net.kyori.adventure.text.Component

/**
 * Command registration scoped to plugin. Auto-unregistered on disable.
 */
public interface PluginCommandManager {
    /** Register `/alias` owned by this plugin. Throws if alias taken. */
    public fun register(alias: String, command: PluginCommand, vararg extraAliases: String)

    /** Unregister alias (and its siblings) */
    public fun unregister(alias: String): Boolean

    /** All aliases owned by this plugin */
    public fun ownedAliases(): Set<String>
}

public interface PluginCommand {
    public val permission: String?
    public val description: String
    public val usage: String
    public fun execute(source: PluginCommandSource, args: Array<String>)
    public fun suggest(source: PluginCommandSource, args: Array<String>): List<String> = emptyList()
}

public interface PluginCommandSource {
    public val name: String
    public val isConsole: Boolean
    public fun hasPermission(permission: String): Boolean
    public fun sendMessage(component: Component)
    public fun asPlayer(): ProxyPlayer?
}

/** Thin permission registry passthrough — lets plugins check/register perms without depending on `:permissions`. */
public interface PermissionRegistry {
    public fun has(player: ProxyPlayer, permission: String): Boolean
    public fun has(source: PluginCommandSource, permission: String): Boolean
}

/** Plugin messaging — `candiriya:channel` style, mirrors Velocity's channel API but minimal. */
public interface PluginMessaging {
    /** Register channel `id:channel` (e.g. `myplugin:data`). Must be lowercase `namespace:name`. */
    public fun registerChannel(channel: String): Boolean

    /** Unregister */
    public fun unregisterChannel(channel: String): Boolean

    /** Send to player */
    public fun send(player: ProxyPlayer, channel: String, data: ByteArray): Boolean

    /** All registered channels */
    public fun channels(): Set<String>
}

/** Tiny config helper — gives plugins `plugins/<id>/config.toml` style loading without pulling nightconfig. */
public interface PluginConfig {
    /** Data dir `plugins/<id>/` */
    public val directory: Path

    /** Load TOML as map, or create default from classpath resource. */
    public fun loadOrCreate(fileName: String = "config.toml", defaultResource: String? = null): Path
}

