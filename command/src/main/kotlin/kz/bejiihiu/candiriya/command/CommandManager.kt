package kz.bejiihiu.candiriya.command

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import java.util.concurrent.ConcurrentHashMap
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.apache.logging.log4j.LogManager

/**
 * Registry + dispatcher.
 * Stores alias (lowercase) -> Command. Supports multiple aliases per command.
 * Dispatch checks permission via `source.hasPermission` before execution.
 * Tab-complete filters by permission as well (like Brigadier .requires).
 */
public class CommandManager {
    private val logger = LogManager.getLogger(CommandManager::class.java)
    private val commands = ConcurrentHashMap<String, Command>() // alias -> command
    private val primaryAlias = ConcurrentHashMap<Command, String>() // command -> primary alias

    public fun register(alias: String, command: Command, vararg extraAliases: String) {
        val lower = alias.lowercase()
        check(!commands.containsKey(lower)) { "command alias '$alias' already registered" }
        commands[lower] = command
        primaryAlias[command] = lower
        for (a in extraAliases) {
            val la = a.lowercase()
            check(!commands.containsKey(la)) { "alias '$a' already taken" }
            commands[la] = command
        }
        logger.info("registered command /{} aliases={} perm={}", alias, extraAliases.toList(), command.permission)
    }

    public fun unregister(alias: String): Boolean {
        val cmd = commands.remove(alias.lowercase()) ?: return false
        // remove other aliases pointing to same command
        val toRemove = commands.entries.filter { it.value === cmd }.map { it.key }
        for (k in toRemove) commands.remove(k)
        primaryAlias.remove(cmd)
        logger.info("unregistered command /{}", alias)
        return true
    }

    public fun getCommand(alias: String): Command? = commands[alias.lowercase()]

    public fun allCommands(): Map<String, Command> = commands.toMap()

    /** Unique commands (deduped). */
    public fun registeredCommands(): Set<Command> = primaryAlias.keys.toSet()

    @SuppressFBWarnings(value = ["BC_BAD_CAST_TO_ABSTRACT_COLLECTION"], justification = "kotlin filter+map safe xd")
    public fun aliasesFor(command: Command): List<String> = commands.entries.filter { it.value === command }.map { it.key }

    /**
     * Dispatch raw input (without leading slash). Returns true if command found (even if no perm).
     */
    public fun dispatch(source: CommandSource, input: String): Boolean {
        val trimmed = input.trim().removePrefix("/")
        if (trimmed.isEmpty()) {
            source.sendMessage(Component.text("No command entered", NamedTextColor.RED))
            return false
        }
        val parts = trimmed.split("\\s+".toRegex())
        val alias = parts[0].lowercase()
        val args = if (parts.size > 1) parts.subList(1, parts.size).toTypedArray() else emptyArray()
        val cmd = commands[alias]
        if (cmd == null) {
            source.sendMessage(Component.text("Unknown command: /$alias", NamedTextColor.RED))
            source.sendMessage(Component.text("Try /candiriya help", NamedTextColor.GRAY))
            return false
        }
        // permission check — like Brigadier requires()
        val perm = cmd.permission
        if (perm != null && !source.hasPermission(perm)) {
            source.sendMessage(Component.text("You don't have permission: $perm", NamedTextColor.RED))
            return true
        }
        try {
            cmd.execute(source, args)
        } catch (e: Exception) {
            logger.error("error executing /{} by {}", alias, source.name, e)
            source.sendMessage(Component.text("Error executing command: ${e.message}", NamedTextColor.RED))
        }
        return true
    }

    /**
     * Tab-complete suggestions. Filters by permission — no perm = no suggest.
     */
    @SuppressFBWarnings(value = ["BC_BAD_CAST_TO_ABSTRACT_COLLECTION"], justification = "kotlin filter sorted safe xd")
    public fun suggest(source: CommandSource, input: String): List<String> {
        val trimmed = input.trim().removePrefix("/")
        // if no space, suggest aliases
        if (!trimmed.contains(" ")) {
            val prefix = trimmed.lowercase()
            return commands.keys.filter { it.startsWith(prefix) }
                .filter { alias ->
                    val c = commands[alias] ?: return@filter false
                    val p = c.permission
                    p == null || source.hasPermission(p)
                }
                .sorted()
        }
        val parts = trimmed.split("\\s+".toRegex())
        val alias = parts[0].lowercase()
        val cmd = commands[alias] ?: return emptyList()
        val perm = cmd.permission
        if (perm != null && !source.hasPermission(perm)) return emptyList()
        val args = if (parts.size > 1) parts.subList(1, parts.size).toTypedArray() else emptyArray()
        return try {
            cmd.suggest(source, args)
        } catch (e: Exception) {
            logger.warn("error suggesting /{}", alias, e)
            emptyList()
        }
    }

    public fun sendHelp(source: CommandSource) {
        source.sendMessage(Component.text("Available commands:", NamedTextColor.YELLOW))
        val seen = mutableSetOf<Command>()
        for ((alias, cmd) in commands.entries.sortedBy { it.key }) {
            if (!seen.add(cmd)) continue // one line per command
            val perm = cmd.permission
            if (perm != null && !source.hasPermission(perm)) continue
            val desc = if (cmd.description.isNotEmpty()) " - ${cmd.description}" else ""
            val usage = if (cmd.usage.isNotEmpty()) " ${cmd.usage}" else ""
            source.sendMessage(Component.text("/$alias$usage$desc", NamedTextColor.GRAY))
        }
    }
}
