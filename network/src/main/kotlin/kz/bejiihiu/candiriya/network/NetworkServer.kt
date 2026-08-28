package kz.bejiihiu.candiriya.network

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import java.util.concurrent.TimeUnit
import kz.bejiihiu.candiriya.config.ProxyConfig
import org.apache.logging.log4j.LogManager

/**
 * Minimal netty server that binds a port and logs connections.
 * No proxy logic yet, just a vertical slice.
 */
public class NetworkServer(
    private val config: ProxyConfig,
    private val bossGroup: EventLoopGroup = NioEventLoopGroup(1),
    private val workerGroup: EventLoopGroup = createWorkerGroup(config)
) {
    private val logger = LogManager.getLogger(NetworkServer::class.java)
    private var channel: Channel? = null

    public fun start(): ChannelFuture {
        val bootstrap = ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(
                object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        // no-op handler, just log :p
                        ch.pipeline().addLast(LoggingHandler())
                    }
                }
            )

        val host = config.network.host()
        val port = config.network.port()
        logger.info("binding to {}:{}", host, port)
        val future = bootstrap.bind(host, port).syncUninterruptibly()
        channel = future.channel()
        logger.info("bound to {}", channel!!.localAddress())
        return future
    }

    public fun stop() {
        logger.info("closing server channel")
        try {
            channel?.close()?.syncUninterruptibly()
        } catch (e: Exception) {
            logger.warn("error closing channel", e)
        }
        // graceful shutdown of event loops
        val quiet = config.shutdown.quietPeriodMs
        val timeout = config.shutdown.timeoutMs
        logger.info("shutting down event loops quiet={}ms timeout={}ms", quiet, timeout)
        bossGroup.shutdownGracefully(quiet, timeout, TimeUnit.MILLISECONDS).syncUninterruptibly()
        workerGroup.shutdownGracefully(quiet, timeout, TimeUnit.MILLISECONDS).syncUninterruptibly()
        logger.info("event loops terminated")
    }

    public companion object {
        private fun createWorkerGroup(config: ProxyConfig): EventLoopGroup {
            val workers = config.network.workers
            return if (workers > 0) NioEventLoopGroup(workers) else NioEventLoopGroup()
        }
    }
}
