package kz.bejiihiu.candiriya.network

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import kz.bejiihiu.candiriya.network.session.ProxySession
import kz.bejiihiu.candiriya.protocol.ConnectionState
import kz.bejiihiu.candiriya.protocol.MinecraftPacket
import kz.bejiihiu.candiriya.protocol.StringUtil
import kz.bejiihiu.candiriya.protocol.VarInt
import kz.bejiihiu.candiriya.protocol.codec.MinecraftCompressionDecoder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.apache.logging.log4j.LogManager

@SuppressFBWarnings(
    value = [
        "EI_EXPOSE_REP2",
        "REC_CATCH_EXCEPTION",
        "DE_MIGHT_IGNORE",
        "UCF_USELESS_CONTROL_FLOW"
    ],
    justification = "session intentional, catch for pipeline robustness"
)
public class BackendHandler(
    private val session: ProxySession
) : SimpleChannelInboundHandler<MinecraftPacket>() {

    private val logger = LogManager.getLogger(BackendHandler::class.java)

    override fun channelActive(ctx: ChannelHandlerContext) {
        logger.info("backend {} connected for {}", ctx.channel().remoteAddress(), session.username)
        // send handshake + loginStart
        sendHandshakeAndLogin(ctx)
        super.channelActive(ctx)
    }

    private fun sendHandshakeAndLogin(ctx: ChannelHandlerContext) {
        // handshake: proto, addr, port, nextState=2
        val hb = Unpooled.buffer()
        VarInt.writeVarInt(hb, session.protocolVersion)
        StringUtil.writeString(hb, session.serverAddress, 255)
        hb.writeShort(session.serverPort)
        VarInt.writeVarInt(hb, 2)
        val hp = MinecraftPacket(0x00, hb)
        ctx.writeAndFlush(hp).addListener { f ->
            hb.release()
            if (!f.isSuccess) logger.warn("failed handshake to backend", f.cause())
        }
        // login start
        val lb = Unpooled.buffer()
        StringUtil.writeString(lb, session.username, 16)
        // try to include uuid if present
        val uuid = session.uuid
        if (uuid != null) {
            lb.writeLong(uuid.mostSignificantBits)
            lb.writeLong(uuid.leastSignificantBits)
        }
        val lp = MinecraftPacket(0x00, lb)
        ctx.writeAndFlush(lp).addListener { f ->
            lb.release()
            if (!f.isSuccess) logger.warn("failed loginStart to backend", f.cause())
        }
    }

    override fun channelRead0(ctx: ChannelHandlerContext, packet: MinecraftPacket) {
        val client = session.clientChannel
        if (client == null || !client.isActive) {
            logger.warn("no client for backend packet id={}", packet.id)
            return
        }

        // intercept important packets for compression etc.
        when (session.state) {
            ConnectionState.LOGIN -> handleLoginBackend(ctx, packet, client)
            ConnectionState.CONFIGURATION, ConnectionState.PLAY -> {
                // raw forward
                forwardToClient(packet, client)
            }
            else -> forwardToClient(packet, client)
        }
    }

    @SuppressFBWarnings(value = ["REC_CATCH_EXCEPTION"], justification = "VarInt parsing")
    private fun handleLoginBackend(
        ctx: ChannelHandlerContext,
        packet: MinecraftPacket,
        client: io.netty.channel.Channel
    ) {
        when (packet.id) {
            0x00 -> { // Disconnect login
                logger.info("backend disconnect for {}", session.username)
                forwardToClient(packet, client)
                // backend will close; we close client after flush
                ctx.close()
            }
            0x01 -> { // EncryptionRequest — only if online mode; we offline so disconnect
                logger.warn(
                    "backend requested encryption but proxy is offline, disconnecting {}",
                    session.username
                )
                val reason = Component.text("Backend requires online mode but proxy is offline")
                disconnectClient(client, reason)
                ctx.close()
            }
            0x02 -> { // LoginSuccess
                logger.info("backend login success for {}", session.username)
                session.state = ConnectionState.CONFIGURATION
                // also flip client handler state
                try {
                    val ch = client.pipeline().get("connection") as? ConnectionHandler
                    if (ch != null) {
                        ch.setState(ConnectionState.CONFIGURATION)
                    }
                } catch (_: Exception) {}
                forwardToClient(packet, client)
            }
            0x03 -> { // SetCompression
                val buf = packet.data
                buf.markReaderIndex()
                val threshold = try {
                    VarInt.readVarInt(buf)
                } catch (e: Exception) {
                    buf.resetReaderIndex()
                    logger.warn("bad SetCompression", e)
                    return
                }
                buf.resetReaderIndex()
                logger.info(
                    "backend SetCompression threshold={} for {}",
                    threshold,
                    session.username
                )
                enableCompression(ctx, threshold)
                enableClientCompression(client, threshold)
                forwardToClient(packet, client)
            }
            else -> forwardToClient(packet, client)
        }
    }

    @SuppressFBWarnings(
        value = ["DE_MIGHT_IGNORE", "REC_CATCH_EXCEPTION"],
        justification = "ignore pipeline errors"
    )
    private fun enableCompression(ctx: ChannelHandlerContext, threshold: Int) {
        if (threshold < 0) return
        val pipeline = ctx.pipeline()
        if (pipeline.get("compressionDecoder") == null) {
            try {
                pipeline.addAfter(
                    "frameDecoder",
                    "compressionDecoder",
                    MinecraftCompressionDecoder(threshold)
                )
            } catch (_: Exception) {
                pipeline.addFirst("compressionDecoder", MinecraftCompressionDecoder(threshold))
            }
        }
        if (pipeline.get("compressionEncoder") == null) {
            // remove length encoder it'll be replaced by compression encoder handling MinecraftPacket
            if (pipeline.get("packetEncoder") != null) {
                try {
                    pipeline.remove("packetEncoder")
                } catch (_: Exception) {}
            }
            pipeline.addLast(
                "compressionEncoder",
                kz.bejiihiu.candiriya.protocol.codec.MinecraftCompressionEncoder(threshold)
            )
        }
        // sync session state
        if (pipeline.get("packetEncoder") == null) {
            // ensure client also switched — client side will still have packetEncoder until we replace
        }
    }

    @SuppressFBWarnings(
        value = ["DE_MIGHT_IGNORE", "REC_CATCH_EXCEPTION"],
        justification = "ignore pipeline errors"
    )
    private fun enableClientCompression(client: io.netty.channel.Channel, threshold: Int) {
        if (threshold < 0) return
        val pipeline = client.pipeline()
        if (pipeline.get("compressionDecoder") != null) return
        try {
            pipeline.addAfter(
                "frameDecoder",
                "compressionDecoder",
                MinecraftCompressionDecoder(threshold)
            )
        } catch (_: Exception) {
            pipeline.addFirst("compressionDecoder", MinecraftCompressionDecoder(threshold))
        }
        if (pipeline.get("packetEncoder") != null) {
            try {
                pipeline.remove("packetEncoder")
            } catch (_: Exception) {}
        }
        pipeline.addLast(
            "compressionEncoder",
            kz.bejiihiu.candiriya.protocol.codec.MinecraftCompressionEncoder(threshold)
        )
        // also flip client state if still LOGIN
        try {
            val h = pipeline.get("connection") as? ConnectionHandler
            if (h != null && h.state == ConnectionState.LOGIN) {
                // don't auto flip here, let backend LoginSuccess drive it
            }
        } catch (_: Exception) {}
    }

    private fun forwardToClient(packet: MinecraftPacket, client: io.netty.channel.Channel) {
        // duplicate buffer because packet will be released after this handler
        val dup = packet.data.retainedDuplicate()
        val fwd = MinecraftPacket(packet.id, dup)
        client.writeAndFlush(fwd).addListener { future ->
            dup.release()
            if (!future.isSuccess) logger.warn("failed forward to client", future.cause())
        }
    }

    private fun disconnectClient(client: io.netty.channel.Channel, reason: Component) {
        val json = GsonComponentSerializer.gson().serialize(reason)
        val buf = Unpooled.buffer()
        StringUtil.writeString(buf, json)
        val pkt = MinecraftPacket(0x00, buf)
        client.writeAndFlush(pkt).addListener {
            buf.release()
            client.close()
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        logger.warn("backend exception for {}", session.username, cause)
        session.closeBoth("backend exception")
        ctx.close()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.info("backend disconnected for {}", session.username)
        session.clientChannel?.close()
        super.channelInactive(ctx)
    }
}
