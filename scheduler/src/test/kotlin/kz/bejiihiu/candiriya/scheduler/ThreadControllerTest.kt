package kz.bejiihiu.candiriya.scheduler

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kz.bejiihiu.candiriya.config.ProxyConfig
import kz.bejiihiu.candiriya.config.ThreadsConfig
import kz.bejiihiu.candiriya.scheduler.threads.ThreadController
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

public class ThreadControllerTest {

    @Test
    public fun `platform async pool names threads`() {
        val config = ProxyConfig(threads = ThreadsConfig(virtual = false, scheduledCoreSize = 1))
        val controller = ThreadController(config)
        try {
            val latch = CountDownLatch(1)
            var name: String? = null
            controller.asyncPool.submit {
                name = Thread.currentThread().name
                latch.countDown()
            }
            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
            assertThat(name).contains("candiriya-async-")
        } finally {
            controller.close()
        }
    }

    @Test
    public fun `virtual pool works`() {
        val config = ProxyConfig(threads = ThreadsConfig(virtual = true, scheduledCoreSize = 1))
        val controller = ThreadController(config)
        try {
            val latch = CountDownLatch(1)
            controller.asyncPool.submit { latch.countDown() }
            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
        } finally {
            controller.close()
        }
    }

    @Test
    public fun `scheduled pool executes`() {
        val config = ProxyConfig(threads = ThreadsConfig(virtual = true, scheduledCoreSize = 1))
        val controller = ThreadController(config)
        try {
            val latch = CountDownLatch(1)
            controller.scheduledPool.schedule(
                { latch.countDown() },
                10,
                TimeUnit.MILLISECONDS
            )
            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
        } finally {
            controller.close()
        }
    }

    @Test
    public fun `close terminates pools`() {
        val controller = ThreadController(ProxyConfig())
        controller.start()
        controller.close()
        assertThat(controller.asyncPool.isShutdown).isTrue()
        assertThat(controller.scheduledPool.isShutdown).isTrue()
    }
}
