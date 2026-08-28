package kz.bejiihiu.candiriya.scheduler.tick

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kz.bejiihiu.candiriya.scheduler.TaskHandle
import kz.bejiihiu.candiriya.scheduler.threads.ThreadController
import org.apache.logging.log4j.LogManager

/**
 * Tick-based scheduler. One tick = [tickRateMs] milliseconds (default 50ms = 20 tps).
 *
 * Uses a fixed-rate task on [ThreadController.scheduledPool] to increment [currentTick]
 * and drain due tasks.
 */
public class TickScheduler(
    private val threadController: ThreadController,
    private val tickRateMs: Long = 50
) : AutoCloseable {
    private val logger = LogManager.getLogger(TickScheduler::class.java)
    private val currentTick = AtomicLong(0)
    private val tasks = ConcurrentHashMap<Long, TickTask>()
    private val idGen = AtomicLong(0)
    private var tickFuture: ScheduledFuture<*>? = null

    // TODO: per-plugin quota / cancelAll(pluginId) / metrics

    public fun start() {
        tickFuture = threadController.scheduledPool.scheduleAtFixedRate(
            { onTick() },
            tickRateMs,
            tickRateMs,
            TimeUnit.MILLISECONDS
        )
        logger.info("TickScheduler started tickRate={}ms", tickRateMs)
    }

    public fun getCurrentTick(): Long = currentTick.get()

    /**
     * Schedule [task] after [delayTicks] ticks. If [periodTicks] > 0 the task repeats every
     * [periodTicks] ticks.
     */
    public fun runAtTick(delayTicks: Long, periodTicks: Long = 0, task: Runnable): TaskHandle {
        require(delayTicks >= 0) { "delayTicks must be >=0" }
        require(periodTicks >= 0) { "periodTicks must be >=0" }
        val id = idGen.incrementAndGet()
        val target = currentTick.get() + delayTicks
        val handle = TickHandle(id)
        val tickTask = TickTask(
            id = id,
            targetTick = target,
            periodTicks = periodTicks,
            task = task,
            handle = handle
        )
        tasks[id] = tickTask
        logger.debug(
            "runAtTick id={} delay={} period={} target={}",
            id,
            delayTicks,
            periodTicks,
            target
        )
        return handle
    }

    /**
     * Convenience overload for one-shot tick delay.
     */
    public fun runAtTick(delayTicks: Long, task: Runnable): TaskHandle =
        runAtTick(delayTicks, 0, task)

    private fun onTick() {
        val tick = currentTick.incrementAndGet()
        // drain due tasks
        val due = mutableListOf<TickTask>()
        for ((_, t) in tasks) {
            if (!t.handle.isCancelled && tick >= t.targetTick) {
                due.add(t)
            }
        }
        for (t in due) {
            if (t.handle.isCancelled) {
                tasks.remove(t.id)
                continue
            }
            try {
                t.task.run()
            } catch (e: Throwable) {
                logger.error("exception in tick task id={} tick={}", t.id, tick, e)
            }
            if (t.periodTicks > 0 && !t.handle.isCancelled) {
                t.targetTick = tick + t.periodTicks
            } else {
                tasks.remove(t.id)
                t.handle.markDone()
            }
        }
    }

    override fun close() {
        tickFuture?.cancel(false)
        tickFuture = null
        tasks.clear()
        logger.info("TickScheduler closed at tick={}", currentTick.get())
    }

    private class TickTask(
        val id: Long,
        @Volatile var targetTick: Long,
        val periodTicks: Long,
        val task: Runnable,
        val handle: TickHandle
    )

    private class TickHandle(
        override val id: Long
    ) : TaskHandle {
        @Volatile
        private var cancelled = false

        @Volatile
        private var done = false

        private val callbacks = mutableListOf<(Throwable?) -> Unit>()

        override val isCancelled: Boolean get() = cancelled
        override val isDone: Boolean get() = done || cancelled

        override fun cancel(): Boolean {
            if (cancelled || done) return false
            cancelled = true
            return true
        }

        fun markDone() {
            done = true
            val copy: List<(Throwable?) -> Unit>
            synchronized(callbacks) {
                copy = callbacks.toList()
                callbacks.clear()
            }
            for (cb in copy) {
                try {
                    cb(null)
                } catch (_: Throwable) {
                }
            }
        }

        override fun onComplete(callback: (Throwable?) -> Unit) {
            var immediate = false
            synchronized(callbacks) {
                if (done || cancelled) {
                    immediate = true
                } else {
                    callbacks.add(callback)
                }
            }
            if (immediate) callback(null)
        }
    }
}
