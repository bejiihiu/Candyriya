package kz.bejiihiu.candyriya.permission

import java.util.concurrent.ConcurrentHashMap

/**
 * Wildcard-aware resolver with negation support.
 * Rules:
 * - exact "foo.bar" beats wildcard "foo.*"
 * - "-foo.bar" means deny even if wildcard would allow
 * - longest matching prefix wins among wildcards
 * - Special "*" matches everything
 */
public class WildcardResolver(
    permissions: Set<String>
) : PermissionFunction {

    private val permissions: Set<String> = permissions.map { it.lowercase() }.toSet()

    // tiny cache per resolver instance — per-subject, so safe to keep
    private val cache = ConcurrentHashMap<String, Tristate>()

    override fun getPermissionValue(permission: String): Tristate {
        val normalized = permission.lowercase().trim()
        if (normalized.isEmpty()) return Tristate.UNDEFINED
        return cache.computeIfAbsent(normalized) { resolveUncached(it) }
    }

    private fun resolveUncached(permission: String): Tristate {
        // direct deny/allow check first (including negated form)
        // if permission set contains "-foo.bar", that is explicit FALSE for "foo.bar"
        if (permissions.contains("-$permission")) return Tristate.FALSE
        if (permissions.contains(permission)) return Tristate.TRUE

        // check wildcards — find longest matching wildcard
        var bestLen = -1
        var bestValue: Tristate? = null
        for (raw in permissions) {
            val isNegated = raw.startsWith("-")
            val perm = if (isNegated) raw.substring(1) else raw
            if (!perm.endsWith(".*") && perm != "*") continue
            val prefix = if (perm == "*") "" else perm.dropLast(2) // "foo.*" -> "foo"
            val matches = if (prefix.isEmpty()) {
                true
            } else {
                permission == prefix || permission.startsWith("$prefix.")
            }
            if (!matches) continue
            // longer prefix wins
            val len = prefix.length
            if (len > bestLen) {
                bestLen = len
                bestValue = if (isNegated) Tristate.FALSE else Tristate.TRUE
            }
        }
        return bestValue ?: Tristate.UNDEFINED
    }

    public fun invalidate() {
        cache.clear()
    }
}
