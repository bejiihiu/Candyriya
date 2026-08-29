package kz.bejiihiu.candyriya.player

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference
import kz.bejiihiu.candyriya.network.session.ProxySession
import kz.bejiihiu.candyriya.scheduler.context.ExecutionContext
import kz.bejiihiu.candyriya.server.ConnectionRequest
import kz.bejiihiu.candyriya.server.ConnectionResult
import kz.bejiihiu.candyriya.server.RegisteredServer
import kz.bejiihiu.candyriya.server.ServerRegistry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.apache.logging.log4j.LogManager

@SuppressFBWarnings(
    value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2", "RpC_REPEATED_CONDITIONAL_TEST"],
    justification = "player fields intentional"
)
public class Player(
    public val uuid: UUID,
    public val username: String,
    public val connection: PlayerConnection,
    public val context: ExecutionContext,
    initialState: PlayerState = PlayerState.HANDSHAKE,
    public var server: RegisteredServer? = null,
    public val session: ProxySession? = null,
    private var serverRegistry: ServerRegistry? = null
) {
    private val logger = LogManager.getLogger(Player::class.java)
    private val stateRef = AtomicReference(initialState)

    public var state: PlayerState
        get() = stateRef.get()
        set(value) {
            transitionTo(value)
        }

    public fun snapshotState(): PlayerState = stateRef.get()

    public fun compareAndSet(expected: PlayerState, newState: PlayerState): Boolean {
        if (!PlayerStateTransitions.can(expected, newState)) {
            logger.warn("blocked invalid CAS {} -> {} for {}", expected, newState, username)
            return false
        }
        val ok = stateRef.compareAndSet(expected, newState)
        if (ok) logger.debug("player {}: {} -> {} (CAS)", username, expected, newState)
        return ok
    }

    public fun transitionTo(newState: PlayerState): Boolean {
        val old = stateRef.get()
        if (old == newState) return true
        if (old == PlayerState.DISCONNECTED || old == PlayerState.DISCONNECTING && newState != PlayerState.DISCONNECTED) {
            if (newState != PlayerState.DISCONNECTED) {
                logger.debug("ignoring transition {} -> {} already {}", old, newState, username)
                return false
            }
        }
        if (!PlayerStateTransitions.can(old, newState)) {
            logger.warn("weird transition {} -> {} for {} (allowing)", old, newState, username)
        }
        stateRef.set(newState)
        logger.debug("player {}: {} -> {}", username, old, newState)
        return true
    }

    public fun ensureOnContext() {
        check(context.isOwnedByCurrentThread()) {
            "player $username not on its context ${context.id} thread, current=${Thread.currentThread().name}"
        }
    }

    public fun isOnContext(): Boolean = context.isOwnedByCurrentThread()

    public fun execute(task: Runnable): Unit = context.execute(task)

    public fun disconnect(reason: Component? = null) {
        transitionTo(PlayerState.DISCONNECTING)
        connection.disconnect(reason)
        transitionTo(PlayerState.DISCONNECTED)
    }

    public fun setServerRegistry(registry: ServerRegistry) {
        serverRegistry = registry
    }

    public fun getServerRegistry(): ServerRegistry? = serverRegistry

    // --- connect API ---

    public fun connect(serverName: String): CompletableFuture<ConnectionResult> {
        val reg = serverRegistry
        if (reg == null) {
            val f = CompletableFuture<ConnectionResult>()
            f.completeExceptionally(IllegalStateException("server registry not set for player $username"))
            return f
        }
        val target = reg.get(serverName)
        if (target == null) {
            return CompletableFuture.completedFuture(
                ConnectionResult(
                    ConnectionResult.Status.SERVER_DISCONNECTED,
                    Component.text("Server not found: $serverName", NamedTextColor.RED),
                    null
                )
            )
        }
        return connect(target)
    }

    public fun connect(target: RegisteredServer): CompletableFuture<ConnectionResult> = createConnectionRequest(target).connect()

    public fun createConnectionRequest(target: RegisteredServer): ConnectionRequest {
        return PlayerConnectionRequest(this, target, serverRegistry)
    }

    override fun toString(): String = "Player($username/$uuid ctx=${context.id} state=${stateRef.get()} server=${server?.name})"
}

/**
 * Default ConnectionRequest impl. Runs on player's ExecutionContext thread,
 * uses session's netty channel to open a new BackendConnection.
 */
@SuppressFBWarnings(
    value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2", "DE_MIGHT_IGNORE", "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", "REC_CATCH_EXCEPTION"],
    justification = "request holder xd"
)
internal class PlayerConnectionRequest(
    private val player: Player,
    private val target: RegisteredServer,
    private val registry: ServerRegistry?
) : ConnectionRequest {
    private val logger = LogManager.getLogger(PlayerConnectionRequest::class.java)

    override fun getServer(): RegisteredServer = target

    override fun connect(): CompletableFuture<ConnectionResult> {
        val future = CompletableFuture<ConnectionResult>()
        // must run on player's context
        if (!player.isOnContext()) {
            player.context.execute { doConnect(future) }
        } else {
            doConnect(future)
        }
        return future
    }

    override fun connectWithIndication(): CompletableFuture<Boolean> {
        return connect().thenApply { result ->
            if (!result.isSuccess) {
                val reason = result.reason ?: Component.text("Could not connect to ${target.name}", NamedTextColor.RED)
                player.connection.disconnect(reason)
            }
            result.isSuccess
        }
    }

    override fun fireAndForget() {
        connectWithIndication()
    }

    private fun doConnect(future: CompletableFuture<ConnectionResult>) {
        // already on same server?
        if (player.server != null && player.server!!.name.equals(target.name, ignoreCase = true)) {
            future.complete(ConnectionResult(ConnectionResult.Status.ALREADY_CONNECTED, null, target))
            return
        }
        // availability check
        if (registry != null && !registry.isAvailable(target)) {
            future.complete(
                ConnectionResult(
                    ConnectionResult.Status.SERVER_DISCONNECTED,
                    Component.text("Server ${target.name} is currently unavailable", NamedTextColor.RED),
                    target
                )
            )
            return
        }
        val session = player.session
        if (session == null || session.clientChannel == null || !session.clientChannel!!.isActive) {
            future.complete(
                ConnectionResult(
                    ConnectionResult.Status.SERVER_DISCONNECTED,
                    Component.text("Player session not active", NamedTextColor.RED),
                    target
                )
            )
            return
        }
        val clientChannel = session.clientChannel!!
        // close old backend gracefully before new connect
        val oldBackend = session.backendChannel
        try {
            if (oldBackend != null && oldBackend.isOpen) {
                // keep client, close backend only
                session.closeBackendOnly("switching to ${target.name}")
            }
        } catch (_: Exception) {}

        // prepare new backend connection
        val backendConn = kz.bejiihiu.candyriya.network.BackendConnection(session, session.config, target)
        backendConn.connect(clientChannel, { _ ->
            // success on netty thread, hop back to context
            val task = Runnable {
                player.server = target
                session.currentServer = target
                registry?.markAvailable(target)
                if (player.state == PlayerState.CONNECTING) player.transitionTo(PlayerState.PLAYING)
                // drain any queued packets
                try {
                    session.drainQueueToBackend()
                } catch (_: Exception) {}
                future.complete(ConnectionResult(ConnectionResult.Status.SUCCESS, null, target))
            }
            if (player.isOnContext()) task.run() else player.context.execute(task)
        }, { cause ->
            registry?.markUnavailable(target)
            val task = Runnable {
                future.complete(
                    ConnectionResult(
                        ConnectionResult.Status.SERVER_DISCONNECTED,
                        Component.text("Could not connect to ${target.name}: ${cause.message ?: "unknown"}", NamedTextColor.RED),
                        target
                    )
                )
            }
            if (player.isOnContext()) task.run() else player.context.execute(task)
        })
    }
}


