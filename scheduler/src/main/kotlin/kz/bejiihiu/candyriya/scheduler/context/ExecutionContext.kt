@file:Suppress("ktlint")

package kz.bejiihiu.candyriya.scheduler.context

import com.google.common.util.concurrent.ThreadFactoryBuilder
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kz.bejiihiu.candyriya.scheduler.TaskHandle
import org.apache.logging.log4j.LogManager

/**
 * Single-threaded execution context — like Folia region thread.
 * All mutations for players bound to this context must happen on its thread.
 * Uses platform thread (never virtual) to avoid pinning issues.
 *
 * Scheduler integration: tasks submitted via [execute] run strictly ordered.
 * Delayed/repeating tasks hop via [scheduledPool] then to context thread.
 */
@SuppressFBWarnings(
    value = ["SE_BAD_FIELD", "EI_EXPOSE_REP", "UWF_UNWRITTEN_FIELD"],
    justification = "executor not serializable but we never serialize xd"
)
public class ExecutionContext(
    public val id: Int,
    private val scheduledPool: ScheduledExecutorService
) : AutoCloseable {

    private val logger = LogManager.getLogger(ExecutionContext::class.java)

    private val executor: ExecutorService = createExecutor()

    @Volatile
    private var closed = false

    private fun createExecutor(): ExecutorService {
        val factory = ThreadFactoryBuilder()
            .setNameFormat("Candyriya-ctx-$id-%d")
            .setDaemon(false)
            .setUncaughtExceptionHandler { t, e ->
                logger.error("uncaught in context {} thread {}", id, t.name, e)
            }
            .build()
        // single platform thread — strictly ordered like Folia region
        return Executors.newSingleThreadExecutor(factory)
    }

    /** True if current thread is this context's thread. */
    public fun isOwnedByCurrentThread(): Boolean {
        // we check thread name prefix — cheap and reliable for single-thread executor
        // alternative: capture Thread reference after first task, but name check is simpler
        return Thread.currentThread().name.startsWith("Candyriya-ctx-$id-")
    }

    /** Throws if not on context thread — use for invariant checks like Folia. */
    public fun ensureOwned() {
        check(isOwnedByCurrentThread()) {
            "not on context $id thread, current=${Thread.currentThread().name}"
        }
    }

    /** Fire-and-forget on context thread. */
    public fun execute(task: Runnable) {
        if (closed) {
            logger.warn("execute on closed context {}", id)
            return
        }
        executor.execute(wrap(task))
    }

    /** Execute with delay — hop via scheduledPool then to context thread. */
    public fun execute(delay: Duration, task: Runnable): TaskHandle {
        if (delay.isZero || delay.isNegative) {
            execute(task)
            // return dummy handle — not cancellable after dispatch xd
            return NoopHandle()
        }
        val handle = ScheduledHandle()
        val future = scheduledPool.schedule(
            {
                if (!handle.isCancelled) {
                    execute(task)
                    handle.markDone()
                }
            },
            delay.toMillis(),
            TimeUnit.MILLISECONDS
        )
        handle.future = future
        return handle
    }

    public fun scheduleAtFixedRate(initialDelay: Duration, period: Duration, task: Runnable): TaskHandle {
        require(!period.isZero && !period.isNegative) { "period must be >0" }
        val handle = ScheduledHandle()
        val future = scheduledPool.scheduleAtFixedRate(
            {
                if (!handle.isCancelled) {
                    // hop to context thread, don't block scheduled thread
                    execute(task)
                }
            },
            initialDelay.toMillis(),
            period.toMillis(),
            TimeUnit.MILLISECONDS
        )
        handle.future = future
        return handle
    }

    private fun wrap(task: Runnable): Runnable = Runnable {
        try {
            task.run()
        } catch (e: Throwable) {
            logger.error("exception in context {} task", id, e)
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        executor.shutdown()
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            executor.shutdownNow()
        }
        logger.info("ExecutionContext {} closed", id)
    }

    // tiny handles for delayed tasks — not full blown DefaultScheduler handles
    @SuppressFBWarnings(value = ["UWF_UNWRITTEN_FIELD"], justification = "kotlin id field xd")
    private class ScheduledHandle : TaskHandle {
        @Volatile var future: java.util.concurrent.ScheduledFuture<*>? = null

        @Volatile private var cancelled = false

        @Volatile private var done = false
        private val callbacks = mutableListOf<(Throwable?) -> Unit>()

        override val id: Long = 0 // not used for context handles
        override val isCancelled: Boolean get() = cancelled
        override val isDone: Boolean get() = done || future?.isDone == true

        override fun cancel(): Boolean {
            if (cancelled || isDone) return false
            cancelled = true
            future?.cancel(false)
            return true
        }

        fun markDone() {
            done = true
            val copy: List<(Throwable?) -> Unit>
            synchronized(callbacks) {
                copy = callbacks.toList()
                callbacks.clear()
            }
            for (cb in copy) try {
                cb(null)
            } catch (_: Throwable) {}
        }

        override fun onComplete(callback: (Throwable?) -> Unit) {
            synchronized(callbacks) {
                if (done || isDone) callback(null) else callbacks.add(callback)
            }
        }
    }

    @SuppressFBWarnings(value = ["UWF_UNWRITTEN_FIELD"], justification = "kotlin id field xd")
    private class NoopHandle : TaskHandle {
        override val id: Long = -1
        override val isCancelled: Boolean = false
        override val isDone: Boolean = true
        override fun cancel(): Boolean = false
        override fun onComplete(callback: (Throwable?) -> Unit) {
            callback(null)
        }
    }
}
