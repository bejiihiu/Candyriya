package kz.bejiihiu.candiriya.network

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.CorruptedFrameException
import io.netty.handler.codec.DecoderException
import io.netty.handler.timeout.ReadTimeoutException
import kz.bejiihiu.candiriya.config.ProxyConfig
import kz.bejiihiu.candiriya.network.session.BackendState
import kz.bejiihiu.candiriya.protocol.ConnectionState
import kz.bejiihiu.candiriya.protocol.MinecraftPacket
import kz.bejiihiu.candiriya.protocol.StringUtil
import kz.bejiihiu.candiriya.protocol.VarInt
import kz.bejiihiu.candiriya.scheduler.Scheduler
import kz.bejiihiu.candiriya.scheduler.tick.TickScheduler
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.apache.logging.log4j.LogManager

/**
 * Handles client→proxy packets. The real proxy logic lives here.
 * Supports handshake→status/login plus backend forwarding with proper backpressure.
 *
 * Big difference from old version: no more dropping packets when backend isn't ready.
 * We queue them (bounded) and drain after backend connects — fixes the 4-byte stall xd
 */
@SuppressFBWarnings(
    value = ["URF_UNREAD_FIELD", "DE_MIGHT_IGNORE", "DLS_DEAD_LOCAL_STORE"],
    justification = "backendConnection is used for lifecycle, dead store is kotlin's fault xd"
)
public class ConnectionHandler(
    private val config: ProxyConfig,
    private val scheduler: Scheduler? = null,
    private val tickScheduler: TickScheduler? = null
) : SimpleChannelInboundHandler<MinecraftPacket>() {

    private val logger = LogManager.getLogger(ConnectionHandler::class.java)

    public var state: ConnectionState = ConnectionState.HANDSHAKE
        private set

    public fun setState(newState: ConnectionState) {
        state = newState
        session.transitionTo(newState)
    }

    private var packetCounter: Long = 0

    private val session = kz.bejiihiu.candiriya.network.session.ProxySession(config)

    private var backendConnection: BackendConnection? = null

    override fun channelActive(ctx: ChannelHandlerContext) {
        // fresh session, always handshake
        state = ConnectionState.HANDSHAKE
        session.transitionTo(ConnectionState.HANDSHAKE)
        session.backendState = BackendState.IDLE
        session.setClient(ctx)
        logger.info("client {} connected", ctx.channel().remoteAddress())
        // AUTO_READ is false (see NetworkServer), so we must trigger first read manually
        // without this, we never get handshake — classic netty pitfall
        ctx.read()
        super.channelActive(ctx)
    }

    override fun channelRead0(ctx: ChannelHandlerContext, packet: MinecraftPacket) {
        packetCounter++
        if (packetCounter % 100 == 0L) {
            val tick = tickScheduler?.getCurrentTick() ?: -1
            logger.debug("chan {} packets={} tick={}", ctx.channel().id(), packetCounter, tick)
        }
        if (packetCounter == 1L) {
            scheduler?.execute {
                logger.debug("first packet from {} id={}", ctx.channel().remoteAddress(), packet.id)
            }
        }

        try {
            when (state) {
                ConnectionState.HANDSHAKE -> handleHandshake(ctx, packet)
                ConnectionState.STATUS -> handleStatus(ctx, packet)
                ConnectionState.LOGIN -> handleLogin(ctx, packet)
                ConnectionState.CONFIGURATION -> handleConfiguration(ctx, packet)
                ConnectionState.PLAY -> handlePlay(ctx, packet)
                ConnectionState.CLOSED -> logger.warn("packet in closed state id={}", packet.id)
            }
        } finally {
            // backpressure: ask for next packet unless we're closed
            // if we queued for backend, this still lets us keep reading until queue full
            if (state != ConnectionState.CLOSED && ctx.channel().isActive) {
                // for LOGIN we might be waiting for backend — still read to fill queue (bounded)
                // but don't overwhelm if backend write buffer is full (check writability)
                val backend = session.backendChannel
                if (backend == null || backend.isWritable) {
                    ctx.read()
                } else {
                    // backend clogged, wait a bit then resume — simple backpressure
                    ctx.channel().eventLoop().schedule(
                        { if (ctx.channel().isActive) ctx.read() },
                        10,
                        java.util.concurrent.TimeUnit.MILLISECONDS
                    )
                }
            }
        }
    }

    private fun handleHandshake(ctx: ChannelHandlerContext, packet: MinecraftPacket) {
        val buf = packet.data
        try {
            if (packet.id != 0x00) {
                logger.warn(
                    "unexpected handshake packet id={} from {}",
                    packet.id,
                    ctx.channel().remoteAddress()
                )
                ctx.close()
                return
            }
            val proto = VarInt.readVarInt(buf)
            val addr = StringUtil.readString(buf, 255)
            val port = buf.readUnsignedShort()
            val nextStateVal = VarInt.readVarInt(buf)
            logger.info(
                "handshake proto={} addr={}:{} next={} from {}",
                proto,
                addr,
                port,
                nextStateVal,
                ctx.channel().remoteAddress()
            )
            // be a slut — accept any protocol version like velocity does xd
            // don't validate proto, just log and go
            state = when (nextStateVal) {
                1 -> ConnectionState.STATUS
                2 -> ConnectionState.LOGIN
                else -> {
                    logger.warn(
                        "unknown nextState {} closing {}",
                        nextStateVal,
                        ctx.channel().remoteAddress()
                    )
                    ctx.close()
                    return
                }
            }
            session.transitionTo(state)
            session.protocolVersion = proto
            session.serverAddress = addr
            session.serverPort = port
        } catch (e: Exception) {
            logger.warn("bad handshake from {}", ctx.channel().remoteAddress(), e)
            ctx.close()
        }
    }

    @SuppressFBWarnings(
        value = ["DLS_DEAD_LOCAL_STORE"],
        justification = "outPacket used for write"
    )
    private fun handleStatus(ctx: ChannelHandlerContext, packet: MinecraftPacket) {
        val buf = packet.data
        try {
            when (packet.id) {
                0x00 -> {
                    // Status Request
                    val json = buildStatusJson()
                    val outBuf = Unpooled.buffer()
                    try {
                        StringUtil.writeString(outBuf, json)
                        val outPacket = MinecraftPacket(0x00, outBuf)
                        ctx.writeAndFlush(outPacket).addListener { future ->
                            if (!future.isSuccess) {
                                logger.warn("failed to send status response", future.cause())
                            }
                            outBuf.release()
                            // trigger next read for ping
                            if (ctx.channel().isActive) ctx.read()
                        }
                    } catch (e: Exception) {
                        outBuf.release()
                        throw e
                    }
                }
                0x01 -> {
                    // Ping — payload is long
                    if (buf.readableBytes() < 8) {
                        logger.warn("ping packet too short from {}", ctx.channel().remoteAddress())
                        ctx.close()
                        return
                    }
                    val payload = buf.readLong()
                    val respBuf = Unpooled.buffer(8)
                    respBuf.writeLong(payload)
                    val outPacket = MinecraftPacket(0x01, respBuf)
                    ctx.writeAndFlush(outPacket).addListener {
                        respBuf.release()
                        ctx.close()
                    }
                }
                else -> {
                    logger.warn(
                        "unknown status packet id={} from {}",
                        packet.id,
                        ctx.channel().remoteAddress()
                    )
                    ctx.read()
                }
            }
        } catch (e: Exception) {
            logger.warn("bad status packet from {}", ctx.channel().remoteAddress(), e)
            ctx.close()
        }
    }

    private fun handleLogin(ctx: ChannelHandlerContext, packet: MinecraftPacket) {
        try {
            when (packet.id) {
                0x00 -> {
                    val username = StringUtil.readString(packet.data, 16)
                    var uuid: java.util.UUID? = null
                    if (packet.data.readableBytes() >= 16) {
                        val most = packet.data.readLong()
                        val least = packet.data.readLong()
                        uuid = java.util.UUID(most, least)
                    }
                    logger.info(
                        "login start username={} uuid={} from {}",
                        username,
                        uuid,
                        ctx.channel().remoteAddress()
                    )
                    session.username = username
                    session.uuid = uuid
                        ?: kz.bejiihiu.candiriya.protocol.UuidUtil.offlineUuid(
                            username
                        )
                    // connect to backend (with retry from config)
                    connectToBackend(ctx)
                }
                else -> {
                    // encryption / plugin responses — forward or queue if backend not ready
                    forwardToBackend(ctx, packet)
                }
            }
        } catch (e: Exception) {
            logger.warn("bad login packet from {}", ctx.channel().remoteAddress(), e)
            ctx.close()
        }
    }

    private fun handleConfiguration(ctx: ChannelHandlerContext, packet: MinecraftPacket) {
        if (packet.id == 0x03) {
            logger.info("client {} finish configuration -> PLAY", ctx.channel().remoteAddress())
            state = ConnectionState.PLAY
            session.transitionTo(ConnectionState.PLAY)
        }
        forwardToBackend(ctx, packet)
    }

    private fun handlePlay(ctx: ChannelHandlerContext, packet: MinecraftPacket) {
        forwardToBackend(ctx, packet)
    }

    private fun forwardToBackend(ctx: ChannelHandlerContext, packet: MinecraftPacket) {
        val backend = session.backendChannel
        // if no backend yet or not writable, queue it — don't drop like old code did xd
        if (backend == null || !backend.isActive || session.backendState != BackendState.CONNECTED) {
            val queued = session.enqueueForBackend(packet)
            if (!queued) {
                logger.warn("queue full, disconnecting {}", session.username)
                val reason = Component.text(
                    "Proxy queue overflow — try again"
                ).color(NamedTextColor.RED)
                val disc = createDisconnectPacket(reason)
                ctx.writeAndFlush(disc).addListener {
                    disc.data.release()
                    session.closeBoth("queue overflow")
                }
            }
            // don't need to do anything else, packet is queued and will be drained on backend connect
            return
        }

        // check writability for backpressure
        if (!backend.isWritable) {
            logger.debug(
                "backend not writable for {}, queuing packet id={}",
                session.username,
                packet.id
            )
            val queued = session.enqueueForBackend(packet)
            if (!queued) session.closeBoth("backend clogged")
            return
        }

        val dup = packet.data.retainedDuplicate()
        val fwd = MinecraftPacket(packet.id, dup)
        backend.writeAndFlush(fwd).addListener { fut ->
            dup.release()
            if (!fut.isSuccess) {
                logger.warn("failed forward to backend for {}", session.username, fut.cause())
                // don't close immediately if we can queue, but if write failed backend is probably dead
                if (!session.isClosed()) {
                    session.closeBoth("backend write failed")
                }
            } else {
                // success — resume reading on client if we paused for backpressure
                if (ctx.channel().isActive) ctx.read()
            }
            // also resume reading on backend channel
            try {
                backend.read()
            } catch (_: Exception) {}
        }
    }

    private fun connectToBackend(ctx: ChannelHandlerContext) {
        // already connecting?
        if (session.backendState == BackendState.CONNECTING || session.backendState == BackendState.CONNECTED) {
            logger.debug(
                "already connecting/connected for {}, ignoring duplicate connect",
                session.username
            )
            return
        }
        val conn = BackendConnection(session, config)
        backendConnection = conn
        conn.connect(
            clientChannel = ctx.channel(),
            onConnected = { _ ->
                // drain any packets that arrived while connecting
                session.drainQueueToBackend()
                // make sure client keeps reading
                if (ctx.channel().isActive) ctx.read()
            },
            onFailed = { cause ->
                logger.warn("could not connect {} to backend", session.username, cause)
                val msg = cause.message ?: "unknown"
                val reason = Component.text(
                    "Could not connect to backend: $msg"
                ).color(NamedTextColor.RED)
                val disc = createDisconnectPacket(reason)
                // send disconnect to client then close both sides with flush
                ctx.writeAndFlush(disc).addListener {
                    disc.data.release()
                    session.closeBoth("backend connect failed: $msg")
                    ctx.close()
                }
            }
        )
    }

    @SuppressFBWarnings(
        value = ["UPM_UNCALLED_PRIVATE_METHOD"],
        justification = "kept for future fallback"
    )
    private fun handleLoginDisconnect(ctx: ChannelHandlerContext, packet: MinecraftPacket) {
        logger.info(
            "login packet id={} from {} — disconnecting (login not implemented)",
            packet.id,
            ctx.channel().remoteAddress()
        )
        val reason = Component.text("Login not implemented yet")
            .color(NamedTextColor.RED)
        val disconnect = createDisconnectPacket(reason)
        ctx.writeAndFlush(disconnect).addListener { _ ->
            disconnect.data.release()
            ctx.close()
        }
    }

    @SuppressFBWarnings(
        value = ["SA_LOCAL_SELF_ASSIGNMENT"],
        justification = "kotlin try-catch generates self assign bytecode"
    )
    private fun buildStatusJson(): String {
        val motdComponent: Component = try {
            MiniMessage.miniMessage().deserialize(config.status.motd)
        } catch (_: Exception) {
            Component.text(config.status.motd)
        }
        val motdJson: String = GsonComponentSerializer.gson().serialize(motdComponent)
        val versionNameEsc = config.status.versionName
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        val versionPart =
            """{"version":{"name":"$versionNameEsc","protocol":${config.status.versionProtocol}}"""
        val playersPart = ""","players":{"max":${config.status.maxPlayers},"online":0}"""
        val descPart = ""","description":$motdJson}"""
        return versionPart + playersPart + descPart
    }

    private fun createDisconnectPacket(reason: Component): MinecraftPacket {
        val json: String = GsonComponentSerializer.gson().serialize(reason)
        val buf = Unpooled.buffer()
        StringUtil.writeString(buf, json)
        return MinecraftPacket(0x00, buf)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        when (cause) {
            is ReadTimeoutException -> logger.info("timeout {}", ctx.channel().remoteAddress())
            is CorruptedFrameException, is DecoderException -> logger.warn(
                "bad packet from {}",
                ctx.channel().remoteAddress(),
                cause
            )
            else -> logger.warn("exception on {}", ctx.channel().remoteAddress(), cause)
        }
        session.closeBoth("exception: ${cause.message}")
        ctx.close()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.info("client {} disconnected", ctx.channel().remoteAddress())
        state = ConnectionState.CLOSED
        session.closeBoth()
        super.channelInactive(ctx)
    }

    override fun channelWritabilityChanged(ctx: ChannelHandlerContext) {
        // if channel becomes writable again, resume reading
        if (ctx.channel().isWritable) {
            ctx.read()
        }
        super.channelWritabilityChanged(ctx)
    }
}
