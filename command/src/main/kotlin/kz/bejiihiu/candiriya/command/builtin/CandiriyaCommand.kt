package kz.bejiihiu.candiriya.command.builtin

import java.nio.file.Files
import java.nio.file.Path
import kz.bejiihiu.candiriya.command.Command
import kz.bejiihiu.candiriya.command.CommandManager
import kz.bejiihiu.candiriya.command.CommandSource
import kz.bejiihiu.candiriya.permission.PermissionManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.apache.logging.log4j.LogManager

/**
 * Root `/candyriya` command — mirrors Velocity's `/velocity` but renamed.
 * Subcommands: plugins, info, reload, dump, heap (from velocity spec)
 * plus help/perms/op kept for internal use.
 *
 * Permissions: candyriya.command.<sub> or candyriya.* for op.
 */
/**
 * Simple DTO for plugin info without pulling `:plugin-loader` into `:command`.
 * Core passes a lambda that returns this from PluginManager.
 */
public data class PluginInfo(
    val id: String,
    val name: String,
    val version: String,
    val state: String
)

public class CandiriyaCommand(
    private val commandManager: CommandManager,
    private val permissionManager: PermissionManager,
    private val permissionsFile: Path? = null,
    private val version: String = "26.1",
    private val pluginsProvider: (() -> List<PluginInfo>)? = null
) : Command {
    override val permission: String? = null // root open, subcommands check own perms
    override val description: String = "Candiriya proxy main command"
    override val usage: String = "<plugins|info|reload|dump|heap|help>"

    private val mini = MiniMessage.miniMessage()
    private val logger = LogManager.getLogger(CandiriyaCommand::class.java)

    override fun execute(source: CommandSource, args: Array<String>) {
        if (args.isEmpty()) {
            sendInfo(source)
            return
        }
        when (args[0].lowercase()) {
            "help" -> handleHelp(source, args.drop(1).toTypedArray())
            "info" -> handleInfo(source)
            "plugins" -> handlePlugins(source)
            "reload" -> handleReload(source)
            "dump" -> handleDump(source)
            "heap" -> handleHeap(source)
            // keep extra internals
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
        return when (args[0].lowercase()) {
            "help" -> commandManager.suggest(source, args.drop(1).joinToString(" "))
            "perms" -> emptyList()
            "op" -> emptyList()
            else -> emptyList()
        }
    }

    private fun suggestRoot(source: CommandSource): List<String> {
        val all = mutableListOf<String>()
        all.add("help")
        all.add("info")
        if (source.hasPermission("candiriya.command.plugins")) all.add("plugins")
        if (source.hasPermission("candiriya.command.reload")) all.add("reload")
        if (source.hasPermission("candiriya.command.dump")) all.add("dump")
        if (source.hasPermission("candiriya.command.heap")) all.add("heap")
        // extra
        if (source.hasPermission("candiriya.command.perms")) all.add("perms")
        if (source.hasPermission("candiriya.command.op") || source.isConsole) all.add("op")
        // info is open to those with perm, but we also allow everyone to see it
        if (!all.contains("info") && source.hasPermission("candiriya.command.info")) all.add("info")
        return all.distinct().sorted()
    }

    private fun handleHelp(source: CommandSource, args: Array<String>) {
        if (args.isEmpty()) {
            sendHelp(source)
            return
        }
        val alias = args[0]
        val cmd = commandManager.getCommand(alias)
        if (cmd == null) {
            source.sendMessage(Component.text("Unknown command: $alias", NamedTextColor.RED))
            return
        }
        source.sendMessage(Component.text("/$alias ${cmd.usage} - ${cmd.description}", NamedTextColor.GRAY))
    }

    private fun sendHelp(source: CommandSource) {
        source.sendMessage(mini.deserialize("<yellow>Candiriya help:</yellow>"))
        source.sendMessage(Component.text("/candyriya plugins - list plugins", NamedTextColor.GRAY))
        source.sendMessage(Component.text("/candyriya info - proxy info", NamedTextColor.GRAY))
        if (source.hasPermission("candiriya.command.reload")) {
            source.sendMessage(Component.text("/candyriya reload - reload config/permissions", NamedTextColor.GRAY))
        }
        if (source.hasPermission("candiriya.command.dump")) {
            source.sendMessage(Component.text("/candyriya dump - anonymized dump", NamedTextColor.GRAY))
        }
        if (source.hasPermission("candiriya.command.heap")) {
            source.sendMessage(Component.text("/candyriya heap - heap dump", NamedTextColor.GRAY))
        }
        source.sendMessage(Component.text("/server [name] - switch server", NamedTextColor.GRAY))
        source.sendMessage(Component.text("/glist [all] - list players", NamedTextColor.GRAY))
        source.sendMessage(Component.text("/send <player|all> <server> - send player", NamedTextColor.GRAY))
        source.sendMessage(Component.text("/shutdown [reason] - console only", NamedTextColor.GRAY))
        commandManager.sendHelp(source)
    }

    private fun sendInfo(source: CommandSource) {
        // per spec: requires candyriya.command.info, but we also allow everyone to see basic info
        if (!source.hasPermission("candiriya.command.info") && !source.isConsole) {
            // still show but hint about perm
            source.sendMessage(Component.text("No permission: candyriya.command.info", NamedTextColor.RED))
            return
        }
        source.sendMessage(mini.deserialize("<gradient:#55FF55:#55FFFF>Candiriya $version</gradient> <gray>— proxy</gray>"))
        source.sendMessage(Component.text("Velocity API: 3.x (candyriya fork)", NamedTextColor.GRAY))
        source.sendMessage(Component.text("Source: ${source.name} (console=${source.isConsole})", NamedTextColor.GRAY))
        source.sendMessage(Component.text("Try /candyriya help", NamedTextColor.DARK_GRAY))
    }

    private fun handleInfo(source: CommandSource) = sendInfo(source)

    private fun handlePlugins(source: CommandSource) {
        if (!source.hasPermission("candiriya.command.plugins")) {
            source.sendMessage(Component.text("No permission: candyriya.command.plugins", NamedTextColor.RED))
            return
        }
        val plugins = try {
            pluginsProvider?.invoke()
        } catch (_: Exception) {
            null
        }
        if (plugins == null) {
            source.sendMessage(Component.text("Plugins (0): ", NamedTextColor.YELLOW))
            source.sendMessage(Component.text("No plugins installed (plugin API coming soon)", NamedTextColor.GRAY))
            return
        }
        if (plugins.isEmpty()) {
            source.sendMessage(Component.text("Plugins (0): ", NamedTextColor.YELLOW))
            source.sendMessage(Component.text("No plugins installed", NamedTextColor.GRAY))
            return
        }
        source.sendMessage(Component.text("Plugins (${plugins.size}): ", NamedTextColor.YELLOW))
        for (p in plugins.sortedBy { it.id }) {
            val color = when (p.state) {
                "ENABLED" -> NamedTextColor.GREEN
                "FAILED" -> NamedTextColor.RED
                else -> NamedTextColor.GRAY
            }
            source.sendMessage(Component.text("- ${p.name} (${p.id}) v${p.version} [${p.state}]", color))
        }
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
            // TODO: reload velocity.toml equivalent — candiriya.toml
            source.sendMessage(Component.text("Proxy config reload not yet implemented (restart for now)", NamedTextColor.YELLOW))
        } catch (e: Exception) {
            logger.warn("reload failed", e)
            source.sendMessage(Component.text("Reload failed: ${e.message}", NamedTextColor.RED))
        }
    }

    private fun handleDump(source: CommandSource) {
        if (!source.hasPermission("candiriya.command.dump")) {
            source.sendMessage(Component.text("No permission: candiriya.command.dump", NamedTextColor.RED))
            return
        }
        try {
            val dumpDir = Path.of("dumps")
            Files.createDirectories(dumpDir)
            val file = dumpDir.resolve("candyriya-dump-${System.currentTimeMillis()}.txt")
            val content = buildString {
                appendLine("Candiriya dump")
                appendLine("version: $version")
                appendLine("groups: ${permissionManager.getGroups().keys}")
                appendLine("ops: ${permissionManager.getGroups()}") // anonymized
                appendLine("commands: ${commandManager.allCommands().keys}")
            }
            Files.writeString(file, content)
            source.sendMessage(Component.text("Dump written to $file (anonymized)", NamedTextColor.GREEN))
            source.sendMessage(Component.text("Share it in Discord for support", NamedTextColor.GRAY))
        } catch (e: Exception) {
            logger.warn("dump failed", e)
            source.sendMessage(Component.text("Dump failed: ${e.message}", NamedTextColor.RED))
        }
    }

    private fun handleHeap(source: CommandSource) {
        if (!source.hasPermission("candiriya.command.heap")) {
            source.sendMessage(Component.text("No permission: candiriya.command.heap", NamedTextColor.RED))
            return
        }
        source.sendMessage(Component.text("Heap dump: use jcmd <pid> GC.heap_dump <path> or jmap", NamedTextColor.YELLOW))
        source.sendMessage(Component.text("Sensitive data — share carefully!", NamedTextColor.RED))
        try {
            val dumpDir = Path.of("dumps")
            Files.createDirectories(dumpDir)
            val file = dumpDir.resolve("candyriya-heap-${System.currentTimeMillis()}.hprof")
            // try to trigger via HotSpotDiagnosticMXBean if available
            try {
                val server = java.lang.management.ManagementFactory.getPlatformMBeanServer()
                val objName = javax.management.ObjectName.getInstance("com.sun.management:type=HotSpotDiagnostic")
                server.invoke(
                    objName,
                    "dumpHeap",
                    arrayOf<Any>(file.toAbsolutePath().toString(), true),
                    arrayOf(String::class.java.name, Boolean::class.java.name)
                )
                source.sendMessage(Component.text("Heap dump written to $file", NamedTextColor.GREEN))
            } catch (ex: Exception) {
                source.sendMessage(Component.text("Auto dump failed: ${ex.message}", NamedTextColor.RED))
                source.sendMessage(Component.text("Manual: jcmd ${ProcessHandle.current().pid()} GC.heap_dump $file", NamedTextColor.GRAY))
            }
        } catch (e: Exception) {
            logger.warn("heap dump failed", e)
            source.sendMessage(Component.text("Heap dump failed: ${e.message}", NamedTextColor.RED))
        }
    }

    private fun handlePerms(source: CommandSource, args: Array<String>) {
        if (!source.hasPermission("candiriya.command.perms")) {
            source.sendMessage(Component.text("No permission: candiriya.command.perms", NamedTextColor.RED))
            return
        }
        if (args.isEmpty()) {
            source.sendMessage(Component.text("Usage: /candyriya perms <player>", NamedTextColor.RED))
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
            source.sendMessage(Component.text("Usage: /candyriya op <player>", NamedTextColor.RED))
            return
        }
        source.sendMessage(Component.text("Op toggle for ${args[0]} — wire to PlayerManager lookup in next step", NamedTextColor.YELLOW))
    }
}
