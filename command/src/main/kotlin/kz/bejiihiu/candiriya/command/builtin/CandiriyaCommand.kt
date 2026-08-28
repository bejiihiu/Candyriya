package kz.bejiihiu.candiriya.command.builtin

import kz.bejiihiu.candiriya.command.Command
import kz.bejiihiu.candiriya.command.CommandManager
import kz.bejiihiu.candiriya.command.CommandSource
import kz.bejiihiu.candiriya.permission.PermissionManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage

/**
 * Root `candiriya` command — the only builtin for now.
 * Subcommands: help, info, reload, perms, op
 * Permission: candiriya.command.<sub> or candiriya.* for op.
 */
public class CandiriyaCommand(
    private val commandManager: CommandManager,
    private val permissionManager: PermissionManager,
    private val permissionsFile: java.nio.file.Path? = null,
    private val version: String = "26.1"
) : Command {
    override val permission: String? = null // root allows help for everyone, subcommands check own perms
    override val description: String = "Candiriya proxy main command"
    override val usage: String = "<help|info|reload|perms|op>"

    private val mini = MiniMessage.miniMessage()

    override fun execute(source: CommandSource, args: Array<String>) {
        if (args.isEmpty()) {
            sendInfo(source)
            return
        }
        when (args[0].lowercase()) {
            "help" -> handleHelp(source, args.drop(1).toTypedArray())
            "info" -> sendInfo(source)
            "reload" -> handleReload(source)
            "perms" -> handlePerms(source, args.drop(1).toTypedArray())
            "op" -> handleOp(source, args.drop(1).toTypedArray())
            else -> {
                source.sendMessage(Component.text("Unknown subcommand: ${args[0]}", NamedTextColor.RED))
                sendHelp(source)
            }
        }
    }

    override fun suggest(source: CommandSource, args: Array<String>): List<String> {
        if (args.isEmpty()) return suggestRoot(source)
        if (args.size == 1) {
            val prefix = args[0].lowercase()
            return suggestRoot(source).filter { it.startsWith(prefix) }
        }
        // sub suggestions
        return when (args[0].lowercase()) {
            "help" -> commandManager.suggest(source, args.drop(1).joinToString(" "))
            "perms" -> suggestPerms(source, args.drop(1).toTypedArray())
            "op" -> suggestOp(args.drop(1).toTypedArray())
            else -> emptyList()
        }
    }

    private fun suggestRoot(source: CommandSource): List<String> {
        val all = mutableListOf("help", "info")
        if (source.hasPermission("candiriya.command.reload")) all.add("reload")
        if (source.hasPermission("candiriya.command.perms")) all.add("perms")
        if (source.hasPermission("candiriya.command.op") || source.isConsole) all.add("op")
        return all
    }

    private fun handleHelp(source: CommandSource, args: Array<String>) {
        if (args.isEmpty()) {
            sendHelp(source)
            return
        }
        // delegate to command manager help for alias
        val alias = args[0]
        val cmd = commandManager.getCommand(alias)
        if (cmd == null) {
            source.sendMessage(Component.text("Unknown command: $alias", NamedTextColor.RED))
            return
        }
        source.sendMessage(Component.text("/$alias ${cmd.usage} - ${cmd.description}", NamedTextColor.GRAY))
    }

    private fun sendHelp(source: CommandSource) {
        source.sendMessage(mini.deserialize("<yellow>Candiriya help:"))
        source.sendMessage(Component.text("/candiriya help - show this help", NamedTextColor.GRAY))
        source.sendMessage(Component.text("/candiriya info - proxy info", NamedTextColor.GRAY))
        if (source.hasPermission("candiriya.command.reload")) {
            source.sendMessage(Component.text("/candiriya reload - reload permissions", NamedTextColor.GRAY))
        }
        if (source.hasPermission("candiriya.command.perms")) {
            source.sendMessage(Component.text("/candiriya perms <user> - show perms", NamedTextColor.GRAY))
        }
        if (source.hasPermission("candiriya.command.op") || source.isConsole) {
            source.sendMessage(Component.text("/candiriya op <player> - toggle op", NamedTextColor.GRAY))
        }
        // also show all commands user can see
        commandManager.sendHelp(source)
    }

    private fun sendInfo(source: CommandSource) {
        source.sendMessage(mini.deserialize("<gradient:#55FF55:#55FFFF>Candiriya $version</gradient> <gray>— proxy</gray>"))
        source.sendMessage(Component.text("Source: ${source.name} (console=${source.isConsole})", NamedTextColor.GRAY))
        source.sendMessage(Component.text("Try /candiriya help", NamedTextColor.DARK_GRAY))
    }

    private fun handleReload(source: CommandSource) {
        if (!source.hasPermission("candiriya.command.reload") && !source.isConsole) {
            source.sendMessage(Component.text("No permission: candiriya.command.reload", NamedTextColor.RED))
            return
        }
        try {
            if (permissionsFile != null) {
                permissionManager.loadFromFile(permissionsFile)
                source.sendMessage(Component.text("Permissions reloaded from $permissionsFile", NamedTextColor.GREEN))
            } else {
                source.sendMessage(Component.text("Permissions reloaded (no file)", NamedTextColor.GREEN))
            }
        } catch (e: Exception) {
            source.sendMessage(Component.text("Reload failed: ${e.message}", NamedTextColor.RED))
        }
    }

    private fun handlePerms(source: CommandSource, args: Array<String>) {
        if (!source.hasPermission("candiriya.command.perms")) {
            source.sendMessage(Component.text("No permission: candiriya.command.perms", NamedTextColor.RED))
            return
        }
        if (args.isEmpty()) {
            source.sendMessage(Component.text("Usage: /candiriya perms <player>", NamedTextColor.RED))
            return
        }
        source.sendMessage(Component.text("Perms for ${args[0]}: (lookup by UUID not wired yet)", NamedTextColor.YELLOW))
        source.sendMessage(Component.text("Groups: ${permissionManager.getGroups().keys}", NamedTextColor.GRAY))
    }

    private fun handleOp(source: CommandSource, args: Array<String>) {
        if (!source.isConsole && !source.hasPermission("candiriya.command.op")) {
            source.sendMessage(Component.text("No permission: candiriya.command.op", NamedTextColor.RED))
            return
        }
        if (args.isEmpty()) {
            source.sendMessage(Component.text("Usage: /candiriya op <player>", NamedTextColor.RED))
            return
        }
        source.sendMessage(Component.text("Op toggle for ${args[0]} — wire to PlayerManager lookup in next step", NamedTextColor.YELLOW))
    }

    private fun suggestPerms(source: CommandSource, args: Array<String>): List<String> = emptyList()
    private fun suggestOp(args: Array<String>): List<String> = emptyList()
}
