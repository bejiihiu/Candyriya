package kz.bejiihiu.candyriya.scheduler

import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kz.bejiihiu.candyriya.config.ProxyConfig
import kz.bejiihiu.candyriya.scheduler.threads.ThreadController
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

public class SchedulerTest {
    private val controller = ThreadController(ProxyConfig())
    private val scheduler = DefaultScheduler(controller)

    @AfterEach
    public fun tearDown() {
        scheduler.close()
        controller.close()
    }

    @Test
    public fun `execute immediate runs task`() {
        val latch = CountDownLatch(1)
        scheduler.execute { latch.countDown() }
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    public fun `execute delayed runs after delay`() {
        val latch = CountDownLatch(1)
        val start = System.currentTimeMillis()
        scheduler.execute(Duration.ofMillis(100)) { latch.countDown() }
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
        assertThat(System.currentTimeMillis() - start).isGreaterThanOrEqualTo(80)
    }

    @Test
    public fun `scheduleAtFixedRate repeats`() {
        val counter = AtomicInteger(0)
        val latch = CountDownLatch(3)
        val handle = scheduler.scheduleAtFixedRate(
            Duration.ofMillis(10),
            Duration.ofMillis(30)
        ) {
            counter.incrementAndGet()
            latch.countDown()
        }
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
        handle.cancel()
        assertThat(counter.get()).isGreaterThanOrEqualTo(3)
    }

    @Test
    public fun `cancel prevents execution`() {
        val counter = AtomicInteger(0)
        val handle = scheduler.execute(Duration.ofMillis(200)) { counter.incrementAndGet() }
        handle.cancel()
        Thread.sleep(400)
        assertThat(counter.get()).isEqualTo(0)
        assertThat(handle.isCancelled).isTrue()
    }

    @Test
    public fun `exception does not kill scheduler`() {
        val latch = CountDownLatch(1)
        scheduler.execute {
            throw RuntimeException("boom xd")
        }
        // next task should still run
        Thread.sleep(100)
        scheduler.execute { latch.countDown() }
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    public fun `async coroutine returns result`() {
        runBlocking {
            val deferred = scheduler.async {
                delay(20)
                42
            }
            assertThat(deferred.await()).isEqualTo(42)
        }
    }

    @Test
    public fun `launch coroutine runs`() {
        val latch = CountDownLatch(1)
        scheduler.launch {
            delay(20)
            latch.countDown()
        }
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    public fun `cancelAll cancels pending`() {
        scheduler.execute(Duration.ofMillis(500)) {}
        scheduler.execute(Duration.ofMillis(500)) {}
        val count = scheduler.cancelAll()
        assertThat(count).isGreaterThanOrEqualTo(1)
    }

    @Test
    public fun `scheduleWithFixedDelay repeats`() {
        val counter = AtomicInteger(0)
        val latch = CountDownLatch(2)
        val handle = scheduler.scheduleWithFixedDelay(
            Duration.ofMillis(10),
            Duration.ofMillis(30)
        ) {
            counter.incrementAndGet()
            latch.countDown()
        }
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
        handle.cancel()
        assertThat(counter.get()).isGreaterThanOrEqualTo(2)
    }
}
