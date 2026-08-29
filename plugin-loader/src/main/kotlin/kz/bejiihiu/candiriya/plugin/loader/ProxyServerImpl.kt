package kz.bejiihiu.candiriya.plugin.loader

import java.util.Optional
import java.util.UUID
import kz.bejiihiu.candiriya.config.ProxyConfig
import kz.bejiihiu.candiriya.player.PlayerManager
import kz.bejiihiu.candiriya.plugin.ProxyPlayer
import kz.bejiihiu.candiriya.plugin.ProxyServer
import kz.bejiihiu.candiriya.plugin.RegisteredBackend
import net.kyori.adventure.text.Component

/**
 * Adapter from core PlayerManager to plugin-facing ProxyServer.
 * Keeps core types hidden — plugins only see ProxyPlayer.
 */
internal class ProxyServerImpl(
    override val config: ProxyConfig,
    private val playerManager: PlayerManager,
    override val version: String = "26.1",
    private val backends: Map<String, RegisteredBackend> = mapOf(
        "default" to SimpleBackend("default", config.backend.host, config.backend.port)
    )
) : ProxyServer {

    override fun getPlayers(): Collection<ProxyPlayer> = playerManager.all().map { wrap(it) }

    override fun getPlayer(uuid: UUID): Optional<ProxyPlayer> = Optional.ofNullable(playerManager.get(uuid)?.let { wrap(it) })

    override fun getPlayer(name: String): Optional<ProxyPlayer> = Optional.ofNullable(playerManager.getByName(name)?.let { wrap(it) })

    override fun getPlayerCount(): Int = playerManager.count()

    override fun getServers(): Map<String, RegisteredBackend> = backends

    override fun getServer(name: String): Optional<RegisteredBackend> = Optional.ofNullable(backends[name])

    override fun broadcast(component: Component) {
        // stub: real chat packet delivery will be wired via Player connection
        // for now we just ensure no crash and log at debug
        for (p in playerManager.all()) {
            try {
                // use disconnect path as placeholder would be wrong — just skip actual send
                // plugins can still call player.sendMessage which uses same stub
                org.apache.logging.log4j.LogManager.getLogger(ProxyServerImpl::class.java)
                    .debug("broadcast to {}: {}", p.username, component)
            } catch (_: Exception) {}
        }
    }

    override fun isOnlineMode(): Boolean = config.security.onlineMode

    private fun wrap(player: kz.bejiihiu.candiriya.player.Player): ProxyPlayer = PlayerAdapter(player)

    private class SimpleBackend(
        override val name: String,
        override val host: String,
        override val port: Int
    ) : RegisteredBackend {
        override fun sendPlayer(player: ProxyPlayer): Boolean {
            // real routing will be wired when multi-backend lands — stub for now
            return false
        }
    }

    private class PlayerAdapter(
        private val handle: kz.bejiihiu.candiriya.player.Player
    ) : ProxyPlayer {
        override val uuid: UUID get() = handle.uuid
        override val username: String get() = handle.username
        override val currentServer: RegisteredBackend? get() = handle.server?.let {
            SimpleBackend(it.name, it.host, it.port)
        }
        override val isOnline: Boolean get() = handle.state != kz.bejiihiu.candiriya.player.PlayerState.DISCONNECTED

        override fun sendMessage(component: Component) {
            // stub: connection has no chat delivery yet, keep as no-op but log
            try {
                org.apache.logging.log4j.LogManager.getLogger(PlayerAdapter::class.java)
                    .debug("sendMessage to {}: {}", handle.username, component)
            } catch (_: Exception) {}
        }

        override fun disconnect(reason: Component) {
            handle.disconnect(reason)
        }

        override fun hasPermission(permission: String): Boolean {
            // delegate through permission manager if needed — for now check console-style
            // real wiring is done via PermissionRegistry impl
            return false
        }

        override fun execute(task: Runnable) = handle.execute(task)

        override fun getRawHandle(): Any = handle
    }
}
