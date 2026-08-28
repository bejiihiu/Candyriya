package kz.bejiihiu.candiriya.network

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.timeout.ReadTimeoutHandler
import java.util.concurrent.TimeUnit
import kz.bejiihiu.candiriya.config.ProxyConfig
import kz.bejiihiu.candiriya.protocol.MinecraftPacketDecoder
import kz.bejiihiu.candiriya.protocol.MinecraftVarintFrameDecoder
import kz.bejiihiu.candiriya.protocol.MinecraftVarintLengthEncoder
import kz.bejiihiu.candiriya.scheduler.Scheduler
import kz.bejiihiu.candiriya.scheduler.threads.ThreadController
import kz.bejiihiu.candiriya.scheduler.tick.TickScheduler
import org.apache.logging.log4j.LogManager

/**
 * Minimal netty server that binds a port and logs connections.
 * No proxy logic yet, just a vertical slice.
 *
 * Threading is owned by [ThreadController] — no direct NioEventLoopGroup creation here.
 */
public class NetworkServer(
    private val config: ProxyConfig,
    private val threadController: ThreadController,
    private val bossGroup: EventLoopGroup = threadController.createBossGroup(),
    private val workerGroup: EventLoopGroup = threadController.createWorkerGroup(),
    private val scheduler: Scheduler? = null,
    private val tickScheduler: TickScheduler? = null
) {
    private val logger = LogManager.getLogger(NetworkServer::class.java)
    private var channel: Channel? = null

    public fun start(): ChannelFuture {
        val bootstrap = ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            // keepalive + nodelay are standard for mc
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childOption(ChannelOption.TCP_NODELAY, true)
            // backpressure stuff — don't auto-read, we control ctx.read() manually
            // this fixes the 4-byte stall bug from StackOverflow 78088619 xd
            .childOption(ChannelOption.AUTO_READ, false)
            .childOption(ChannelOption.AUTO_CLOSE, false)
            .childOption(
                ChannelOption.WRITE_BUFFER_WATER_MARK,
                io.netty.channel.WriteBufferWaterMark(32 * 1024, 64 * 1024)
            )
            .childHandler(
                object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        if (config.network.readTimeoutSeconds > 0) {
                            ch.pipeline().addLast(
                                "timeout",
                                ReadTimeoutHandler(config.network.readTimeoutSeconds)
                            )
                        }
                        ch.pipeline().addLast(
                            "frameDecoder",
                            MinecraftVarintFrameDecoder(config.protocol.maxPacketSize)
                        )
                        ch.pipeline().addLast("packetDecoder", MinecraftPacketDecoder())
                        ch.pipeline().addLast("packetEncoder", MinecraftVarintLengthEncoder())
                        ch.pipeline().addLast(
                            "connection",
                            ConnectionHandler(config, scheduler, tickScheduler)
                        )
                        ch.pipeline().addLast("logger", LoggingHandler())
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
        // graceful shutdown of event loops — quiet/timeout from config
        val quiet = config.shutdown.quietPeriodMs
        val timeout = config.shutdown.timeoutMs
        logger.info("shutting down event loops quiet={}ms timeout={}ms", quiet, timeout)
        bossGroup.shutdownGracefully(quiet, timeout, TimeUnit.MILLISECONDS).syncUninterruptibly()
        workerGroup.shutdownGracefully(quiet, timeout, TimeUnit.MILLISECONDS).syncUninterruptibly()
        logger.info("event loops terminated")
    }
}
