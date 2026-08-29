@file:Suppress("ktlint")

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
import kz.bejiihiu.candiriya.player.NettyPlayerConnection
import kz.bejiihiu.candiriya.player.Player
import kz.bejiihiu.candiriya.player.PlayerManager
import kz.bejiihiu.candiriya.player.PlayerState
import kz.bejiihiu.candiriya.protocol.ConnectionState
import kz.bejiihiu.candiriya.protocol.MinecraftPacket
import kz.bejiihiu.candiriya.protocol.StringUtil
import kz.bejiihiu.candiriya.protocol.VarInt
import kz.bejiihiu.candiriya.scheduler.Scheduler
import kz.bejiihiu.candiriya.scheduler.context.ContextRegistry
import kz.bejiihiu.candiriya.scheduler.tick.TickScheduler
import kz.bejiihiu.candiriya.server.RegisteredServer
import kz.bejiihiu.candiriya.server.ServerRegistry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.apache.logging.log4j.LogManager

@SuppressFBWarnings(
    value = ["URF_UNREAD_FIELD", "DE_MIGHT_IGNORE", "DLS_DEAD_LOCAL_STORE", "EI_EXPOSE_REP", "EI_EXPOSE_REP2", "BC_BAD_CAST_TO_ABSTRACT_COLLECTION", "REC_CATCH_EXCEPTION", "RpC_REPEATED_CONDITIONAL_TEST", "BC_VACUOUS_INSTANCEOF", "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE"],
    justification = "backendConnection is used for lifecycle, dead store is kotlin's fault xd"
)
public class ConnectionHandler(
    private val config: ProxyConfig,
    private val scheduler: Scheduler? = null,
    private val tickScheduler: TickScheduler? = null,
    private val contextRegistry: ContextRegistry? = null,
    private val playerManager: PlayerManager? = null,
    private val serverRegistry: ServerRegistry? = null
) : SimpleChannelInboundHandler<MinecraftPacket>() {

    private val logger = LogManager.getLogger(ConnectionHandler::class.java)

    public var state: ConnectionState = ConnectionState.HANDSHAKE
        private set

    public fun setState(newState: ConnectionState) {
        state = newState
        session.transitionTo(newState)
        player?.let { p ->
            val target = mapToPlayerState(newState)
            if (target != null) p.transitionTo(target)
        }
    }

    private var packetCounter: Long = 0
    private val session = kz.bejiihiu.candiriya.network.session.ProxySession(config)
    private var backendConnection: BackendConnection? = null
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
            scheduler?.execute { logger.debug("first packet from {} id={}", ctx.channel().remoteAddress(), packet.id) }
        }
        val p = player
        if (p != null && contextRegistry != null && !p.isOnContext()) {
            packet.data.retain()
            p.context.execute(Runnable {
                try { handlePacket(ctx, packet) } finally {
                    packet.data.release()
                    ensureRead(ctx)
                }
            })
            return
        }
        try { handlePacket(ctx, packet) } finally {
            if (p == null || p.isOnContext()) ensureRead(ctx)
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
            if (backend == null || backend.isWritable) ctx.read()
            else ctx.channel().eventLoop().schedule({ if (ctx.channel().isActive) ctx.read() }, 10, java.util.concurrent.TimeUnit.MILLISECONDS)
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
                ctx.close(); return
            }
            val proto = VarInt.readVarInt(buf)
            val addr = StringUtil.readString(buf, 255)
            val port = buf.readUnsignedShort()
            val nextStateVal = VarInt.readVarInt(buf)
            logger.info("handshake proto={} addr={}:{} next={} from {}", proto, addr, port, nextStateVal, ctx.channel().remoteAddress())
            state = when (nextStateVal) {
                1 -> ConnectionState.STATUS
                2 -> ConnectionState.LOGIN
                else -> { logger.warn("unknown nextState {} closing {}", nextStateVal, ctx.channel().remoteAddress()); ctx.close(); return }
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
                    } catch (e: Exception) { outBuf.release(); throw e }
                }
                0x01 -> {
                    if (buf.readableBytes() < 8) { logger.warn("ping packet too short from {}", ctx.channel().remoteAddress()); ctx.close(); return }
                    val payload = buf.readLong()
                    val respBuf = Unpooled.buffer(8)
                    respBuf.writeLong(payload)
                    val outPacket = MinecraftPacket(0x01, respBuf)
                    ctx.writeAndFlush(outPacket).addListener { respBuf.release(); ctx.close() }
                }
                else -> { logger.warn("unknown status packet id={} from {}", packet.id, ctx.channel().remoteAddress()); ctx.read() }
            }
        } catch (e: Exception) { logger.warn("bad status packet from {}", ctx.channel().remoteAddress(), e); ctx.close() }
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
                    val resolvedUuid = uuid ?: kz.bejiihiu.candiriya.protocol.UuidUtil.offlineUuid(username)
                    session.username = username
                    session.uuid = resolvedUuid

                    if (playerManager != null && contextRegistry != null) {
                        try {
                            val conn = NettyPlayerConnection(ctx.channel())
                            // pick default server from registry, fallback to try list
                            val initialServer = serverRegistry?.defaultServer()
                                ?: serverRegistry?.tryServers()?.firstOrNull()
                            val pl = playerManager.create(resolvedUuid, username, conn, initialServer)
                            if (serverRegistry != null) pl.setServerRegistry(serverRegistry)
                            pl.transitionTo(PlayerState.LOGIN)
                            player = pl
                            session.currentServer = initialServer
                            logger.info("player {} bound to ctx {} server={} (players in ctx={})", username, pl.context.id, initialServer?.name, playerManager.getByContext(pl.context.id).size)
                            if (config.security.onlineMode && scheduler != null) {
                                pl.transitionTo(PlayerState.AUTHENTICATING)
                                scheduler.execute(Runnable {
                                    try { Thread.sleep(10) } catch (_: InterruptedException) {}
                                    pl.context.execute(Runnable {
                                        pl.transitionTo(PlayerState.CONNECTING)
                                        connectWithFallback(ctx, null)
                                    })
                                })
                                return
                            } else {
                                pl.transitionTo(PlayerState.CONNECTING)
                            }
                        } catch (e: Exception) { logger.warn("failed to create player for {}", username, e) }
                    }
                    connectWithFallback(ctx, null)
                }
                else -> forwardToBackend(ctx, packet)
            }
        } catch (e: Exception) { logger.warn("bad login packet from {}", ctx.channel().remoteAddress(), e); ctx.close() }
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
        if (player?.state == PlayerState.CONNECTING) player?.transitionTo(PlayerState.PLAYING)
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
                ctx.writeAndFlush(disc).addListener { disc.data.release(); session.closeBoth("queue overflow"); player?.transitionTo(PlayerState.DISCONNECTED) }
            }
            return
        }
        if (!backend.isWritable) {
            logger.debug("backend not writable for {}, queuing packet id={}", session.username, packet.id)
            val queued = session.enqueueForBackend(packet)
            if (!queued) { session.closeBoth("backend clogged"); player?.transitionTo(PlayerState.DISCONNECTED) }
            return
        }
        val dup = packet.data.retainedDuplicate()
        val fwd = MinecraftPacket(packet.id, dup)
        backend.writeAndFlush(fwd).addListener { fut ->
            dup.release()
            if (!fut.isSuccess) {
                logger.warn("failed forward to backend for {}", session.username, fut.cause())
                if (!session.isClosed()) { session.closeBoth("backend write failed"); player?.transitionTo(PlayerState.DISCONNECTED) }
            } else { if (ctx.channel().isActive) ctx.read() }
            try { backend.read() } catch (_: Exception) {}
        }
    }

    /** Connect with fallback chain over try order. If [explicitTarget] given, try it first then fallbacks. */
    private fun connectWithFallback(ctx: ChannelHandlerContext, explicitTarget: RegisteredServer?) {
        if (session.backendState == BackendState.CONNECTING || session.backendState == BackendState.CONNECTED) {
            logger.debug("already connecting/connected for {}, ignoring duplicate connect", session.username)
            return
        }
        val registry = serverRegistry
        if (registry == null || registry.count() == 0) {
            logger.warn("no servers configured for {}", session.username)
            val reason = Component.text("No servers configured").color(NamedTextColor.RED)
            val disc = createDisconnectPacket(reason)
            ctx.writeAndFlush(disc).addListener { disc.data.release(); session.closeBoth("no servers"); player?.let { playerManager?.remove(it.uuid) }; ctx.close() }
            return
        }
        val chain: List<RegisteredServer> = when {
            explicitTarget != null -> {
                val fallbacks = registry.availableFallbacks(explicitTarget)
                listOf(explicitTarget) + fallbacks
            }
            else -> registry.tryServers().filter { registry.isAvailable(it) }
        }
        if (chain.isEmpty()) {
            val reason = Component.text("All servers are currently unavailable").color(NamedTextColor.RED)
            val disc = createDisconnectPacket(reason)
            ctx.writeAndFlush(disc).addListener { disc.data.release(); session.closeBoth("all unavailable"); player?.let { playerManager?.remove(it.uuid) }; ctx.close() }
            return
        }
        attemptChain(ctx, chain, 0)
    }

    private fun attemptChain(ctx: ChannelHandlerContext, chain: List<RegisteredServer>, index: Int) {
        if (index >= chain.size) {
            val reason = Component.text("Could not connect to any server").color(NamedTextColor.RED)
            val disc = createDisconnectPacket(reason)
            ctx.writeAndFlush(disc).addListener { disc.data.release(); session.closeBoth("all backends failed"); player?.transitionTo(PlayerState.DISCONNECTED); player?.let { playerManager?.remove(it.uuid) }; ctx.close() }
            return
        }
        val target = chain[index]
        logger.info("attempting connect for {} -> {} ({}/{})", session.username, target.name, index + 1, chain.size)
        val conn = BackendConnection(session, config, target)
        backendConnection = conn
        conn.connect(ctx.channel(), onConnected = { _ ->
            session.drainQueueToBackend()
            session.currentServer = target
            player?.server = target
            serverRegistry?.markAvailable(target)
            if (state == ConnectionState.LOGIN) {
                state = ConnectionState.CONFIGURATION
                session.transitionTo(ConnectionState.CONFIGURATION)
            }
            player?.let { p -> if (p.state == PlayerState.CONNECTING) p.transitionTo(PlayerState.PLAYING) }
            if (ctx.channel().isActive) ctx.read()
        }, onFailed = { cause ->
            logger.warn("failed to connect {} to {}: {}", session.username, target.name, cause.message, cause)
            serverRegistry?.markUnavailable(target)
            // try next in chain
            attemptChain(ctx, chain, index + 1)
        })
    }

    /** Called by BackendHandler when backend disconnects unexpectedly. */
    public fun handleBackendDisconnect(cause: String? = null) {
        if (session.isClosed()) return
        val current = session.currentServer ?: player?.server
        val registry = serverRegistry
        if (config.servers.failoverOnUnexpectedDisconnect && registry != null && state != ConnectionState.CLOSED) {
            val fallback = registry.fallbackFor(current)
            if (fallback != null) {
                logger.info("backend {} for {} disconnected ({}), failing over to {}", current?.name, session.username, cause, fallback.name)
                // notify player via scheduler/context
                val p = player
                if (p != null) {
                    // hop to context
                    val task = Runnable {
                        // send message to player via connection
                        try {
                            val clientCh = session.clientChannel
                            if (clientCh != null && clientCh.isActive) {
                                session.closeBackendOnly("failover to ${fallback.name}")
                                val ctx = clientCh.pipeline().context("connection")
                                connectWithFallback(ctx, fallback)
                            }
                        } catch (e: Exception) { logger.warn("failover failed for {}", session.username, e) }
                    }
                    if (p.isOnContext()) task.run() else p.context.execute(task)
                    return
                } else {
                    // no player, try direct
                    session.closeBackendOnly("failover to ${fallback.name}")
                    // need ctx — fallback not possible without client ctx, just close both
                }
            }
        }
        // no fallback or failover disabled -> kick
        logger.info("no fallback for {} (current={}), kicking", session.username, current?.name)
        val client = session.clientChannel
        if (client != null && client.isActive) {
            val reason = Component.text("Disconnected from ${current?.name ?: "server"}").color(NamedTextColor.RED)
            val disc = createDisconnectPacket(reason)
            client.writeAndFlush(disc).addListener { disc.data.release(); session.closeBoth("backend disconnect no fallback"); player?.transitionTo(PlayerState.DISCONNECTED); player?.let { playerManager?.remove(it.uuid) }; client.close() }
        } else {
            session.closeBoth("backend disconnect no client")
        }
    }

    @SuppressFBWarnings(value = ["SA_LOCAL_SELF_ASSIGNMENT"], justification = "kotlin try-catch generates self assign bytecode")
    private fun buildStatusJson(): String {
        val online = playerManager?.count() ?: 0
        val motdComponent: Component = try { MiniMessage.miniMessage().deserialize(config.status.motd) } catch (_: Exception) { Component.text(config.status.motd) }
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
