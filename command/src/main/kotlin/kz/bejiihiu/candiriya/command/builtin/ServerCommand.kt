package kz.bejiihiu.candiriya.command.builtin

import kz.bejiihiu.candiriya.command.Command
import kz.bejiihiu.candiriya.command.CommandSource
import kz.bejiihiu.candiriya.command.PlayerSource
import kz.bejiihiu.candiriya.player.PlayerManager
import kz.bejiihiu.candiriya.server.ServerRegistry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

/**
 * `/server` — view and switch to another server.
 * - `/server` — show current server + list available
 * - `/server <name>` — attempt to connect
 */
public class ServerCommand(
    private val playerManager: PlayerManager,
    private val registry: ServerRegistry
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
                val available = registry.all().joinToString(", ") { "${it.name}(${if (registry.isAvailable(it)) "online" else "offline"})" }
                source.sendMessage(Component.text("Available: $available", NamedTextColor.GRAY))
                source.sendMessage(Component.text("Use /server <name> to switch", NamedTextColor.DARK_GRAY))
            } else {
                source.sendMessage(Component.text("Servers: ${registry.all().joinToString(", ") { it.name }}", NamedTextColor.YELLOW))
                source.sendMessage(Component.text("Players: ${playerManager.count()} online", NamedTextColor.GRAY))
            }
            return
        }
        val targetName = args[0].lowercase()
        val target = registry.get(targetName)
        if (target == null) {
            source.sendMessage(Component.text("Server not found: ${args[0]}", NamedTextColor.RED))
            source.sendMessage(Component.text("Available: ${registry.all().joinToString(", ") { it.name }}", NamedTextColor.GRAY))
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
        if (!registry.isAvailable(target)) {
            source.sendMessage(Component.text("Server ${target.name} is currently unavailable", NamedTextColor.RED))
            return
        }
        source.sendMessage(Component.text("Connecting to ${target.name} (${target.address()})...", NamedTextColor.GREEN))
        player.connect(target).whenComplete { result, ex ->
            if (ex != null) {
                source.sendMessage(Component.text("Failed to connect: ${ex.message}", NamedTextColor.RED))
            } else if (result.isSuccess) {
                source.sendMessage(Component.text("Connected to ${target.name}", NamedTextColor.GREEN))
            } else {
                val reason = result.reason ?: Component.text("Could not connect to ${target.name}", NamedTextColor.RED)
                source.sendMessage(reason)
            }
        }
    }

    override fun suggest(source: CommandSource, args: Array<String>): List<String> {
        if (args.size == 1) {
            val prefix = args[0].lowercase()
            return registry.all().map { it.name }.filter { it.lowercase().startsWith(prefix) }
        }
        return emptyList()
    }
}
