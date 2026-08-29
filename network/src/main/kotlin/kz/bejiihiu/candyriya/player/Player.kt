package kz.bejiihiu.candyriya.player

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kz.bejiihiu.candyriya.network.session.ProxySession
import kz.bejiihiu.candyriya.scheduler.context.ExecutionContext
import org.apache.logging.log4j.LogManager

/**
 * Normal player model from the spec.
 *
 * ```kotlin
 * class Player {
 *   val uuid: UUID
 *   val username: String
 *   val connection: Connection
 *   var server: Server?
 *   var state: PlayerState
 * }
 * ```
 *
 * Concurrency: all mutations via [context] thread. Reads of [uuid]/[username]/[connection] are safe from any thread.
 * State is AtomicReference + validated transitions.
 */
@SuppressFBWarnings(
    value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2", "RpC_REPEATED_CONDITIONAL_TEST"],
    justification = "player fields intentionally exposed xd"
)
public class Player(
    public val uuid: UUID,
    public val username: String,
    public val connection: PlayerConnection,
    public val context: ExecutionContext,
    initialState: PlayerState = PlayerState.HANDSHAKE,
    public var server: RegisteredServer? = null,
    // tie to existing ProxySession during migration — will be removed once fully ported
    public val session: ProxySession? = null
) {
    private val logger = LogManager.getLogger(Player::class.java)

    private val stateRef = AtomicReference(initialState)

    public var state: PlayerState
        get() = stateRef.get()
        set(value) {
            // direct set only for trusted context thread — validates
            transitionTo(value)
        }

    public fun snapshotState(): PlayerState = stateRef.get()

    /** CAS transition — thread-safe, validates. */
    public fun compareAndSet(expected: PlayerState, newState: PlayerState): Boolean {
        if (!PlayerStateTransitions.can(expected, newState)) {
            logger.warn("blocked invalid CAS {} -> {} for {}", expected, newState, username)
            return false
        }
        val ok = stateRef.compareAndSet(expected, newState)
        if (ok) logger.debug("player {}: {} -> {} (CAS)", username, expected, newState)
        return ok
    }

    /** Force transition with validation — must be called on context thread ideally. */
    public fun transitionTo(newState: PlayerState): Boolean {
        val old = stateRef.get()
        if (old == newState) return true
        if (old == PlayerState.DISCONNECTED || old == PlayerState.DISCONNECTING && newState != PlayerState.DISCONNECTED) {
            // allow anything -> DISCONNECTED, but log weird
            if (newState != PlayerState.DISCONNECTED) {
                logger.debug("ignoring transition {} -> {} already {}", old, newState, username)
                return false
            }
        }
        if (!PlayerStateTransitions.can(old, newState)) {
            // be lenient like Velocity — allow but warn xd
            logger.warn("weird transition {} -> {} for {} (allowing)", old, newState, username)
        }
        stateRef.set(newState)
        logger.debug("player {}: {} -> {}", username, old, newState)
        return true
    }

    /** Ensure caller is on player's context thread — like Folia isOwnedByCurrentRegion. */
    public fun ensureOnContext() {
        check(context.isOwnedByCurrentThread()) {
            "player $username not on its context ${context.id} thread, current=${Thread.currentThread().name}"
        }
    }

    public fun isOnContext(): Boolean = context.isOwnedByCurrentThread()

    /** Submit task to player's context — the main way to do async then hop back. */
    public fun execute(task: Runnable): Unit = context.execute(task)

    public fun disconnect(reason: net.kyori.adventure.text.Component? = null) {
        transitionTo(PlayerState.DISCONNECTING)
        connection.disconnect(reason)
        transitionTo(PlayerState.DISCONNECTED)
    }

    override fun toString(): String = "Player($username/$uuid ctx=${context.id} state=${stateRef.get()} server=${server?.name})"
}
