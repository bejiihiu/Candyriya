@file:Suppress("ktlint")

package kz.bejiihiu.candyriya.scheduler.context

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kz.bejiihiu.candyriya.config.ProxyConfig
import kz.bejiihiu.candyriya.scheduler.threads.ThreadController
import org.apache.logging.log4j.LogManager

/**
 * Holds N [ExecutionContext]s and assigns players to them.
 * Like Folia's region mapping: Player A+B -> ctx1, C -> ctx2 etc.
 *
 * Assignment strategies:
 * - hash(uuid) % N — deterministic, sticky per player
 * - round-robin — for load spread
 * - explicit — caller passes contextId
 */
@SuppressFBWarnings(
    value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2", "BC_BAD_CAST_TO_ABSTRACT_COLLECTION", "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE"],
    justification = "contexts list is intentionally exposed as snapshot, false positives xd"
)
public class ContextRegistry(
    private val config: ProxyConfig,
    private val threadController: ThreadController
) : AutoCloseable {

    private val logger = LogManager.getLogger(ContextRegistry::class.java)

    private val contexts: List<ExecutionContext>
    private val roundRobin = AtomicInteger(0)

    // player uuid -> context id, for observability / migration
    private val bindings = ConcurrentHashMap<UUID, Int>()

    init {
        val n = resolveCount(config.scheduler.contexts)
        contexts = (0 until n).map { id ->
            ExecutionContext(id, threadController.scheduledPool)
        }
        logger.info("ContextRegistry created with {} contexts (tick={}ms)", n, config.scheduler.tickRateMs)
    }

    private fun resolveCount(requested: Int): Int {
        if (requested > 0) return requested.coerceIn(1, 32)
        // 0 = cpu count like NetworkServer workers
        val cpu = Runtime.getRuntime().availableProcessors()
        return cpu.coerceIn(2, 16)
    }

    public fun size(): Int = contexts.size

    public fun get(id: Int): ExecutionContext {
        require(id in contexts.indices) { "context id $id out of range 0..${contexts.size - 1}" }
        return contexts[id]
    }

    public fun all(): List<ExecutionContext> = contexts

    /** Deterministic assignment: hash(uuid) % N — sticky. */
    public fun assign(uuid: UUID): ExecutionContext {
        val id = Math.floorMod(uuid.hashCode(), contexts.size)
        bindings[uuid] = id
        logger.debug("assign {} -> ctx {}", uuid, id)
        return contexts[id]
    }

    /** Round-robin assignment — spreads load even if hash clusters. */
    public fun assignRoundRobin(uuid: UUID): ExecutionContext {
        val id = Math.floorMod(roundRobin.getAndIncrement(), contexts.size)
        bindings[uuid] = id
        logger.debug("assign rr {} -> ctx {}", uuid, id)
        return contexts[id]
    }

    /** Explicit assignment — caller knows target. */
    public fun assignTo(uuid: UUID, contextId: Int): ExecutionContext {
        require(contextId in contexts.indices)
        bindings[uuid] = contextId
        return contexts[contextId]
    }

    public fun getFor(uuid: UUID): ExecutionContext? {
        val id = bindings[uuid] ?: return null
        return contexts.getOrNull(id)
    }

    /** Migrate player to another context — next tasks will run there. */
    public fun migrate(uuid: UUID, targetId: Int): ExecutionContext {
        require(targetId in contexts.indices)
        val old = bindings[uuid]
        bindings[uuid] = targetId
        logger.info("migrate {} ctx {} -> {}", uuid, old, targetId)
        return contexts[targetId]
    }

    public fun unbind(uuid: UUID) {
        bindings.remove(uuid)
    }

    /** Execute runnable on player's context if bound, otherwise on async pool. */
    public fun executeFor(uuid: UUID, task: Runnable) {
        val ctx = getFor(uuid)
        if (ctx != null) {
            ctx.execute(task)
        } else {
            threadController.asyncPool.execute(task)
        }
    }

    public fun stats(): Map<Int, Int> {
        val counts = mutableMapOf<Int, Int>()
        for (id in bindings.values) {
            counts[id] = (counts[id] ?: 0) + 1
        }
        return counts
    }

    override fun close() {
        for (ctx in contexts) {
            try {
                ctx.close()
            } catch (e: Exception) {
                logger.warn("error closing ctx {}", ctx.id, e)
            }
        }
        bindings.clear()
        logger.info("ContextRegistry closed")
    }
}
