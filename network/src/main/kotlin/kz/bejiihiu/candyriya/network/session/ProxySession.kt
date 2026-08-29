package kz.bejiihiu.candyriya.network.session

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import java.security.KeyPair
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kz.bejiihiu.candyriya.config.ProxyConfig
import kz.bejiihiu.candyriya.protocol.ConnectionState
import kz.bejiihiu.candyriya.protocol.EncryptionUtil
import kz.bejiihiu.candyriya.protocol.MinecraftPacket
import org.apache.logging.log4j.LogManager

/**
 * Holds both sides of the proxy: client <-> proxy <-> backend.
 * One session per player connection, so no crazy concurrency — but we still guard close().
 */
@SuppressFBWarnings(
    value = ["DLS_DEAD_LOCAL_STORE", "DE_MIGHT_IGNORE", "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE"],
    justification = "dead store is javac artifact, close ignore is intentional xd"
)
public enum class BackendState {
    IDLE,
    CONNECTING,
    CONNECTED,
    FAILED,
    CLOSED
}

@SuppressFBWarnings(
    value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2", "DE_MIGHT_IGNORE"],
    justification = "session fields intentional"
)
public class ProxySession(
    public val config: ProxyConfig,
    public var clientChannel: Channel? = null,
    public var backendChannel: Channel? = null,
    public var state: ConnectionState = ConnectionState.HANDSHAKE,
    public var backendState: BackendState = BackendState.IDLE,
    public var protocolVersion: Int = -1,
    public var serverAddress: String = "",
    public var serverPort: Int = 25565,
    public var username: String = "",
    public var uuid: UUID? = null,
    public var verifyToken: ByteArray? = null,
    public val keyPair: KeyPair = EncryptionUtil.generateKeyPair()
) {
    private val logger = LogManager.getLogger(ProxySession::class.java)

    // queue for packets that arrive before backend is ready — bounded so we don't OOM xd
    private val pendingQueue: ArrayDeque<MinecraftPacket> = ArrayDeque(64)
    private val maxQueueSize: Int = 128

    // idempotent close guard — without this we double-close and leak buf refs lol
    private val closed: AtomicBoolean = AtomicBoolean(false)

    public fun setClient(ctx: ChannelHandlerContext) {
        clientChannel = ctx.channel()
    }

    public fun setBackend(ch: Channel) {
        backendChannel = ch
        backendState = BackendState.CONNECTED
    }

    /**
     * Transition session state atomically, logs if weird.
     * Velocity does similar but with stringly-typed states — we keep it simple.
     */
    public fun transitionTo(newState: ConnectionState) {
        // accept anything like velocity — be a slut, accept all proto versions xd
        // but log weird jumps for debugging
        val old = state
        if (old == ConnectionState.CLOSED) {
            logger.debug(
                "ignoring transition {} -> {} (already closed) for {}",
                old,
                newState,
                username
            )
            return
        }
        state = newState
        logger.debug("session {}: {} -> {}", username, old, newState)
    }

    /**
     * Try to enqueue packet for later drain when backend connects.
     * Returns false if queue full — caller should drop + warn.
     */
    public fun enqueueForBackend(packet: MinecraftPacket): Boolean {
        synchronized(pendingQueue) {
            if (pendingQueue.size >= maxQueueSize) {
                logger.warn(
                    "pending queue full ({}), dropping packet id={} for {}",
                    maxQueueSize,
                    packet.id,
                    username
                )
                return false
            }
            // retain duplicate so original can be released by caller
            val dup = MinecraftPacket(packet.id, packet.data.retainedDuplicate())
            pendingQueue.addLast(dup)
            return true
        }
    }

    /**
     * Drain queued packets to backend channel. Called once backend channelActive fires.
     * Must be called on backend eventLoop.
     */
    public fun drainQueueToBackend() {
        val backend = backendChannel
        if (backend == null || !backend.isActive) {
            logger.warn("can't drain queue, no backend for {}", username)
            // release queued bufs to avoid leak
            synchronized(pendingQueue) {
                while (pendingQueue.isNotEmpty()) {
                    pendingQueue.removeFirst().data.release()
                }
            }
            return
        }
        synchronized(pendingQueue) {
            while (pendingQueue.isNotEmpty()) {
                val pkt = pendingQueue.removeFirst()
                // writeAndFlush and release after
                backend.writeAndFlush(pkt).addListener { fut ->
                    pkt.data.release()
                    if (!fut.isSuccess) {
                        logger.warn(
                            "failed to drain queued packet id={} for {}",
                            pkt.id,
                            username,
                            fut.cause()
                        )
                    }
                }
            }
        }
        // resume reading on client after drain — backpressure dance xd
        try {
            clientChannel?.read()
        } catch (_: Exception) {}
    }

    /**
     * clear queue and release bufs — called on failure/close to avoid leak
     */
    public fun clearPendingQueue() {
        synchronized(pendingQueue) {
            while (pendingQueue.isNotEmpty()) {
                try {
                    pendingQueue.removeFirst().data.release()
                } catch (_: Exception) {}
            }
        }
    }

    public fun isClosed(): Boolean = closed.get()

    @SuppressFBWarnings(value = ["DE_MIGHT_IGNORE"], justification = "close ignore")
    public fun closeBoth(reason: String? = null) {
        if (!closed.compareAndSet(false, true)) {
            // already closed, no double work
            return
        }
        try {
            if (reason != null) logger.info("closing session {} reason={}", username, reason)
        } catch (_: Exception) {}
        backendState = BackendState.CLOSED
        state = ConnectionState.CLOSED
        clearPendingQueue()
        // flush-and-close pattern — drain outbound buffer before closing
        // otherwise we lose last disconnect packet, and lose data in OS buffer
        flushAndClose(clientChannel)
        flushAndClose(backendChannel)
    }

    @SuppressFBWarnings(
        value = ["DLS_DEAD_LOCAL_STORE"],
        justification = "kotlin catch generates dead store, false positive xd"
    )
    private fun flushAndClose(ch: Channel?) {
        if (ch == null || !ch.isOpen) return
        try {
            // write empty buffer and close after — this flushes the outbound buffer
            // netty's AUTO_CLOSE=false means we have to do it manually
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
        } catch (_: Exception) {
            try {
                ch.close()
            } catch (_: Exception) {}
        }
    }

    public fun failBackend(cause: Throwable? = null) {
        backendState = BackendState.FAILED
        clearPendingQueue()
        if (cause != null) {
            logger.warn("backend failed for {}", username, cause)
        }
    }
}
