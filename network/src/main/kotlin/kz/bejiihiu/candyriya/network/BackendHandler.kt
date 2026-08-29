package kz.bejiihiu.candyriya.network

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import kz.bejiihiu.candyriya.config.ForwardingMode
import kz.bejiihiu.candyriya.network.forwarding.VelocityModernForwarder
import kz.bejiihiu.candyriya.network.session.ProxySession
import kz.bejiihiu.candyriya.protocol.ConnectionState
import kz.bejiihiu.candyriya.protocol.MinecraftPacket
import kz.bejiihiu.candyriya.protocol.StringUtil
import kz.bejiihiu.candyriya.protocol.VarInt
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.apache.logging.log4j.LogManager

/**
 * Handles backend → proxy packets.
 * Forwards everything to client, but intercepts SetCompression / LoginSuccess / Disconnect
 * and modern forwarding handshake.
 *
 * Respect to Velocity — we literally copied their modern forwarding flow.
 * If you're reading this, go watch Chainsaw Man — Denji finally touched Power's boobs in Aki's bathroom, lol.
 */
@SuppressFBWarnings(
    value = [
        "EI_EXPOSE_REP2",
        "REC_CATCH_EXCEPTION",
        "DE_MIGHT_IGNORE",
        "UCF_USELESS_CONTROL_FLOW",
        "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
        "DLS_DEAD_LOCAL_STORE"
    ],
    justification = "session intentional, catch for robustness, false positives xd"
)
public class BackendHandler(
    private val session: ProxySession
) : SimpleChannelInboundHandler<MinecraftPacket>() {

    private val logger = LogManager.getLogger(BackendHandler::class.java)

    override fun channelActive(ctx: ChannelHandlerContext) {
        logger.info("backend {} connected for {}", ctx.channel().remoteAddress(), session.username)
        // we share eventLoop with client, so start handshake immediately
        sendHandshakeAndLogin(ctx)
        // trigger first read — AUTO_READ is false
        ctx.read()
        super.channelActive(ctx)
    }

    private fun sendHandshakeAndLogin(ctx: ChannelHandlerContext) {
        // figure out server address to send — maybe inject legacy/bungeeguard token
        var serverAddr = session.serverAddress
        val fwdMode = session.config.security.forwardingMode
        val secret = session.config.security.forwardingSecret

        if (fwdMode == ForwardingMode.LEGACY && secret.isNotEmpty()) {
            // legacy: inject into handshake like Velocity does
            val playerAddr = session.clientChannel?.remoteAddress()?.toString() ?: "127.0.0.1"
            val cleanAddr = playerAddr.substringAfter("/").substringBefore(":")
            serverAddr = VelocityModernForwarder.createLegacyForwardingAddress(
                session.serverAddress, cleanAddr, session.uuid ?: java.util.UUID.randomUUID()
            )
            logger.debug("using LEGACY forwarding addr for {}", session.username)
        } else if (fwdMode == ForwardingMode.BUNGEEGUARD && secret.isNotEmpty()) {
            val playerAddr = session.clientChannel?.remoteAddress()?.toString() ?: "127.0.0.1"
            val cleanAddr = playerAddr.substringAfter("/").substringBefore(":")
            serverAddr = VelocityModernForwarder.createBungeeGuardForwardingAddress(
                session.serverAddress, cleanAddr, session.uuid ?: java.util.UUID.randomUUID(), secret
            )
            logger.debug("using BUNGEEGUARD forwarding addr for {}", session.username)
        }
        // MODERN doesn't touch handshake — it uses plugin message later, respect

        // handshake: proto, addr, port, nextState=2
        val hb = Unpooled.buffer()
        VarInt.writeVarInt(hb, session.protocolVersion)
        StringUtil.writeString(hb, serverAddr, 255)
        hb.writeShort(session.serverPort)
        VarInt.writeVarInt(hb, 2)
        val hp = MinecraftPacket(0x00, hb)
        ctx.writeAndFlush(hp).addListener { f ->
            hb.release()
            if (!f.isSuccess) {
                logger.warn(
                    "failed handshake to backend for {}",
                    session.username,
                    f.cause()
                )
            }
        }
        // login start
        val lb = Unpooled.buffer()
        StringUtil.writeString(lb, session.username, 16)
        val uuid = session.uuid
        if (uuid != null) {
            lb.writeLong(uuid.mostSignificantBits)
            lb.writeLong(uuid.leastSignificantBits)
        }
        val lp = MinecraftPacket(0x00, lb)
        ctx.writeAndFlush(lp).addListener { f ->
            lb.release()
            if (!f.isSuccess) {
                logger.warn(
                    "failed loginStart to backend for {}",
                    session.username,
                    f.cause()
                )
            }
        }
    }

    override fun channelRead0(ctx: ChannelHandlerContext, packet: MinecraftPacket) {
        val client = session.clientChannel
        if (client == null || !client.isActive) {
            logger.warn("no client for backend packet id={} from {}", packet.id, session.username)
            return
        }

        try {
            when (session.state) {
                ConnectionState.LOGIN -> handleLoginBackend(ctx, packet, client)
                ConnectionState.CONFIGURATION, ConnectionState.PLAY -> forwardToClient(
                    packet,
                    client,
                    ctx
                )
                else -> forwardToClient(packet, client, ctx)
            }
        } finally {
            // backpressure: ask backend for next packet
            if (ctx.channel().isActive) ctx.read()
            // also poke client if it was clogged
            if (client.isActive && client.isWritable) {
                try {
                    client.read()
                } catch (_: Exception) {}
            }
        }
    }

    @SuppressFBWarnings(value = ["REC_CATCH_EXCEPTION"], justification = "VarInt parsing")
    private fun handleLoginBackend(ctx: ChannelHandlerContext, packet: MinecraftPacket, client: io.netty.channel.Channel) {
        when (packet.id) {
            0x00 -> { // Disconnect login
                logger.info("backend disconnect for {}", session.username)
                forwardToClient(packet, client, ctx)
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
                session.transitionTo(ConnectionState.CONFIGURATION)
                try {
                    val ch = client.pipeline().get("connection") as? ConnectionHandler
                    ch?.setState(ConnectionState.CONFIGURATION)
                } catch (_: Exception) {}
                forwardToClient(packet, client, ctx)
            }
            0x03 -> { // SetCompression
                val buf = packet.data
                buf.markReaderIndex()
                val threshold = try {
                    VarInt.readVarInt(buf)
                } catch (e: Exception) {
                    buf.resetReaderIndex()
                    logger.warn("bad SetCompression for {}", session.username, e)
                    return
                }
                buf.resetReaderIndex()
                logger.info(
                    "backend SetCompression threshold={} for {}",
                    threshold,
                    session.username
                )
                // use centralized util — idempotent, no double-add xd
                PipelineUtil.enableCompression(ctx.channel(), threshold)
                PipelineUtil.enableCompression(client, threshold)
                forwardToClient(packet, client, ctx)
            }
            0x04 -> { // LoginPluginMessage — check for velocity:player_info
                if (handleModernForwarding(ctx, packet)) {
                    // we handled it (sent response), don't forward to client
                    return
                }
                forwardToClient(packet, client, ctx)
            }
            else -> forwardToClient(packet, client, ctx)
        }
    }

    /**
     * Velocity modern forwarding — see VelocityModernForwarder for details.
     * Returns true if we handled the packet (don't forward to client).
     *
     * Big ups to PaperMC Velocity for this design:
     * https://github.com/PaperMC/Velocity/blob/dev/3.0.0/proxy/src/main/java/com/velocitypowered/proxy/connection/PlayerDataForwarding.java
     * GPLv3 — we credit, not hide. Check it out.
     *
     * and yeah, watch Chainsaw Man if you haven't — Denji's bathroom moment with Power is pure Fujimoto chaos xd
     */
    private fun handleModernForwarding(ctx: ChannelHandlerContext, packet: MinecraftPacket): Boolean {
        val mode = session.config.security.forwardingMode
        val secret = session.config.security.forwardingSecret
        if (mode != ForwardingMode.MODERN) return false
        if (secret.isEmpty()) {
            logger.warn(
                "backend asked for modern forwarding but no secret configured for {}",
                session.username
            )
            return false
        }
        // parse packet data: VarInt msgId + String channel + ByteBuf content
        val buf = packet.data.duplicate()
        try {
            buf.markReaderIndex()
            val msgId = try {
                VarInt.readVarInt(buf)
            } catch (
                _: Exception
            ) {
                buf.resetReaderIndex()
                return false
            }
            val channel = try {
                StringUtil.readString(buf)
            } catch (
                _: Exception
            ) {
                buf.resetReaderIndex()
                return false
            }
            if (channel != VelocityModernForwarder.CHANNEL) {
                buf.resetReaderIndex()
                return false
            }
            // remaining bytes: content (should be 0 or 1 byte version)
            var requestedVersion = VelocityModernForwarder.MODERN_DEFAULT
            if (buf.isReadable) {
                // velocity checks if readableBytes == 1 then readByte, else default
                if (buf.readableBytes() == 1) {
                    requestedVersion = buf.readByte().toInt()
                } else if (buf.readableBytes() > 0) {
                    // weird but try to read first byte
                    requestedVersion = buf.readByte().toInt()
                }
            }
            logger.info(
                "modern forwarding request version={} for {}",
                requestedVersion,
                session.username
            )

            // build forwarding data — uses HMAC like velocity does
            val playerAddr = session.clientChannel?.remoteAddress()?.toString() ?: "127.0.0.1"
            val cleanAddr = playerAddr.substringAfter("/").substringBefore(":")
            val uuid = session.uuid ?: return false
            val forwardingData = VelocityModernForwarder.createForwardingData(
                secret,
                cleanAddr,
                uuid,
                session.username,
                requestedVersion
            )
            // response packet: id = 0x02, VarInt msgId + boolean true + data
            val respBuf = Unpooled.buffer()
            VarInt.writeVarInt(respBuf, msgId)
            respBuf.writeBoolean(true)
            respBuf.writeBytes(forwardingData)
            forwardingData.release()

            val respPacket = MinecraftPacket(0x02, respBuf)
            ctx.writeAndFlush(respPacket).addListener { f ->
                respBuf.release()
                if (!f.isSuccess) {
                    logger.warn(
                        "failed modern forwarding response for {}",
                        session.username,
                        f.cause()
                    )
                } else {
                    logger.info("sent modern forwarding response for {}", session.username)
                }
            }
            return true
        } catch (e: Exception) {
            logger.warn("failed to handle modern forwarding for {}", session.username, e)
            return false
        } finally {
            // don't release original packet.data — SimpleChannelInboundHandler will
        }
    }

    private fun forwardToClient(packet: MinecraftPacket, client: io.netty.channel.Channel, backendCtx: ChannelHandlerContext) {
        if (!client.isActive) {
            logger.debug(
                "client not active, dropping backend packet id={} for {}",
                packet.id,
                session.username
            )
            return
        }
        if (!client.isWritable) {
            logger.debug(
                "client not writable, dropping packet id={} for {} (backpressure)",
                packet.id,
                session.username
            )
            // could queue but for backend->client we just drop and let backend read() pause?
            // for now wait a bit then forward
            backendCtx.channel().config().isAutoRead = false
            client.eventLoop().schedule({
                if (client.isWritable && client.isActive) {
                    val dup = packet.data.retainedDuplicate()
                    val fwd = MinecraftPacket(packet.id, dup)
                    client.writeAndFlush(fwd).addListener { fut ->
                        dup.release()
                        if (!fut.isSuccess) {
                            logger.warn(
                                "failed deferred forward to client",
                                fut.cause()
                            )
                        }
                        backendCtx.read()
                    }
                } else {
                    backendCtx.read()
                }
            }, 10, java.util.concurrent.TimeUnit.MILLISECONDS)
            return
        }
        val dup = packet.data.retainedDuplicate()
        val fwd = MinecraftPacket(packet.id, dup)
        client.writeAndFlush(fwd).addListener { future ->
            dup.release()
            if (!future.isSuccess) {
                logger.warn(
                    "failed forward to client for {}",
                    session.username,
                    future.cause()
                )
            }
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
        session.closeBoth("backend exception: ${cause.message}")
        ctx.close()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.info("backend disconnected for {}", session.username)
        // only close client if session not already closed — idempotent
        if (!session.isClosed()) {
            session.closeBoth("backend inactive")
        }
        super.channelInactive(ctx)
    }

    override fun channelWritabilityChanged(ctx: ChannelHandlerContext) {
        if (ctx.channel().isWritable) ctx.read()
        super.channelWritabilityChanged(ctx)
    }
}
