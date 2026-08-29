package kz.bejiihiu.candyriya.scheduler

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kz.bejiihiu.candyriya.lifecycle.LifecycleState
import kz.bejiihiu.candyriya.scheduler.threads.ThreadController
import org.apache.logging.log4j.LogManager

/**
 * Handle for a scheduled task.
 */
public interface TaskHandle {
    public val id: Long
    public val isCancelled: Boolean
    public val isDone: Boolean
    public fun cancel(): Boolean
    public fun onComplete(callback: (Throwable?) -> Unit)
}

/**
 * Main scheduler interface.
 */
public interface Scheduler : AutoCloseable {
    public fun execute(task: Runnable): TaskHandle

    public fun execute(delay: Duration, task: Runnable): TaskHandle

    public fun scheduleAtFixedRate(initialDelay: Duration, period: Duration, task: Runnable): TaskHandle

    public fun scheduleWithFixedDelay(initialDelay: Duration, delay: Duration, task: Runnable): TaskHandle

    public fun launch(block: suspend () -> Unit): TaskHandle

    public suspend fun <T> async(block: suspend () -> T): Deferred<T>

    public fun cancelAll(): Int
}

/**
 * Default scheduler delegating to [ThreadController] pools.
 *
 * @param threadController backing pools
 * @param lifecycle supplier for current lifecycle state, used to reject tasks when stopping
 */
@SuppressFBWarnings(
    value = ["SE_BAD_FIELD"],
    justification = "coroutine continuation not serialized"
)
public class DefaultScheduler(
    private val threadController: ThreadController,
    private val lifecycle: () -> LifecycleState = { LifecycleState.RUNNING }
) : Scheduler {
    private val logger = LogManager.getLogger(DefaultScheduler::class.java)
    private val idGen = AtomicLong(0)
    private val handles = ConcurrentHashMap<Long, HandleImpl>()

    // TODO: per-plugin quota / cancelAll(pluginId) / metrics

    override fun execute(task: Runnable): TaskHandle {
        checkLifecycle()
        return submitAsync(task)
    }

    override fun execute(delay: Duration, task: Runnable): TaskHandle {
        checkLifecycle()
        if (delay.isZero || delay.isNegative) {
            return submitAsync(task)
        }
        return submitScheduled(delay, task)
    }

    override fun scheduleAtFixedRate(initialDelay: Duration, period: Duration, task: Runnable): TaskHandle {
        checkLifecycle()
        require(!period.isZero && !period.isNegative) { "period must be >0" }
        val id = idGen.incrementAndGet()
        val handle = HandleImpl(id, owner = null)
        val wrapped = wrap(task, handle)
        val future = threadController.scheduledPool.scheduleAtFixedRate(
            wrapped,
            initialDelay.toMillis(),
            period.toMillis(),
            TimeUnit.MILLISECONDS
        )
        handle.future = future
        handles[id] = handle
        logger.debug("scheduledAtFixedRate id={} initial={} period={}", id, initialDelay, period)
        return handle
    }

    override fun scheduleWithFixedDelay(initialDelay: Duration, delay: Duration, task: Runnable): TaskHandle {
        checkLifecycle()
        require(!delay.isZero && !delay.isNegative) { "delay must be >0" }
        val id = idGen.incrementAndGet()
        val handle = HandleImpl(id, owner = null)
        val wrapped = wrap(task, handle)
        val future = threadController.scheduledPool.scheduleWithFixedDelay(
            wrapped,
            initialDelay.toMillis(),
            delay.toMillis(),
            TimeUnit.MILLISECONDS
        )
        handle.future = future
        handles[id] = handle
        logger.debug("scheduledWithFixedDelay id={} initial={} delay={}", id, initialDelay, delay)
        return handle
    }

    override fun launch(block: suspend () -> Unit): TaskHandle {
        checkLifecycle()
        val id = idGen.incrementAndGet()
        val handle = HandleImpl(id, owner = null)
        val job = threadController.schedulerScope.launch {
            try {
                block()
                handle.markDone(null)
            } catch (e: Throwable) {
                logger.error("exception in coroutine task id={}", id, e)
                handle.markDone(e)
            }
        }
        handle.job = job
        job.invokeOnCompletion { ex ->
            if (ex != null && handle.isCancelled) {
                handle.markDone(ex)
            }
        }
        handles[id] = handle
        return handle
    }

    override suspend fun <T> async(block: suspend () -> T): Deferred<T> {
        checkLifecycle()
        // TODO: per-plugin quota / metrics
        return threadController.schedulerScope.async {
            block()
        }
    }

    override fun cancelAll(): Int {
        var count = 0
        for (handle in handles.values) {
            if (handle.cancel()) count++
        }
        handles.clear()
        logger.info("cancelAll removed {} tasks", count)
        return count
    }

    override fun close() {
        cancelAll()
        logger.info("DefaultScheduler closed")
    }

    private fun submitAsync(task: Runnable): TaskHandle {
        val id = idGen.incrementAndGet()
        val handle = HandleImpl(id, owner = null)
        val wrapped = wrap(task, handle)
        val future = threadController.asyncPool.submit(wrapped)
        handle.future = future
        handles[id] = handle
        return handle
    }

    private fun submitScheduled(delay: Duration, task: Runnable): TaskHandle {
        val id = idGen.incrementAndGet()
        val handle = HandleImpl(id, owner = null)
        val wrapped = Runnable {
            // run via async pool to not block scheduled thread xd
            try {
                task.run()
                handle.markDone(null)
            } catch (e: Throwable) {
                logger.error("exception in delayed task id={}", id, e)
                handle.markDone(e)
            }
        }
        val future = threadController.scheduledPool.schedule(
            wrapped,
            delay.toMillis(),
            TimeUnit.MILLISECONDS
        )
        handle.future = future
        handles[id] = handle
        return handle
    }

    private fun wrap(task: Runnable, handle: HandleImpl): Runnable = Runnable {
        try {
            task.run()
        } catch (e: Throwable) {
            logger.error("exception in task id={}", handle.id, e)
            // don't kill scheduler, just log
        }
    }

    private fun checkLifecycle() {
        if (lifecycle() == LifecycleState.STOPPING) {
            logger.warn("rejecting task, lifecycle is STOPPING")
            throw IllegalStateException("scheduler is stopping")
        }
    }

    private class HandleImpl(
        override val id: Long,
        val owner: TaskOwner?
    ) : TaskHandle {
        @Volatile
        var future: java.util.concurrent.Future<*>? = null

        @Volatile
        var job: Job? = null

        private val callbacks = mutableListOf<(Throwable?) -> Unit>()

        @Volatile
        private var cancelled = false

        @Volatile
        private var done = false

        private var doneError: Throwable? = null

        override val isCancelled: Boolean get() = cancelled

        override val isDone: Boolean get() =
            done || future?.isDone == true || job?.isCompleted == true

        override fun cancel(): Boolean {
            if (cancelled || isDone) return false
            cancelled = true
            future?.cancel(false)
            job?.cancel()
            return true
        }

        fun markDone(error: Throwable?) {
            done = true
            doneError = error
            val copy: List<(Throwable?) -> Unit>
            synchronized(callbacks) {
                copy = callbacks.toList()
                callbacks.clear()
            }
            for (cb in copy) {
                try {
                    cb(error)
                } catch (_: Throwable) {
                    // ignore callback errors xd
                }
            }
        }

        override fun onComplete(callback: (Throwable?) -> Unit) {
            var immediate = false
            var err: Throwable? = null
            synchronized(callbacks) {
                if (done || isDone) {
                    immediate = true
                    err = doneError
                } else {
                    callbacks.add(callback)
                }
            }
            if (immediate) {
                callback(err)
            } else {
                // also hook future/job completion
                future?.let { f ->
                    // poll completion via scheduled check? for now onComplete called via markDone
                }
                job?.invokeOnCompletion { ex ->
                    val pending: List<(Throwable?) -> Unit>
                    synchronized(callbacks) {
                        pending = callbacks.toList()
                        callbacks.clear()
                    }
                    for (cb in pending) {
                        try {
                            cb(ex)
                        } catch (_: Throwable) {
                        }
                    }
                }
            }
        }
    }
}
