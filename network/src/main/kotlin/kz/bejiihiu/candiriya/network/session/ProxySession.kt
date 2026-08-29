package kz.bejiihiu.candiriya.network.session

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import java.security.KeyPair
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kz.bejiihiu.candiriya.config.ProxyConfig
import kz.bejiihiu.candiriya.protocol.ConnectionState
import kz.bejiihiu.candiriya.protocol.EncryptionUtil
import kz.bejiihiu.candiriya.protocol.MinecraftPacket
import kz.bejiihiu.candiriya.server.RegisteredServer
import org.apache.logging.log4j.LogManager

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
    public val keyPair: KeyPair = EncryptionUtil.generateKeyPair(),
    public var currentServer: RegisteredServer? = null
) {
    private val logger = LogManager.getLogger(ProxySession::class.java)

    private val pendingQueue: ArrayDeque<MinecraftPacket> = ArrayDeque(64)
    private val maxQueueSize: Int = 128

    private val closed: AtomicBoolean = AtomicBoolean(false)

    public fun setClient(ctx: ChannelHandlerContext) {
        clientChannel = ctx.channel()
    }

    public fun setBackend(ch: Channel) {
        backendChannel = ch
        backendState = BackendState.CONNECTED
    }

    public fun transitionTo(newState: ConnectionState) {
        val old = state
        if (old == ConnectionState.CLOSED) {
            logger.debug("ignoring transition {} -> {} (already closed) for {}", old, newState, username)
            return
        }
        state = newState
        logger.debug("session {}: {} -> {}", username, old, newState)
    }

    public fun enqueueForBackend(packet: MinecraftPacket): Boolean {
        synchronized(pendingQueue) {
            if (pendingQueue.size >= maxQueueSize) {
                logger.warn("pending queue full ({}), dropping packet id={} for {}", maxQueueSize, packet.id, username)
                return false
            }
            val dup = MinecraftPacket(packet.id, packet.data.retainedDuplicate())
            pendingQueue.addLast(dup)
            return true
        }
    }

    public fun drainQueueToBackend() {
        val backend = backendChannel
        if (backend == null || !backend.isActive) {
            logger.warn("can't drain queue, no backend for {}", username)
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
                backend.writeAndFlush(pkt).addListener { fut ->
                    pkt.data.release()
                    if (!fut.isSuccess) {
                        logger.warn("failed to drain queued packet id={} for {}", pkt.id, username, fut.cause())
                    }
                }
            }
        }
        try {
            clientChannel?.read()
        } catch (_: Exception) {}
    }

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

    /** Close only backend side, keep client alive for fallback. */
    public fun closeBackendOnly(reason: String? = null) {
        if (reason != null) logger.info("closing backend for {} reason={}", username, reason)
        backendState = BackendState.CLOSED
        clearPendingQueue()
        flushAndClose(backendChannel)
        backendChannel = null
    }

    @SuppressFBWarnings(value = ["DE_MIGHT_IGNORE"], justification = "close ignore")
    public fun closeBoth(reason: String? = null) {
        if (!closed.compareAndSet(false, true)) return
        try {
            if (reason != null) logger.info("closing session {} reason={}", username, reason)
        } catch (_: Exception) {}
        backendState = BackendState.CLOSED
        state = ConnectionState.CLOSED
        clearPendingQueue()
        flushAndClose(clientChannel)
        flushAndClose(backendChannel)
    }

    @SuppressFBWarnings(value = ["DLS_DEAD_LOCAL_STORE"], justification = "kotlin catch generates dead store, false positive xd")
    private fun flushAndClose(ch: Channel?) {
        if (ch == null || !ch.isOpen) return
        try {
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
        if (cause != null) logger.warn("backend failed for {}", username, cause)
    }
}
