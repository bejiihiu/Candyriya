package kz.bejiihiu.candiriya.network

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.CorruptedFrameException
import io.netty.handler.codec.DecoderException
import io.netty.handler.timeout.ReadTimeoutException
import kz.bejiihiu.candiriya.config.ProxyConfig
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
 * Handles client→proxy Minecraft packets.
 * Supports handshake→status/login + backend proxy forwarding.
 */
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
        session.state = newState
    }

    private var packetCounter: Long = 0

    private val session = kz.bejiihiu.candiriya.network.session.ProxySession(config)

    override fun channelActive(ctx: ChannelHandlerContext) {
        state = ConnectionState.HANDSHAKE
        session.state = ConnectionState.HANDSHAKE
        session.setClient(ctx)
        logger.info("client {} connected", ctx.channel().remoteAddress())
        super.channelActive(ctx)
    }

    override fun channelRead0(ctx: ChannelHandlerContext, packet: MinecraftPacket) {
        packetCounter++
        if (packetCounter % 100 == 0L) {
            val tick = tickScheduler?.getCurrentTick() ?: -1
            logger.debug("chan {} packets={} tick={}", ctx.channel().id(), packetCounter, tick)
        }
        // also periodic debug counter via scheduler (simple metrics)
        if (packetCounter == 1L) {
            scheduler?.execute {
                logger.debug("first packet from {} id={}", ctx.channel().remoteAddress(), packet.id)
            }
        }

        when (state) {
            ConnectionState.HANDSHAKE -> handleHandshake(ctx, packet)
            ConnectionState.STATUS -> handleStatus(ctx, packet)
            ConnectionState.LOGIN -> handleLogin(ctx, packet)
            ConnectionState.CONFIGURATION -> handleConfiguration(ctx, packet)
            ConnectionState.PLAY -> handlePlay(ctx, packet)
            ConnectionState.CLOSED -> logger.warn("packet in closed state id={}", packet.id)
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
            // accept any protocolVersion, just log
            // TODO: Velocity-style version translation
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
            session.state = state
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
                            // don't release outBuf here, encoder copies; release after flush
                            outBuf.release()
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
                }
            }
        } catch (e: Exception) {
            logger.warn("bad status packet from {}", ctx.channel().remoteAddress(), e)
            ctx.close()
        }
    }

    private fun handleLogin(ctx: ChannelHandlerContext, packet: MinecraftPacket) {
        // LoginStart is 0x00 with username (+ optional uuid)
        try {
            when (packet.id) {
                0x00 -> {
                    val username = StringUtil.readString(packet.data, 16)
                    // try read uuid if present (16 bytes)
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
                    // connect to backend
                    connectToBackend(ctx)
                }
                else -> {
                    // encryption / plugin responses — forward to backend if connected
                    forwardToBackend(packet)
                }
            }
        } catch (e: Exception) {
            logger.warn("bad login packet from {}", ctx.channel().remoteAddress(), e)
            ctx.close()
        }
    }

    private fun handleConfiguration(ctx: ChannelHandlerContext, packet: MinecraftPacket) {
        // client is in CONFIGURATION (1.21.5); forward to backend
        // if packet is FinishConfiguration ack (0x03), transition to PLAY
        if (packet.id == 0x03) {
            logger.info("client {} finish configuration -> PLAY", ctx.channel().remoteAddress())
            state = ConnectionState.PLAY
            session.state = ConnectionState.PLAY
        }
        forwardToBackend(packet)
    }

    private fun handlePlay(ctx: ChannelHandlerContext, packet: MinecraftPacket) {
        forwardToBackend(packet)
    }

    private fun forwardToBackend(packet: MinecraftPacket) {
        val backend = session.backendChannel
        if (backend == null || !backend.isActive) {
            logger.warn("no backend for packet id={} from {}", packet.id, session.username)
            return
        }
        val dup = packet.data.retainedDuplicate()
        val fwd = MinecraftPacket(packet.id, dup)
        backend.writeAndFlush(fwd).addListener { fut ->
            dup.release()
            if (!fut.isSuccess) logger.warn("failed forward to backend", fut.cause())
        }
    }

    private fun connectToBackend(ctx: ChannelHandlerContext) {
        val host = config.backend.host
        val port = config.backend.port
        logger.info("connecting {} to backend {}:{}", session.username, host, port)
        val bootstrap = io.netty.bootstrap.Bootstrap()
            .group(ctx.channel().eventLoop())
            .channel(io.netty.channel.socket.nio.NioSocketChannel::class.java)
            .option(io.netty.channel.ChannelOption.SO_KEEPALIVE, true)
            .option(io.netty.channel.ChannelOption.TCP_NODELAY, true)
            .handler(
                object : io.netty.channel.ChannelInitializer<
                    io.netty.channel.socket.SocketChannel
                    >() {
                    override fun initChannel(ch: io.netty.channel.socket.SocketChannel) {
                        ch.pipeline().addLast(
                            "frameDecoder",
                            kz.bejiihiu.candiriya.protocol.MinecraftVarintFrameDecoder(
                                config.protocol.maxPacketSize
                            )
                        )
                        ch.pipeline().addLast(
                            "packetDecoder",
                            kz.bejiihiu.candiriya.protocol.MinecraftPacketDecoder()
                        )
                        ch.pipeline().addLast(
                            "packetEncoder",
                            kz.bejiihiu.candiriya.protocol.MinecraftVarintLengthEncoder()
                        )
                        ch.pipeline().addLast(
                            "backendHandler",
                            BackendHandler(session)
                        )
                    }
                }
            )
        bootstrap.connect(host, port).addListener { fut ->
            if (!fut.isSuccess) {
                logger.warn(
                    "failed to connect {} to backend {}:{}",
                    session.username,
                    host,
                    port,
                    fut.cause()
                )
                val reason = Component.text(
                    "Could not connect to backend: ${fut.cause()?.message ?: "unknown"}"
                )
                    .color(NamedTextColor.RED)
                val disc = createDisconnectPacket(reason)
                ctx.writeAndFlush(disc).addListener {
                    disc.data.release()
                    ctx.close()
                }
            } else {
                val ch = (fut as io.netty.channel.ChannelFuture).channel()
                session.setBackend(ch)
                logger.info("backend connected for {}", session.username)
            }
        }
    }

    @SuppressFBWarnings(
        value = ["UPM_UNCALLED_PRIVATE_METHOD"],
        justification = "kept for future fallback"
    )
    private fun handleLoginDisconnect(ctx: ChannelHandlerContext, packet: MinecraftPacket) {
        // fallback disconnect (unused)
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
        // motd comes from config as MiniMessage string
        // parse via adventure, then serialize to gson json for status description
        val motdComponent: Component = try {
            MiniMessage.miniMessage().deserialize(config.status.motd)
        } catch (_: Exception) {
            // fallback to plain text if minimessage borked xd
            Component.text(config.status.motd)
        }
        val motdJson: String = GsonComponentSerializer.gson().serialize(motdComponent)
        // version name/protocol from config, supports 26.x + old clients (Velocity-style any proto)
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
        session.closeBoth("exception")
        ctx.close()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.info("client {} disconnected", ctx.channel().remoteAddress())
        state = ConnectionState.CLOSED
        session.closeBoth()
        super.channelInactive(ctx)
    }
}
