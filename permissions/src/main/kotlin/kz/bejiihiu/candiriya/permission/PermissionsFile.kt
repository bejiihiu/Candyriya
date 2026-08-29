package kz.bejiihiu.candiriya.permission

import com.electronwill.nightconfig.core.file.CommentedFileConfig
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import org.apache.logging.log4j.LogManager

/**
 * Simple TOML file for permissions.
 * Format:
 * [groups.default]
 * permissions = ["candiriya.command.help"]
 * parents = []
 * default = true
 *
 * [groups.op]
 * permissions = ["candiriya.*"]
 * parents = ["default"]
 *
 * [users."550e8400-..."]
 * groups = ["op"]
 * permissions = ["some.extra"]
 *
 * [ops]
 * list = ["550e8400-...", ...]
 */
public object PermissionsFile {
    private val logger = LogManager.getLogger(PermissionsFile::class.java)

    public fun load(path: Path): PermissionsFileData {
        if (!Files.exists(path)) {
            logger.info("permissions file not found at {}, using defaults", path)
            return PermissionsFileData()
        }
        val config = CommentedFileConfig.builder(path).build()
        config.load()
        val groups = mutableMapOf<String, PermissionGroup>()
        val ops = mutableSetOf<UUID>()
        val users = mutableMapOf<UUID, UserPermissions>()

        // groups — iterate valueMap
        val groupsMap = config.valueMap()["groups"]
        if (groupsMap is Map<*, *>) {
            for (key in groupsMap.keys) {
                val name = key.toString()
                val perms = config.getOrElse<List<String>>("groups.$name.permissions", emptyList())
                val parents = config.getOrElse<List<String>>("groups.$name.parents", emptyList())
                val isDefault = config.getOrElse<Boolean>("groups.$name.default", false)
                groups[name.lowercase()] = PermissionGroup(
                    name = name,
                    permissions = perms.toSet(),
                    parents = parents.map { it.lowercase() }.toSet(),
                    isDefault = isDefault
                )
            }
        }

        // ops
        val opsList = config.getOrElse<List<String>>("ops.list", emptyList())
            .ifEmpty { config.getOrElse<List<String>>("ops", emptyList()) }
        for (s in opsList) {
            try {
                ops.add(UUID.fromString(s))
            } catch (_: Exception) {
                logger.warn("invalid op uuid {}", s)
            }
        }

        // users
        val usersMap = config.valueMap()["users"]
        if (usersMap is Map<*, *>) {
            for (key in usersMap.keys) {
                val raw = key.toString()
                val clean = raw.removeSurrounding("\"")
                val uuid = try {
                    UUID.fromString(clean)
                } catch (_: Exception) {
                    logger.warn("invalid user uuid {}", raw)
                    continue
                }
                val g = config.getOrElse<List<String>>("users.\"$clean\".groups", emptyList())
                val p = config.getOrElse<List<String>>("users.\"$clean\".permissions", emptyList())
                users[uuid] = UserPermissions(g.map { it.lowercase() }.toSet(), p.toSet())
            }
        }

        config.close()
        return PermissionsFileData(groups, ops, users)
    }

    public fun save(path: Path, data: PermissionsFileData) {
        path.parent?.let { Files.createDirectories(it) }
        val config = CommentedFileConfig.builder(path).build()
        // clear is not needed — new file will be overwritten
        for ((name, group) in data.groups) {
            config.set<List<String>>("groups.$name.permissions", group.permissions.toList())
            config.set<List<String>>("groups.$name.parents", group.parents.toList())
            config.set<Boolean>("groups.$name.default", group.isDefault)
        }
        config.set<List<String>>("ops.list", data.ops.map { it.toString() })

        for ((uuid, up) in data.users) {
            val key = uuid.toString()
            config.set<List<String>>("users.\"$key\".groups", up.groups.toList())
            config.set<List<String>>("users.\"$key\".permissions", up.permissions.toList())
        }

        config.save()
        config.close()
    }

    /** Write default file if missing. */
    public fun ensureExists(path: Path) {
        if (Files.exists(path)) return
        val defaults = PermissionsFileData(
            groups = mapOf(
                "default" to PermissionGroup(
                    "default",
                    setOf(
                        "candiriya.command.help",
                        "candiriya.command.info",
                        "candiriya.command.server"
                    ),
                    emptySet(),
                    true
                ),
                "op" to PermissionGroup("op", setOf("candiriya.*"), setOf("default"), false)
            ),
            ops = emptySet(),
            users = emptyMap()
        )
        save(path, defaults)
    }
}
