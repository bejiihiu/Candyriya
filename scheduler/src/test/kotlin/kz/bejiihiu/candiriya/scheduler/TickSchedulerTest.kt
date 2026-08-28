package kz.bejiihiu.candiriya.scheduler

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kz.bejiihiu.candiriya.config.ProxyConfig
import kz.bejiihiu.candiriya.scheduler.threads.ThreadController
import kz.bejiihiu.candiriya.scheduler.tick.TickScheduler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

public class TickSchedulerTest {
    private val controller = ThreadController(ProxyConfig())
    private val tickScheduler = TickScheduler(controller, 20)

    @AfterEach
    public fun tearDown() {
        tickScheduler.close()
        controller.close()
    }

    @Test
    public fun `getCurrentTick increments`() {
        tickScheduler.start()
        val start = tickScheduler.getCurrentTick()
        Thread.sleep(100)
        assertThat(tickScheduler.getCurrentTick()).isGreaterThan(start)
    }

    @Test
    public fun `delayTicks schedules after ticks`() {
        tickScheduler.start()
        val latch = CountDownLatch(1)
        tickScheduler.runAtTick(2) { latch.countDown() }
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    public fun `periodTicks repeats`() {
        tickScheduler.start()
        val counter = AtomicInteger(0)
        val latch = CountDownLatch(3)
        tickScheduler.runAtTick(1, 1) {
            counter.incrementAndGet()
            latch.countDown()
        }
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
        assertThat(counter.get()).isGreaterThanOrEqualTo(3)
    }

    @Test
    public fun `cancel tick task`() {
        tickScheduler.start()
        val counter = AtomicInteger(0)
        val handle = tickScheduler.runAtTick(5) { counter.incrementAndGet() }
        handle.cancel()
        Thread.sleep(200)
        assertThat(counter.get()).isEqualTo(0)
        assertThat(handle.isCancelled).isTrue()
    }

    @Test
    public fun `zero delay runs on next tick`() {
        tickScheduler.start()
        val latch = CountDownLatch(1)
        tickScheduler.runAtTick(0) { latch.countDown() }
        assertThat(latch.await(500, TimeUnit.MILLISECONDS)).isTrue()
    }
}
