package kz.bejiihiu.candyriya.server

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import java.util.concurrent.ConcurrentHashMap
import org.apache.logging.log4j.LogManager

/**
 * Thread-safe registry of backend servers.
 * Holds [RegisteredServer] list, try-order, and passive availability.
 *
 * Passive health: on connect failure we mark server as unavailable for [unavailableCooldownMs].
 * Active ping can be added via [markAvailable]/[markUnavailable] from a scheduled checker.
 */
@SuppressFBWarnings(
    value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2", "BC_BAD_CAST_TO_ABSTRACT_COLLECTION"],
    justification = "registry intentionally exposes xd"
)
public class ServerRegistry(
    servers: Map<String, RegisteredServer> = emptyMap(),
    tryOrder: List<String> = emptyList(),
    private val unavailableCooldownMs: Long = 5000
) {
    private val logger = LogManager.getLogger(ServerRegistry::class.java)

    private val byName: ConcurrentHashMap<String, RegisteredServer> = ConcurrentHashMap()
    private val tryNames: MutableList<String> = mutableListOf()

    // passive unavailable until timestamp
    private val unavailableUntil: ConcurrentHashMap<String, Long> = ConcurrentHashMap()

    init {
        for ((k, v) in servers) {
            byName[k.lowercase()] = v
        }
        // validate try order
        for (n in tryOrder) {
            if (byName.containsKey(n.lowercase())) {
                tryNames.add(n)
            } else {
                logger.warn("try server '{}' not found in servers map, ignoring", n)
            }
        }
        // if try empty but we have servers, default to first
        if (tryNames.isEmpty() && byName.isNotEmpty()) {
            tryNames.add(byName.values.first().name)
        }
    }

    public fun get(name: String): RegisteredServer? = byName[name.lowercase()]

    public fun all(): Collection<RegisteredServer> = byName.values.toList()

    public fun names(): Set<String> = byName.values.map { it.name }.toSet()

    public fun tryServers(): List<RegisteredServer> = tryNames.mapNotNull { byName[it.lowercase()] }

    public fun defaultServer(): RegisteredServer? = tryServers().firstOrNull() ?: byName.values.firstOrNull()

    /**
     * Next fallback after [exclude], skipping unavailable.
     * Traverses [tryServers] in order, skips [exclude].
     */
    public fun fallbackFor(exclude: RegisteredServer?): RegisteredServer? {
        val list = tryServers()
        if (list.isEmpty()) return null
        for (s in list) {
            if (exclude != null && s.name.equals(exclude.name, ignoreCase = true)) continue
            if (!isAvailable(s)) continue
            return s
        }
        return null
    }

    /** All available servers from try list, optionally excluding one. */
    public fun availableFallbacks(exclude: RegisteredServer? = null): List<RegisteredServer> = tryServers().filter { s ->
        (exclude == null || !s.name.equals(exclude.name, ignoreCase = true)) && isAvailable(s)
    }

    public fun register(server: RegisteredServer) {
        require(server.name.matches(Regex("^[a-zA-Z0-9_-]{1,16}$"))) { "invalid server name '${server.name}'" }
        require(server.port in 1..65535) { "port out of range ${server.port}" }
        byName[server.name.lowercase()] = server
        logger.info("registered server {} -> {}", server.name, server.address())
    }

    public fun unregister(name: String): RegisteredServer? {
        val removed = byName.remove(name.lowercase())
        tryNames.removeIf { it.equals(name, ignoreCase = true) }
        unavailableUntil.remove(name.lowercase())
        if (removed != null) logger.info("unregistered server {}", name)
        return removed
    }

    public fun update(servers: Map<String, RegisteredServer>, tryOrder: List<String>) {
        byName.clear()
        tryNames.clear()
        unavailableUntil.clear()
        for ((k, v) in servers) byName[k.lowercase()] = v
        for (n in tryOrder) if (byName.containsKey(n.lowercase())) tryNames.add(n)
        if (tryNames.isEmpty() && byName.isNotEmpty()) tryNames.add(byName.values.first().name)
        logger.info("registry updated: servers={} try={}", byName.keys, tryNames)
    }

    public fun isAvailable(server: RegisteredServer): Boolean {
        val until = unavailableUntil[server.name.lowercase()] ?: return true
        if (System.currentTimeMillis() > until) {
            unavailableUntil.remove(server.name.lowercase())
            return true
        }
        return false
    }

    public fun isAvailable(name: String): Boolean {
        val s = get(name) ?: return false
        return isAvailable(s)
    }

    public fun markUnavailable(server: RegisteredServer, cooldownMs: Long = unavailableCooldownMs) {
        unavailableUntil[server.name.lowercase()] = System.currentTimeMillis() + cooldownMs
        logger.warn("marked server {} unavailable for {}ms", server.name, cooldownMs)
    }

    public fun markUnavailable(name: String, cooldownMs: Long = unavailableCooldownMs) {
        get(name)?.let { markUnavailable(it, cooldownMs) }
    }

    public fun markAvailable(server: RegisteredServer) {
        unavailableUntil.remove(server.name.lowercase())
        logger.info("marked server {} available", server.name)
    }

    public fun count(): Int = byName.size

    public fun toMap(): Map<String, RegisteredServer> = byName.values.associateBy { it.name }
}

