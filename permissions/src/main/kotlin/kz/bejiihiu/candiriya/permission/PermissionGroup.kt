package kz.bejiihiu.candiriya.permission

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings

/**
 * Permission group/role.
 * Inherits permissions from parents recursively.
 */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2"], justification = "immutable sets, no external mutation xd")
public data class PermissionGroup(
    val name: String,
    val permissions: Set<String> = emptySet(),
    val parents: Set<String> = emptySet(),
    val isDefault: Boolean = false
)

@SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2"], justification = "immutable sets xd")
public data class UserPermissions(
    val groups: Set<String> = emptySet(),
    val permissions: Set<String> = emptySet()
)

@SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2"], justification = "immutable sets xd")
public data class PermissionsFileData(
    val groups: Map<String, PermissionGroup> = emptyMap(),
    val ops: Set<java.util.UUID> = emptySet(),
    val users: Map<java.util.UUID, UserPermissions> = emptyMap()
)
