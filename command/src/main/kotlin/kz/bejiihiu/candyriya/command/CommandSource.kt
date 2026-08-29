package kz.bejiihiu.candyriya.command

import java.util.UUID
import kz.bejiihiu.candyriya.permission.ConsoleSubject
import kz.bejiihiu.candyriya.permission.PermissionManager
import kz.bejiihiu.candyriya.permission.PermissionSubject
import kz.bejiihiu.candyriya.permission.PlayerSubject
import kz.bejiihiu.candyriya.permission.Tristate
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.identity.Identity
import net.kyori.adventure.text.Component

/**
 * Command source — who ran the command.
 * Combines PermissionSubject + Audience so commands can do `source.sendMessage` and `source.hasPermission`.
 * Console is distinguishable via `isConsole`.
 */
public interface CommandSource : PermissionSubject, Audience {
    public val name: String
    public val isConsole: Boolean

    override fun sendMessage(source: Identity, message: Component, type: net.kyori.adventure.audience.MessageType) {
        sendMessage(message)
    }
}

/**
 * Console source — has all permissions, writes to logger / stdout.
 */
public class ConsoleSource(
    private val audience: Audience? = null,
    private val onMessage: ((Component) -> Unit)? = null
) : CommandSource {
    override val name: String = "Console"
    override val isConsole: Boolean = true
    override val identifier: String = ConsoleSubject.identifier

    override fun permissionValue(permission: String): Tristate = Tristate.TRUE
    override fun hasPermission(permission: String): Boolean = true

    override fun sendMessage(message: Component) {
        if (onMessage != null) {
            onMessage.invoke(message)
            return
        }
        audience?.sendMessage(message) ?: run {
            // fallback without plain serializer to avoid extra dep
            println("[Console] $message")
        }
    }
}

/**
 * Player source — delegates permission checks to PermissionManager.
 * Decoupled from network Player type to avoid module cycle.
 */
public class PlayerSource(
    private val playerId: UUID,
    private val playerName: String,
    private val permissionManager: PermissionManager,
    private val onMessage: ((Component) -> Unit)? = null
) : CommandSource {
    private val subject = PlayerSubject(playerId, playerName, permissionManager)
    override val name: String get() = playerName
    override val isConsole: Boolean = false
    override val identifier: String get() = subject.identifier

    override fun permissionValue(permission: String): Tristate = permissionManager.permissionValue(subject, permission)

    override fun hasPermission(permission: String): Boolean = permissionManager.permissionValue(subject, permission) == Tristate.TRUE

    override fun sendMessage(message: Component) {
        if (onMessage != null) {
            onMessage.invoke(message)
            return
        }
        try {
            org.apache.logging.log4j.LogManager.getLogger(PlayerSource::class.java).info("[to {}] {}", name, message)
        } catch (_: Exception) {
            // ignore
        }
    }

    public fun uuid(): UUID = playerId

    public companion object {
        /**
         * Convenience factory from network Player (via reflection to avoid hard dep).
         * If you have a Player instance, call this instead of constructor.
         */
        public fun fromPlayer(player: Any, permissionManager: PermissionManager, onMessage: ((Component) -> Unit)? = null): PlayerSource {
            val clazz = player.javaClass
            val uuid = clazz.getMethod("getUuid").invoke(player) as UUID
            val name = clazz.getMethod("getUsername").invoke(player) as String
            return PlayerSource(uuid, name, permissionManager, onMessage)
        }
    }
}
