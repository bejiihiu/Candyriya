package kz.bejiihiu.candiriya.command.builtin

import kz.bejiihiu.candiriya.command.Command
import kz.bejiihiu.candiriya.command.CommandSource
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer

/**
 * `/shutdown` — gracefully shut down proxy. Console only.
 * Optional reason: MiniMessage or JSON (if starts with ", [, {).
 */
public class ShutdownCommand(
    private val onShutdown: () -> Unit
) : Command {
    override val permission: String? = null // console only, checked manually
    override val description: String = "Shutdown proxy (console only)"
    override val usage: String = "[<reason>]"

    private val mini = MiniMessage.miniMessage()
    private val gson = GsonComponentSerializer.gson()

    override fun execute(source: CommandSource, args: Array<String>) {
        if (!source.isConsole) {
            source.sendMessage(Component.text("This command can only be executed from console", NamedTextColor.RED))
            return
        }
        val reasonRaw = if (args.isEmpty()) null else args.joinToString(" ")
        val reasonComponent: Component? = reasonRaw?.let { parseReason(it) }

        val msg = if (reasonComponent != null) {
            Component.text("Shutting down: ", NamedTextColor.YELLOW).append(reasonComponent)
        } else {
            Component.text("Shutting down...", NamedTextColor.YELLOW)
        }
        source.sendMessage(msg)
        // give a tick to flush messages, then stop
        try {
            onShutdown()
        } catch (_: Exception) {
            // ignore
        }
    }

    private fun parseReason(raw: String): Component {
        val trimmed = raw.trim()
        return if (trimmed.startsWith("\"") || trimmed.startsWith("[") || trimmed.startsWith("{")) {
            try {
                gson.deserialize(trimmed)
            } catch (_: Exception) {
                mini.deserialize(trimmed)
            }
        } else {
            mini.deserialize(trimmed)
        }
    }
}
