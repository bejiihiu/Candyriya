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
 * Supports handshake→status/login flow, without backend forward yet.
 */
public class ConnectionHandler(
    private val config: ProxyConfig,
    private val scheduler: Scheduler? = null,
    private val tickScheduler: TickScheduler? = null
) : SimpleChannelInboundHandler<MinecraftPacket>() {

    private val logger = LogManager.getLogger(ConnectionHandler::class.java)

    public var state: ConnectionState = ConnectionState.HANDSHAKE
        private set

    private var packetCounter: Long = 0

    override fun channelActive(ctx: ChannelHandlerContext) {
        state = ConnectionState.HANDSHAKE
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
            ConnectionState.LOGIN -> handleLoginDisconnect(ctx, packet)
            ConnectionState.PLAY -> logger.warn("unexpected play packet id={}", packet.id)
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

    private fun handleLoginDisconnect(ctx: ChannelHandlerContext, packet: MinecraftPacket) {
        // any login packet → disconnect with reason
        logger.info(
            "login packet id={} from {} — disconnecting (login not implemented)",
            packet.id,
            ctx.channel().remoteAddress()
        )
        val reason = Component.text("Login not implemented yet")
            .color(NamedTextColor.RED)
        val disconnect = createDisconnectPacket(reason)
        ctx.writeAndFlush(disconnect).addListener { _ ->
            // release after encode copied
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
        ctx.close()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.info("client {} disconnected", ctx.channel().remoteAddress())
        state = ConnectionState.CLOSED
        super.channelInactive(ctx)
    }
}
