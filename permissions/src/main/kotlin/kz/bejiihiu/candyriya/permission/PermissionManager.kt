package kz.bejiihiu.candyriya.permission

import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.apache.logging.log4j.LogManager

/**
 * Central permission manager.
 * Holds groups (default/op), user assignments, and provider SPI.
 * Thread-safe: groups are immutable snapshots, user map is concurrent.
 */
public class PermissionManager(
    private val configPath: Path? = null
) {
    private val logger = LogManager.getLogger(PermissionManager::class.java)

    // group name (lowercase) -> Group
    @Volatile
    private var groups: Map<String, PermissionGroup> = defaultGroups()

    // uuid -> set of group names + extra perms (from file or runtime)
    private val userGroups = ConcurrentHashMap<UUID, MutableSet<String>>()
    private val userPerms = ConcurrentHashMap<UUID, MutableSet<String>>()
    private val userResolverCache = ConcurrentHashMap<UUID, WildcardResolver>()

    // op set — uuids that get "*" regardless of groups
    private val ops = ConcurrentHashMap.newKeySet<UUID>()

    @Volatile
    private var provider: PermissionProvider? = null

    /**
     * Override provider for external plugins like LuckPerms.
     * If set, all permission checks delegate to it.
     */
    public fun setProvider(provider: PermissionProvider?) {
        this.provider = provider
        // clear caches so new provider takes effect
        userResolverCache.clear()
        logger.info("permission provider set to {}", provider?.javaClass?.name ?: "internal")
    }

    public fun getProvider(): PermissionProvider? = provider

    public fun isOp(uuid: UUID): Boolean = ops.contains(uuid)

    public fun setOp(uuid: UUID, op: Boolean) {
        if (op) ops.add(uuid) else ops.remove(uuid)
        userResolverCache.remove(uuid)
    }

    public fun getGroups(): Map<String, PermissionGroup> = groups

    public fun getGroup(name: String): PermissionGroup? = groups[name.lowercase()]

    public fun setGroups(newGroups: Map<String, PermissionGroup>) {
        groups = newGroups.mapKeys { it.key.lowercase() }
        userResolverCache.clear()
    }

    public fun getUserGroups(uuid: UUID): Set<String> = userGroups[uuid]?.toSet() ?: emptySet()

    public fun setUserGroups(uuid: UUID, groupNames: Collection<String>) {
        userGroups[uuid] = groupNames.map { it.lowercase() }.toMutableSet()
        userResolverCache.remove(uuid)
    }

    public fun addUserGroup(uuid: UUID, group: String) {
        userGroups.computeIfAbsent(uuid) { mutableSetOf() }.add(group.lowercase())
        userResolverCache.remove(uuid)
    }

    public fun removeUserGroup(uuid: UUID, group: String) {
        userGroups[uuid]?.remove(group.lowercase())
        userResolverCache.remove(uuid)
    }

    public fun addUserPermission(uuid: UUID, permission: String) {
        userPerms.computeIfAbsent(uuid) { mutableSetOf() }.add(permission)
        userResolverCache.remove(uuid)
    }

    public fun removeUserPermission(uuid: UUID, permission: String) {
        userPerms[uuid]?.remove(permission)
        userResolverCache.remove(uuid)
    }

    /**
     * Main entry: compute permission value for a subject.
     * If external provider present, delegate; otherwise use internal wildcard logic.
     */
    public fun permissionValue(subject: PermissionSubject, permission: String): Tristate {
        // console always true — checked via subject type, but also fallback here
        if (subject is ConsoleSubject) return Tristate.TRUE

        val ext = provider
        if (ext != null) {
            return ext.createFunction(subject).getPermissionValue(permission)
        }

        // try to resolve via uuid if subject has one
        val uuid = (subject as? PlayerSubject)?.uuid
        if (uuid != null) {
            if (ops.contains(uuid)) return Tristate.TRUE
            val resolver = userResolverCache.computeIfAbsent(uuid) { buildResolver(it) }
            val v = resolver.getPermissionValue(permission)
            if (v != Tristate.UNDEFINED) return v
            // fall through to group defaults already merged in resolver, so undefined means no perm
            return Tristate.FALSE
        }

        // generic subject without uuid — just check direct perms if any
        return Tristate.FALSE
    }

    private fun buildResolver(uuid: UUID): WildcardResolver {
        val perms = mutableSetOf<String>()
        // collect group perms (with inheritance)
        val assigned = userGroups[uuid] ?: emptySet()
        val effectiveGroups = if (assigned.isEmpty()) {
            // default group if none assigned
            setOf("default")
        } else {
            assigned
        }

        val visited = mutableSetOf<String>()
        fun collectGroup(name: String) {
            val lower = name.lowercase()
            if (!visited.add(lower)) return
            val g = groups[lower] ?: return
            perms.addAll(g.permissions)
            for (parent in g.parents) collectGroup(parent)
        }
        for (g in effectiveGroups) collectGroup(g)
        // add user extra perms (exact overrides)
        userPerms[uuid]?.let { perms.addAll(it) }
        return WildcardResolver(perms)
    }

    public fun loadFromFile(path: Path) {
        try {
            val loaded = PermissionsFile.load(path)
            setGroups(loaded.groups)
            ops.clear()
            ops.addAll(loaded.ops)
            userGroups.clear()
            userPerms.clear()
            for ((uuid, data) in loaded.users) {
                if (data.groups.isNotEmpty()) userGroups[uuid] = data.groups.toMutableSet()
                if (data.permissions.isNotEmpty()) userPerms[uuid] = data.permissions.toMutableSet()
            }
            userResolverCache.clear()
            logger.info("loaded permissions from {} groups={} users={} ops={}", path, groups.size, loaded.users.size, ops.size)
        } catch (e: Exception) {
            logger.error("failed to load permissions from {}", path, e)
        }
    }

    public fun saveToFile(path: Path) {
        try {
            val data = PermissionsFileData(
                groups = groups,
                ops = ops.toSet(),
                users = userGroups.mapValues { (uuid, groups) ->
                    UserPermissions(
                        groups = groups.toSet(),
                        permissions = userPerms[uuid]?.toSet() ?: emptySet()
                    )
                } + userPerms.filterKeys { uuid -> !userGroups.containsKey(uuid) }.mapValues { (_, perms) ->
                    UserPermissions(emptySet(), perms.toSet())
                }
            )
            PermissionsFile.save(path, data)
            logger.info("saved permissions to {}", path)
        } catch (e: Exception) {
            logger.error("failed to save permissions to {}", path, e)
        }
    }

    private fun defaultGroups(): Map<String, PermissionGroup> = mapOf(
        "default" to PermissionGroup(
            name = "default",
            // server/help/info granted to all by default
            permissions = setOf(
                "candyriya.command.help",
                "candyriya.command.info",
                "candyriya.command.server"
            ),
            parents = emptySet(),
            isDefault = true
        ),
        "op" to PermissionGroup(
            name = "op",
            permissions = setOf("candyriya.*"),
            parents = setOf("default"),
            isDefault = false
        )
    )
}
