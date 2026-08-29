package kz.bejiihiu.candyriya.command.builtin

import kz.bejiihiu.candyriya.command.Command
import kz.bejiihiu.candyriya.command.CommandSource
import kz.bejiihiu.candyriya.config.ProxyConfig
import kz.bejiihiu.candyriya.player.PlayerManager
import kz.bejiihiu.candyriya.player.RegisteredServer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

/**
 * `/send` — send player(s) to another server.
 * Requires `Candyriya.command.send`.
 * Usage: `/send <player|all> <server>`
 */
public class SendCommand(
    private val playerManager: PlayerManager,
    private val config: ProxyConfig,
    private val servers: Map<String, RegisteredServer> = mapOf(
        "default" to RegisteredServer("default", config.backend.host, config.backend.port)
    )
) : Command {
    override val permission: String = "Candyriya.command.send"
    override val description: String = "Send player to server"
    override val usage: String = "<player|all> <server>"

    override fun execute(source: CommandSource, args: Array<String>) {
        if (args.size < 2) {
            source.sendMessage(Component.text("Usage: /send <player|all> <server>", NamedTextColor.RED))
            source.sendMessage(Component.text("Servers: ${servers.keys.joinToString(", ")}", NamedTextColor.GRAY))
            return
        }
        val targetName = args[1].lowercase()
        val target = servers[targetName] ?: servers.entries.find { it.key.lowercase() == targetName }?.value
        if (target == null) {
            source.sendMessage(Component.text("Server not found: ${args[1]}", NamedTextColor.RED))
            return
        }
        val who = args[0]
        if (who.equals("all", ignoreCase = true)) {
            val all = playerManager.all().toList()
            if (all.isEmpty()) {
                source.sendMessage(Component.text("No players online", NamedTextColor.YELLOW))
                return
            }
            for (p in all) {
                p.server = target
            }
            source.sendMessage(Component.text("Sent ${all.size} players to ${target.name}", NamedTextColor.GREEN))
            return
        }
        val player = playerManager.getByName(who)
        if (player == null) {
            source.sendMessage(Component.text("Player not found: $who", NamedTextColor.RED))
            return
        }
        player.server = target
        source.sendMessage(Component.text("Sent ${player.username} to ${target.name} (${target.address()})", NamedTextColor.GREEN))
        // TODO: actual backend transfer
    }

    override fun suggest(source: CommandSource, args: Array<String>): List<String> {
        return when (args.size) {
            1 -> {
                val prefix = args[0].lowercase()
                val names = playerManager.all().map { it.username } + "all"
                names.filter { it.lowercase().startsWith(prefix) }
            }
            2 -> {
                val prefix = args[1].lowercase()
                servers.keys.filter { it.lowercase().startsWith(prefix) }
            }
            else -> emptyList()
        }
    }
}
