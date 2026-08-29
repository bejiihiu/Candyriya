@file:Suppress("ktlint")

package kz.bejiihiu.candyriya.network

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.CorruptedFrameException
import io.netty.handler.codec.DecoderException
import io.netty.handler.timeout.ReadTimeoutException
import kz.bejiihiu.candyriya.config.ProxyConfig
import kz.bejiihiu.candyriya.network.session.BackendState
import kz.bejiihiu.candyriya.player.NettyPlayerConnection
import kz.bejiihiu.candyriya.player.Player
import kz.bejiihiu.candyriya.player.PlayerManager
import kz.bejiihiu.candyriya.player.PlayerState
import kz.bejiihiu.candyriya.player.RegisteredServer
import kz.bejiihiu.candyriya.protocol.ConnectionState
import kz.bejiihiu.candyriya.protocol.MinecraftPacket
import kz.bejiihiu.candyriya.protocol.StringUtil
import kz.bejiihiu.candyriya.protocol.VarInt
import kz.bejiihiu.candyriya.scheduler.Scheduler
import kz.bejiihiu.candyriya.scheduler.context.ContextRegistry
import kz.bejiihiu.candyriya.scheduler.tick.TickScheduler
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.apache.logging.log4j.LogManager

/**
 * Handles client→proxy packets. Now with Player + ExecutionContext affinity.
 *
 * Flow:
 * - HANDSHAKE/STATUS — on Netty worker thread (no player yet, cheap).
 * - LOGIN/CONFIG/PLAY — hop to player's ExecutionContext (like Folia region thread).
 *   Player A+B -> ctx1, C -> ctx2 etc. All mutations happen strictly on ctx thread.
 * - AUTHENTICATING hops via scheduler.asyncPool (virtual threads) then back to ctx.
 *
 * Uses our own Scheduler/ThreadController for async work — no extra pools.
 */
@SuppressFBWarnings(
    value = ["URF_UNREAD_FIELD", "DE_MIGHT_IGNORE", "DLS_DEAD_LOCAL_STORE", "EI_EXPOSE_REP", "EI_EXPOSE_REP2", "BC_BAD_CAST_TO_ABSTRACT_COLLECTION", "REC_CATCH_EXCEPTION", "RpC_REPEATED_CONDITIONAL_TEST"],
    justification = "backendConnection is used for lifecycle, dead store is kotlin's fault xd"
)
public class ConnectionHandler(
    private val config: ProxyConfig,
    private val scheduler: Scheduler? = null,
    private val tickScheduler: TickScheduler? = null,
    private val contextRegistry: ContextRegistry? = null,
    private val playerManager: PlayerManager? = null
) : SimpleChannelInboundHandler<MinecraftPacket>() {

    private val logger = LogManager.getLogger(ConnectionHandler::class.java)

    public var state: ConnectionState = ConnectionState.HANDSHAKE
        private set

    public fun setState(newState: ConnectionState) {
        state = newState
        session.transitionTo(newState)
        // also bump player state if present
        player?.let { p ->
            val target = mapToPlayerState(newState)
            if (target != null) p.transitionTo(target)
        }
    }

    private var packetCounter: Long = 0

    private val session = kz.bejiihiu.candyriya.network.session.ProxySession(config)

    private var backendConnection: BackendConnection? = null

    // player bound after LOGIN 0x00 — null before handshake
    private var player: Player? = null

    override fun channelActive(ctx: ChannelHandlerContext) {
        state = ConnectionState.HANDSHAKE
        session.transitionTo(ConnectionState.HANDSHAKE)
        session.backendState = BackendState.IDLE
        session.setClient(ctx)
        logger.info("client {} connected", ctx.channel().remoteAddress())
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

        // if player exists and we have registry, ensure we run on player's context thread
        // like Folia's isOwnedByCurrentRegion check — hop if needed
        val p = player
        if (p != null && contextRegistry != null && !p.isOnContext()) {
            // retain packet for async hop — SimpleChannelInboundHandler will release original after return
            packet.data.retain()
            p.context.execute(
                Runnable {
                    try {
                        handlePacket(ctx, packet)
                    } finally {
                        packet.data.release()
                        // resume reading after hop — backpressure dance
                        ensureRead(ctx)
                    }
                }
            )
            return
        }

        try {
            handlePacket(ctx, packet)
        } finally {
            // only auto-read if we didn't hop (hopped path does its own read)
            if (p == null || p.isOnContext()) {
                ensureRead(ctx)
            }
        }
    }

    private fun handlePacket(ctx: ChannelHandlerContext, packet: MinecraftPacket) {
        when (state) {
            ConnectionState.HANDSHAKE -> handleHandshake(ctx, packet)
            ConnectionState.STATUS -> handleStatus(ctx, packet)
            ConnectionState.LOGIN -> handleLogin(ctx, packet)
            ConnectionState.CONFIGURATION -> handleConfiguration(ctx, packet)
            ConnectionState.PLAY -> handlePlay(ctx, packet)
            ConnectionState.CLOSED -> logger.warn("packet in closed state id={}", packet.id)
        }
    }

    private fun ensureRead(ctx: ChannelHandlerContext) {
        if (state != ConnectionState.CLOSED && ctx.channel().isActive) {
            val backend = session.backendChannel
            if (backend == null || backend.isWritable) {
                ctx.read()
            } else {
                ctx.channel().eventLoop().schedule(
                    { if (ctx.channel().isActive) ctx.read() },
                    10,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
            }
        }
    }

    private fun mapToPlayerState(cs: ConnectionState): PlayerState? = when (cs) {
        ConnectionState.HANDSHAKE -> PlayerState.HANDSHAKE
        ConnectionState.LOGIN -> PlayerState.LOGIN
        ConnectionState.CONFIGURATION -> PlayerState.CONNECTING
        ConnectionState.PLAY -> PlayerState.PLAYING
        ConnectionState.CLOSED -> PlayerState.DISCONNECTED
        else -> null
    }

    private fun handleHandshake(ctx: ChannelHandlerContext, packet: MinecraftPacket) {
        val buf = packet.data
        try {
            if (packet.id != 0x00) {
                logger.warn("unexpected handshake packet id={} from {}", packet.id, ctx.channel().remoteAddress())
                ctx.close()
                return
            }
            val proto = VarInt.readVarInt(buf)
            val addr = StringUtil.readString(buf, 255)
            val port = buf.readUnsignedShort()
            val nextStateVal = VarInt.readVarInt(buf)
            logger.info("handshake proto={} addr={}:{} next={} from {}", proto, addr, port, nextStateVal, ctx.channel().remoteAddress())
            state = when (nextStateVal) {
                1 -> ConnectionState.STATUS
                2 -> ConnectionState.LOGIN
                else -> {
                    logger.warn("unknown nextState {} closing {}", nextStateVal, ctx.channel().remoteAddress())
                    ctx.close()
                    return
                }
            }
            session.transitionTo(state)
            session.protocolVersion = proto
            session.serverAddress = addr
            session.serverPort = port
            player?.transitionTo(mapToPlayerState(state) ?: PlayerState.HANDSHAKE)
        } catch (e: Exception) {
            logger.warn("bad handshake from {}", ctx.channel().remoteAddress(), e)
            ctx.close()
        }
    }

    @SuppressFBWarnings(value = ["DLS_DEAD_LOCAL_STORE"], justification = "outPacket used for write")
    private fun handleStatus(ctx: ChannelHandlerContext, packet: MinecraftPacket) {
        val buf = packet.data
        try {
            when (packet.id) {
                0x00 -> {
                    val json = buildStatusJson()
                    val outBuf = Unpooled.buffer()
                    try {
                        StringUtil.writeString(outBuf, json)
                        val outPacket = MinecraftPacket(0x00, outBuf)
                        ctx.writeAndFlush(outPacket).addListener { future ->
                            if (!future.isSuccess) logger.warn("failed to send status response", future.cause())
                            outBuf.release()
                            if (ctx.channel().isActive) ctx.read()
                        }
                    } catch (e: Exception) {
                        outBuf.release()
                        throw e
                    }
                }
                0x01 -> {
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
                    logger.warn("unknown status packet id={} from {}", packet.id, ctx.channel().remoteAddress())
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
                    logger.info("login start username={} uuid={} from {}", username, uuid, ctx.channel().remoteAddress())
                    val resolvedUuid = uuid ?: kz.bejiihiu.candyriya.protocol.UuidUtil.offlineUuid(username)
                    session.username = username
                    session.uuid = resolvedUuid

                    // create Player if manager present — assign to ExecutionContext
                    if (playerManager != null && contextRegistry != null) {
                        try {
                            val conn = NettyPlayerConnection(ctx.channel())
                            val server = RegisteredServer("default", config.backend.host, config.backend.port)
                            val pl = playerManager.create(resolvedUuid, username, conn, server)
                            // sync initial states
                            pl.transitionTo(PlayerState.LOGIN)
                            // tie session username/uuid already done
                            player = pl
                            logger.info(
                                "player {} bound to ctx {} (players in ctx={})",
                                username,
                                pl.context.id,
                                playerManager.getByContext(pl.context.id).size
                            )

                            // if onlineMode, hop to async pool then back to context — demonstrate our Scheduler
                            if (config.security.onlineMode && scheduler != null) {
                                pl.transitionTo(PlayerState.AUTHENTICATING)
                                scheduler.execute(
                                    Runnable {
                                        // fake async auth — virtual thread via our asyncPool
                                        try {
                                            Thread.sleep(10)
                                        } catch (_: InterruptedException) {}
                                        // hop back to context thread
                                        pl.context.execute(
                                            Runnable {
                                                pl.transitionTo(PlayerState.CONNECTING)
                                                connectToBackend(ctx)
                                            }
                                        )
                                    }
                                )
                                return
                            } else {
                                pl.transitionTo(PlayerState.CONNECTING)
                            }
                        } catch (e: Exception) {
                            logger.warn("failed to create player for {}", username, e)
                        }
                    }

                    connectToBackend(ctx)
                }
                else -> forwardToBackend(ctx, packet)
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
            player?.transitionTo(PlayerState.PLAYING)
        }
        forwardToBackend(ctx, packet)
    }

    private fun handlePlay(ctx: ChannelHandlerContext, packet: MinecraftPacket) {
        // ensure player is PLAYING
        if (player?.state == PlayerState.CONNECTING) {
            player?.transitionTo(PlayerState.PLAYING)
        }
        forwardToBackend(ctx, packet)
    }

    private fun forwardToBackend(ctx: ChannelHandlerContext, packet: MinecraftPacket) {
        val backend = session.backendChannel
        if (backend == null || !backend.isActive || session.backendState != BackendState.CONNECTED) {
            val queued = session.enqueueForBackend(packet)
            if (!queued) {
                logger.warn("queue full, disconnecting {}", session.username)
                val reason = Component.text("Proxy queue overflow — try again").color(NamedTextColor.RED)
                val disc = createDisconnectPacket(reason)
                ctx.writeAndFlush(disc).addListener {
                    disc.data.release()
                    session.closeBoth("queue overflow")
                    player?.transitionTo(PlayerState.DISCONNECTED)
                }
            }
            return
        }

        if (!backend.isWritable) {
            logger.debug("backend not writable for {}, queuing packet id={}", session.username, packet.id)
            val queued = session.enqueueForBackend(packet)
            if (!queued) {
                session.closeBoth("backend clogged")
                player?.transitionTo(PlayerState.DISCONNECTED)
            }
            return
        }

        val dup = packet.data.retainedDuplicate()
        val fwd = MinecraftPacket(packet.id, dup)
        backend.writeAndFlush(fwd).addListener { fut ->
            dup.release()
            if (!fut.isSuccess) {
                logger.warn("failed forward to backend for {}", session.username, fut.cause())
                if (!session.isClosed()) {
                    session.closeBoth("backend write failed")
                    player?.transitionTo(PlayerState.DISCONNECTED)
                }
            } else {
                if (ctx.channel().isActive) ctx.read()
            }
            try {
                backend.read()
            } catch (_: Exception) {}
        }
    }

    private fun connectToBackend(ctx: ChannelHandlerContext) {
        if (session.backendState == BackendState.CONNECTING || session.backendState == BackendState.CONNECTED) {
            logger.debug("already connecting/connected for {}, ignoring duplicate connect", session.username)
            return
        }
        val conn = BackendConnection(session, config)
        backendConnection = conn
        conn.connect(
            clientChannel = ctx.channel(),
            onConnected = { _ ->
                session.drainQueueToBackend()
                // bump states
                if (state == ConnectionState.LOGIN) {
                    state = ConnectionState.CONFIGURATION
                    session.transitionTo(ConnectionState.CONFIGURATION)
                }
                player?.let { p ->
                    if (p.state == PlayerState.CONNECTING) p.transitionTo(PlayerState.PLAYING)
                }
                if (ctx.channel().isActive) ctx.read()
            },
            onFailed = { cause ->
                logger.warn("could not connect {} to backend", session.username, cause)
                val msg = cause.message ?: "unknown"
                val reason = Component.text("Could not connect to backend: $msg").color(NamedTextColor.RED)
                val disc = createDisconnectPacket(reason)
                ctx.writeAndFlush(disc).addListener {
                    disc.data.release()
                    session.closeBoth("backend connect failed: $msg")
                    player?.transitionTo(PlayerState.DISCONNECTED)
                    // remove from manager
                    player?.let { playerManager?.remove(it.uuid) }
                    ctx.close()
                }
            }
        )
    }

    @SuppressFBWarnings(value = ["UPM_UNCALLED_PRIVATE_METHOD"], justification = "kept for future fallback")
    private fun handleLoginDisconnect(ctx: ChannelHandlerContext, packet: MinecraftPacket) {
        logger.info("login packet id={} from {} — disconnecting (login not implemented)", packet.id, ctx.channel().remoteAddress())
        val reason = Component.text("Login not implemented yet").color(NamedTextColor.RED)
        val disconnect = createDisconnectPacket(reason)
        ctx.writeAndFlush(disconnect).addListener { _ ->
            disconnect.data.release()
            ctx.close()
        }
    }

    @SuppressFBWarnings(value = ["SA_LOCAL_SELF_ASSIGNMENT"], justification = "kotlin try-catch generates self assign bytecode")
    private fun buildStatusJson(): String {
        val online = playerManager?.count() ?: 0
        val motdComponent: Component = try {
            MiniMessage.miniMessage().deserialize(config.status.motd)
        } catch (_: Exception) {
            Component.text(config.status.motd)
        }
        val motdJson: String = GsonComponentSerializer.gson().serialize(motdComponent)
        val versionNameEsc = config.status.versionName.replace("\\", "\\\\").replace("\"", "\\\"")
        val versionPart = """{"version":{"name":"$versionNameEsc","protocol":${config.status.versionProtocol}}"""
        val playersPart = ""","players":{"max":${config.status.maxPlayers},"online":$online}"""
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
            is CorruptedFrameException, is DecoderException -> logger.warn("bad packet from {}", ctx.channel().remoteAddress(), cause)
            else -> logger.warn("exception on {}", ctx.channel().remoteAddress(), cause)
        }
        session.closeBoth("exception: ${cause.message}")
        player?.transitionTo(PlayerState.DISCONNECTED)
        player?.let { playerManager?.remove(it.uuid) }
        ctx.close()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.info("client {} disconnected", ctx.channel().remoteAddress())
        state = ConnectionState.CLOSED
        session.closeBoth()
        player?.transitionTo(PlayerState.DISCONNECTED)
        player?.let { playerManager?.remove(it.uuid) }
        super.channelInactive(ctx)
    }

    override fun channelWritabilityChanged(ctx: ChannelHandlerContext) {
        if (ctx.channel().isWritable) ctx.read()
        super.channelWritabilityChanged(ctx)
    }
}
