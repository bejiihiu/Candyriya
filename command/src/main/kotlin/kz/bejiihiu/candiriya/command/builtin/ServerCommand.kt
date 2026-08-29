package kz.bejiihiu.candiriya.command.builtin

import kz.bejiihiu.candiriya.command.Command
import kz.bejiihiu.candiriya.command.CommandSource
import kz.bejiihiu.candiriya.command.PlayerSource
import kz.bejiihiu.candiriya.config.ProxyConfig
import kz.bejiihiu.candiriya.player.PlayerManager
import kz.bejiihiu.candiriya.player.RegisteredServer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

/**
 * `/server` — view and switch to another server.
 * Per velocity spec: granted to all by default (`candiriya.command.server`).
 * - `/server` — show current server + list available
 * - `/server <name>` — attempt to connect
 */
public class ServerCommand(
    private val playerManager: PlayerManager,
    private val config: ProxyConfig,
    private val servers: Map<String, RegisteredServer> = mapOf(
        "default" to RegisteredServer("default", config.backend.host, config.backend.port)
    )
) : Command {
    override val permission: String = "candiriya.command.server"
    override val description: String = "View or switch server"
    override val usage: String = "[<server>]"

    override fun execute(source: CommandSource, args: Array<String>) {
        if (args.isEmpty()) {
            if (source is PlayerSource) {
                val player = playerManager.get(source.uuid())
                val current = player?.server?.name ?: "unknown"
                source.sendMessage(Component.text("You are on: $current", NamedTextColor.YELLOW))
                source.sendMessage(Component.text("Available: ${servers.keys.joinToString(", ")}", NamedTextColor.GRAY))
                source.sendMessage(Component.text("Use /server <name> to switch", NamedTextColor.DARK_GRAY))
            } else {
                source.sendMessage(Component.text("Servers: ${servers.keys.joinToString(", ")}", NamedTextColor.YELLOW))
                source.sendMessage(Component.text("Players: ${playerManager.count()} online", NamedTextColor.GRAY))
            }
            return
        }
        val targetName = args[0].lowercase()
        val target = servers[targetName] ?: servers.entries.find { it.key.lowercase() == targetName }?.value
        if (target == null) {
            source.sendMessage(Component.text("Server not found: ${args[0]}", NamedTextColor.RED))
            source.sendMessage(Component.text("Available: ${servers.keys.joinToString(", ")}", NamedTextColor.GRAY))
            return
        }
        if (source !is PlayerSource) {
            source.sendMessage(Component.text("Only players can switch servers", NamedTextColor.RED))
            return
        }
        val player = playerManager.get(source.uuid())
        if (player == null) {
            source.sendMessage(Component.text("Player not found", NamedTextColor.RED))
            return
        }
        // stub: update player's server field and notify
        player.server = target
        source.sendMessage(Component.text("Connecting to ${target.name} (${target.address()})...", NamedTextColor.GREEN))
        // TODO: actual backend transfer via BackendConnection
    }

    override fun suggest(source: CommandSource, args: Array<String>): List<String> {
        if (args.size == 1) {
            val prefix = args[0].lowercase()
            return servers.keys.filter { it.lowercase().startsWith(prefix) }
        }
        return emptyList()
    }
}
