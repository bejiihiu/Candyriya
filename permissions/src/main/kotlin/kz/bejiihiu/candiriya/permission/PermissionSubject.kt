package kz.bejiihiu.candiriya.permission

/**
 * Anything that can have permissions — player, console, plugin.
 * Minimal surface so external providers (LuckPerms etc.) can wrap it.
 */
public interface PermissionSubject {
    /** Stable id for logging / storage (uuid string for player, "console" for console). */
    public val identifier: String

    public fun permissionValue(permission: String): Tristate

    public fun hasPermission(permission: String): Boolean = permissionValue(permission) == Tristate.TRUE
}
