package kz.bejiihiu.candyriya.command.builtin

import kz.bejiihiu.candyriya.command.Command
import kz.bejiihiu.candyriya.command.CommandSource
import kz.bejiihiu.candyriya.player.PlayerManager
import kz.bejiihiu.candyriya.server.ServerRegistry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

/**
 * `/send` — send player(s) to another server.
 * Requires `candyriya.command.send`.
 * Usage: `/send <player|all> <server>`
 */
public class SendCommand(
    private val playerManager: PlayerManager,
    private val registry: ServerRegistry
) : Command {
    override val permission: String = "candyriya.command.send"
    override val description: String = "Send player to server"
    override val usage: String = "<player|all> <server>"

    override fun execute(source: CommandSource, args: Array<String>) {
        if (args.size < 2) {
            source.sendMessage(Component.text("Usage: /send <player|all> <server>", NamedTextColor.RED))
            source.sendMessage(Component.text("Servers: ${registry.all().joinToString(", ") { it.name }}", NamedTextColor.GRAY))
            return
        }
        val targetName = args[1].lowercase()
        val target = registry.get(targetName)
        if (target == null) {
            source.sendMessage(Component.text("Server not found: ${args[1]}", NamedTextColor.RED))
            return
        }
        if (!registry.isAvailable(target)) {
            source.sendMessage(Component.text("Server ${target.name} is currently unavailable", NamedTextColor.RED))
            return
        }
        val who = args[0]
        if (who.equals("all", ignoreCase = true)) {
            val all = playerManager.all().toList()
            if (all.isEmpty()) {
                source.sendMessage(Component.text("No players online", NamedTextColor.YELLOW))
                return
            }
            var sent = 0
            for (p in all) {
                p.connect(target).whenComplete { result, _ ->
                    if (result != null && result.isSuccess) sent++
                }
            }
            source.sendMessage(Component.text("Sending ${all.size} players to ${target.name}", NamedTextColor.GREEN))
            return
        }
        val player = playerManager.getByName(who)
        if (player == null) {
            source.sendMessage(Component.text("Player not found: $who", NamedTextColor.RED))
            return
        }
        source.sendMessage(Component.text("Sending ${player.username} to ${target.name} (${target.address()})", NamedTextColor.GREEN))
        player.connect(target).whenComplete { result, ex ->
            if (ex != null) {
                source.sendMessage(Component.text("Failed: ${ex.message}", NamedTextColor.RED))
            } else if (result.isSuccess) {
                source.sendMessage(Component.text("Sent ${player.username} to ${target.name}", NamedTextColor.GREEN))
            } else {
                source.sendMessage(result.reason ?: Component.text("Could not connect to ${target.name}", NamedTextColor.RED))
            }
        }
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
                registry.all().map { it.name }.filter { it.lowercase().startsWith(prefix) }
            }
            else -> emptyList()
        }
    }
}


