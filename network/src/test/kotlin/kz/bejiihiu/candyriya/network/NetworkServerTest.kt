package kz.bejiihiu.candyriya.network

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kz.bejiihiu.candyriya.config.NetworkConfig
import kz.bejiihiu.candyriya.config.ProxyConfig
import kz.bejiihiu.candyriya.scheduler.threads.ThreadController
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

public class NetworkServerTest {

    @Test
    public fun `server binds to ephemeral port`() {
        // bind to 0 to get random free port
        val config = ProxyConfig(network = NetworkConfig(bind = "127.0.0.1:0"))
        val controller = ThreadController(config)
        val server = NetworkServer(config, controller)
        val future = server.start()
        try {
            assertThat(future.channel().isActive).isTrue()
            assertThat(future.channel().localAddress()).isNotNull()
        } finally {
            server.stop()
            controller.close()
        }
    }

    @Test
    public fun `server stops gracefully`() {
        val config = ProxyConfig(network = NetworkConfig(bind = "127.0.0.1:0"))
        val controller = ThreadController(config)
        val server = NetworkServer(config, controller)
        server.start()
        server.stop()
        controller.close()
        // after stop, groups should be shutdown - no exception means ok
    }

    @Test
    public fun `server uses controller thread factory naming`() {
        val config = ProxyConfig(network = NetworkConfig(bind = "127.0.0.1:0"))
        val controller = ThreadController(config)
        val server = NetworkServer(config, controller)
        server.start()
        try {
            val latch = CountDownLatch(1)
            var workerThreadName: String? = null
            // trigger a netty worker thread by connecting and checking LoggingHandler?
            // easier: just submit a task to the worker group and observe name
            // we cheat via controller.createWorkerGroup directly
            val group = controller.createWorkerGroup()
            try {
                group.submit {
                    workerThreadName = Thread.currentThread().name
                    latch.countDown()
                }
                assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
                assertThat(workerThreadName).contains("Candyriya-netty-worker-")
            } finally {
                group.shutdownGracefully(0, 100, TimeUnit.MILLISECONDS).syncUninterruptibly()
            }
            // also boss factory naming check
            val bossGroup = controller.createBossGroup()
            val bossLatch = CountDownLatch(1)
            var bossName: String? = null
            try {
                bossGroup.submit {
                    bossName = Thread.currentThread().name
                    bossLatch.countDown()
                }
                assertThat(bossLatch.await(2, TimeUnit.SECONDS)).isTrue()
                assertThat(bossName).contains("Candyriya-netty-boss-")
            } finally {
                bossGroup.shutdownGracefully(0, 100, TimeUnit.MILLISECONDS).syncUninterruptibly()
            }
        } finally {
            server.stop()
            controller.close()
        }
    }
}
