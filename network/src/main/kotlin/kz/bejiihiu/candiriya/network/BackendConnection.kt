package kz.bejiihiu.candiriya.network

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.WriteBufferWaterMark
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import kz.bejiihiu.candiriya.config.ProxyConfig
import kz.bejiihiu.candiriya.network.session.BackendState
import kz.bejiihiu.candiriya.network.session.ProxySession
import kz.bejiihiu.candiriya.protocol.MinecraftPacketDecoder
import kz.bejiihiu.candiriya.protocol.MinecraftVarintFrameDecoder
import kz.bejiihiu.candiriya.protocol.MinecraftVarintLengthEncoder
import kz.bejiihiu.candiriya.server.RegisteredServer
import org.apache.logging.log4j.LogManager

/**
 * Owns the outbound connection to a specific backend server.
 * One instance per connect attempt.
 */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP2", "DE_MIGHT_IGNORE", "REC_CATCH_EXCEPTION"], justification = "session shared")
public class BackendConnection(
    private val session: ProxySession,
    private val config: ProxyConfig,
    private val target: RegisteredServer
) {
    private val logger = LogManager.getLogger(BackendConnection::class.java)

    @Volatile
    private var currentFuture: ChannelFuture? = null

    public fun connect(clientChannel: Channel, onConnected: (Channel) -> Unit, onFailed: (Throwable) -> Unit) {
        session.backendState = BackendState.CONNECTING
        session.currentServer = target
        attemptConnect(clientChannel, config.servers.retryAttempts, onConnected, onFailed)
    }

    private fun attemptConnect(clientChannel: Channel, retriesLeft: Int, onConnected: (Channel) -> Unit, onFailed: (Throwable) -> Unit) {
        if (!clientChannel.isActive) {
            logger.warn("client already gone, not connecting to backend for {}", session.username)
            session.failBackend()
            onFailed(IllegalStateException("client closed"))
            return
        }

        val bootstrap = Bootstrap()
            .group(clientChannel.eventLoop())
            .channel(NioSocketChannel::class.java)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.servers.connectTimeoutMs)
            .option(ChannelOption.AUTO_READ, false)
            .option(ChannelOption.AUTO_CLOSE, false)
            .option(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark(32 * 1024, 64 * 1024))
            .handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast("frameDecoder", MinecraftVarintFrameDecoder(config.protocol.maxPacketSize))
                    ch.pipeline().addLast("packetDecoder", MinecraftPacketDecoder())
                    ch.pipeline().addLast("packetEncoder", MinecraftVarintLengthEncoder())
                    ch.pipeline().addLast("backendHandler", BackendHandler(session))
                }
            })

        logger.info("connecting {} to backend {}:{} (retries left={})", session.username, target.host, target.port, retriesLeft)

        val future = bootstrap.connect(target.host, target.port)
        currentFuture = future

        future.addListener { fut ->
            val cf = fut as ChannelFuture
            if (cf.isSuccess) {
                val backendCh = cf.channel()
                session.setBackend(backendCh)
                logger.info("backend connected for {} -> {}:{}", session.username, target.host, target.port)
                try {
                    backendCh.config().isAutoRead = false
                    backendCh.read()
                    clientChannel.read()
                } catch (_: Exception) {}
                onConnected(backendCh)
            } else {
                val cause = cf.cause()
                logger.warn("failed to connect {} to {}:{} — {}", session.username, target.host, target.port, cause?.message, cause)
                if (retriesLeft > 0) {
                    val delay = config.servers.retryDelayMs
                    logger.info("retrying in {}ms for {}", delay, session.username)
                    clientChannel.eventLoop().schedule({
                        attemptConnect(clientChannel, retriesLeft - 1, onConnected, onFailed)
                    }, delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                } else {
                    session.failBackend(cause)
                    onFailed(cause ?: RuntimeException("unknown connect failure"))
                }
            }
        }
    }

    public fun cancel() {
        try {
            currentFuture?.cancel(false)
        } catch (_: Exception) {}
    }
}
