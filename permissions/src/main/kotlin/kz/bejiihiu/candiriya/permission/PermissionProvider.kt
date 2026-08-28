package kz.bejiihiu.candiriya.permission

/**
 * SPI for replacing permission resolution.
 * Default impl uses groups + wildcard. LuckPerms bridge would return its own function.
 */
public fun interface PermissionProvider {
    public fun createFunction(subject: PermissionSubject): PermissionFunction
}
