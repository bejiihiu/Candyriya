package kz.bejiihiu.candyriya.permission

import java.util.UUID

/**
 * Console subject — always has all permissions.
 * Used to distinguish console from players (requirement: "console must be console").
 */
public object ConsoleSubject : PermissionSubject {
    override val identifier: String = "console"

    override fun permissionValue(permission: String): Tristate = Tristate.TRUE

    override fun hasPermission(permission: String): Boolean = true
}

/**
 * Wraps a Player as PermissionSubject delegating to PermissionManager.
 * Keeps permissions module free of network dependency.
 */
public class PlayerSubject(
    public val uuid: UUID,
    public val username: String,
    private val manager: PermissionManager
) : PermissionSubject {
    override val identifier: String get() = uuid.toString()
    override fun permissionValue(permission: String): Tristate = manager.permissionValue(this, permission)
}
