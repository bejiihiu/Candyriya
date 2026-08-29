package kz.bejiihiu.candiriya.command.builtin

import kz.bejiihiu.candiriya.command.Command
import kz.bejiihiu.candiriya.command.CommandSource
import kz.bejiihiu.candiriya.player.PlayerManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

/**
 * `/glist` — view number of players on proxy.
 * Per velocity spec: `candiriya.command.glist` granted to nobody by default.
 * - `/glist` — total count
 * - `/glist all` — per-server listing
 */
public class GlistCommand(
    private val playerManager: PlayerManager
) : Command {
    override val permission: String = "candiriya.command.glist"
    override val description: String = "List proxy players"
    override val usage: String = "[all]"

    override fun execute(source: CommandSource, args: Array<String>) {
        val total = playerManager.count()
        if (args.isEmpty()) {
            source.sendMessage(Component.text("Total players online: $total", NamedTextColor.YELLOW))
            if (total > 0) {
                val names = playerManager.all().joinToString(", ") { it.username }
                source.sendMessage(Component.text("Players: $names", NamedTextColor.GRAY))
            }
            source.sendMessage(Component.text("Use /glist all for per-server view", NamedTextColor.DARK_GRAY))
            return
        }
        if (args[0].equals("all", ignoreCase = true)) {
            source.sendMessage(Component.text("Total players: $total", NamedTextColor.YELLOW))
            // group by server
            val byServer = playerManager.all().groupBy { it.server?.name ?: "unknown" }
            for ((server, players) in byServer) {
                source.sendMessage(
                    Component.text(
                        "[$server] (${players.size}): ${players.joinToString(", ") { it.username }}",
                        NamedTextColor.GRAY
                    )
                )
            }
            if (byServer.isEmpty()) {
                source.sendMessage(Component.text("No players per server (single backend)", NamedTextColor.GRAY))
            }
            return
        }
        source.sendMessage(Component.text("Usage: /glist [all]", NamedTextColor.RED))
    }

    override fun suggest(source: CommandSource, args: Array<String>): List<String> {
        if (args.size == 1) {
            val prefix = args[0].lowercase()
            return listOf("all").filter { it.startsWith(prefix) }
        }
        return emptyList()
    }
}
