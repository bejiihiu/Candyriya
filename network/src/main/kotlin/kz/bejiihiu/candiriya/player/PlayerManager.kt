package kz.bejiihiu.candiriya.player

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kz.bejiihiu.candiriya.scheduler.context.ContextRegistry
import kz.bejiihiu.candiriya.server.RegisteredServer
import kz.bejiihiu.candiriya.server.ServerRegistry
import org.apache.logging.log4j.LogManager

/**
 * Thread-safe registry of online players.
 * Like Velocity's player map but sharded by context conceptually.
 */
@SuppressFBWarnings(
    value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2", "BC_BAD_CAST_TO_ABSTRACT_COLLECTION"],
    justification = "registry intentional xd"
)
public class PlayerManager(
    private val registry: ContextRegistry,
    private var serverRegistry: ServerRegistry? = null
) {
    private val logger = LogManager.getLogger(PlayerManager::class.java)

    private val byUuid = ConcurrentHashMap<UUID, Player>()
    private val byName = ConcurrentHashMap<String, Player>() // lowercased

    public fun create(
        uuid: UUID,
        username: String,
        connection: PlayerConnection,
        server: RegisteredServer? = null,
        strategy: AssignStrategy = AssignStrategy.HASH
    ): Player {
        val ctx = when (strategy) {
            AssignStrategy.HASH -> registry.assign(uuid)
            AssignStrategy.ROUND_ROBIN -> registry.assignRoundRobin(uuid)
        }
        val player = Player(uuid = uuid, username = username, connection = connection, context = ctx, server = server)
        serverRegistry?.let { player.setServerRegistry(it) }
        val prev = byUuid.putIfAbsent(uuid, player)
        check(prev == null) { "player $uuid already exists" }
        byName[username.lowercase()] = player
        logger.info("player created {} -> ctx {} (strategy={})", username, ctx.id, strategy)
        return player
    }

    /** For handshake stage where uuid not yet known — create with random uuid then replace on login. */
    public fun createPending(connection: PlayerConnection, strategy: AssignStrategy = AssignStrategy.ROUND_ROBIN): Player {
        val tmpUuid = UUID.randomUUID()
        return create(tmpUuid, "pending-${tmpUuid.toString().take(8)}", connection, strategy = strategy)
    }

    public fun get(uuid: UUID): Player? = byUuid[uuid]
    public fun getByName(name: String): Player? = byName[name.lowercase()]
    public fun all(): Collection<Player> = byUuid.values
    public fun count(): Int = byUuid.size

    public fun getByContext(contextId: Int): List<Player> = byUuid.values.filter { it.context.id == contextId }

    public fun remove(uuid: UUID): Player? {
        val p = byUuid.remove(uuid)
        if (p != null) {
            byName.remove(p.username.lowercase())
            registry.unbind(uuid)
            logger.info("player removed {} ctx {}", p.username, p.context.id)
        }
        return p
    }

    public fun remove(player: Player): Player? = remove(player.uuid)

    /** Migrate player to another context — future tasks run there. */
    public fun migrate(uuid: UUID, targetContextId: Int): Player? {
        val player = byUuid[uuid] ?: return null
        // we can't change Player.context val, so we rebind registry and log — real migration needs new Player wrapper
        // for now just rebind registry mapping; tasks via registry.executeFor will go to new ctx
        registry.migrate(uuid, targetContextId)
        logger.info("player {} migrated ctx {} -> {}", player.username, player.context.id, targetContextId)
        return player
    }

    public fun setServerRegistry(registry: ServerRegistry) {
        serverRegistry = registry
        // propagate to existing players
        for (p in byUuid.values) p.setServerRegistry(registry)
    }

    public fun getServerRegistry(): ServerRegistry? = serverRegistry

    public enum class AssignStrategy { HASH, ROUND_ROBIN }
}
