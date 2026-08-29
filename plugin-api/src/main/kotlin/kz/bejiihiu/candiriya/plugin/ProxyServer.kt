package kz.bejiihiu.candiriya.plugin

import java.util.Optional
import java.util.UUID
import kz.bejiihiu.candiriya.config.ProxyConfig
import net.kyori.adventure.text.Component

/**
 * Facade that plugins see. Mirrors a tiny subset of Velocity's `ProxyServer`.
 * All methods are thread-safe — they hop to the right context internally.
 */
public interface ProxyServer {
    /** Immutable snapshot of current config (re-read on reload, but plugins see copy). */
    public val config: ProxyConfig

    /** Proxy version string, e.g. `26.1` */
    public val version: String

    /** All online players (snapshot copy). */
    public fun getPlayers(): Collection<ProxyPlayer>

    /** Find player by uuid */
    public fun getPlayer(uuid: UUID): Optional<ProxyPlayer>

    /** Find player by name (case-insensitive) */
    public fun getPlayer(name: String): Optional<ProxyPlayer>

    /** Number of online players */
    public fun getPlayerCount(): Int

    /** Registered backends (`[servers]` future, now single `backend`). */
    public fun getServers(): Map<String, RegisteredBackend>

    /** Find backend by name */
    public fun getServer(name: String): Optional<RegisteredBackend>

    /** Broadcast to all players */
    public fun broadcast(component: Component)

    /** Kick wrapper — future Velocity bridge can use same shape */
    public fun isOnlineMode(): Boolean
}

/**
 * Minimal player handle exposed to plugins.
 * Mutations that must run on player's context are exposed as `scheduler` / `execute`.
 */
public interface ProxyPlayer {
    public val uuid: UUID
    public val username: String
    public val currentServer: RegisteredBackend?
    public val isOnline: Boolean

    public fun sendMessage(component: Component)
    public fun disconnect(reason: Component)
    public fun hasPermission(permission: String): Boolean

    /**
     * Run [task] on player's execution context (Folia-style).
     * Use this when touching player state from async plugin thread.
     */
    public fun execute(task: Runnable)

    /** Future Velocity bridge: raw `Player` object is hidden, use this to avoid leaking netty types. */
    public fun getRawHandle(): Any
}

/** Backend server info. Single backend for now, but map-shaped for future multi-backend. */
public interface RegisteredBackend {
    public val name: String
    public val host: String
    public val port: Int
    public fun sendPlayer(player: ProxyPlayer): Boolean
}
