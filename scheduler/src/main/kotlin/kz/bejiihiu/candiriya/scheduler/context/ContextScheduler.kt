@file:Suppress("ktlint")

package kz.bejiihiu.candiriya.scheduler.context

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import java.time.Duration
import java.util.UUID
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kz.bejiihiu.candiriya.scheduler.Scheduler
import kz.bejiihiu.candiriya.scheduler.TaskHandle
import kz.bejiihiu.candiriya.scheduler.threads.ThreadController

@SuppressFBWarnings(value = ["UWF_UNWRITTEN_FIELD", "SE_BAD_FIELD"], justification = "kotlin object fields xd")
private object NoopHandle : TaskHandle {
    override val id = -1L
    override val isCancelled = false
    override val isDone = true
    override fun cancel() = false
    override fun onComplete(callback: (Throwable?) -> Unit) {
        callback(null)
    }
}

@SuppressFBWarnings(value = ["UWF_UNWRITTEN_FIELD", "SE_BAD_FIELD"], justification = "wrapper xd")
private class FutureHandle(private val f: java.util.concurrent.Future<*>) : TaskHandle {
    override val id = -1L
    override val isCancelled get() = f.isCancelled
    override val isDone get() = f.isDone
    override fun cancel() = f.cancel(false)
    override fun onComplete(callback: (Throwable?) -> Unit) {}
}

/**
 * Scheduler that hops to player's [ExecutionContext].
 * Like Folia's EntityScheduler / RegionScheduler.
 */
@SuppressFBWarnings(
    value = ["EI_EXPOSE_REP2", "SE_BAD_FIELD", "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE"],
    justification = "scheduler wrappers, false positive xd"
)
public class ContextScheduler(
    private val registry: ContextRegistry,
    private val uuid: UUID,
    private val threadController: ThreadController
) : Scheduler {

    override fun execute(task: Runnable): TaskHandle {
        val c = registry.getFor(uuid)
        return if (c != null) {
            c.execute(task)
            NoopHandle
        } else {
            val f = threadController.asyncPool.submit(task)
            FutureHandle(f)
        }
    }

    override fun execute(delay: Duration, task: Runnable): TaskHandle {
        val c = registry.getFor(uuid)
        return if (c != null) {
            c.execute(delay, task)
        } else {
            val f = threadController.scheduledPool.schedule(
                { threadController.asyncPool.execute(task) },
                delay.toMillis(),
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            FutureHandle(f)
        }
    }

    override fun scheduleAtFixedRate(initialDelay: Duration, period: Duration, task: Runnable): TaskHandle {
        val c = registry.getFor(uuid)
        return c?.scheduleAtFixedRate(initialDelay, period, task)
            ?: threadController.scheduledPool.scheduleAtFixedRate(task, initialDelay.toMillis(), period.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS).let {
                FutureHandle(it)
            }
    }

    override fun scheduleWithFixedDelay(initialDelay: Duration, delay: Duration, task: Runnable): TaskHandle {
        val c = registry.getFor(uuid)
        if (c != null) {
            val future = threadController.scheduledPool.scheduleWithFixedDelay(
                { c.execute(task) },
                initialDelay.toMillis(),
                delay.toMillis(),
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            return FutureHandle(future)
        }
        val f = threadController.scheduledPool.scheduleWithFixedDelay(
            task,
            initialDelay.toMillis(),
            delay.toMillis(),
            java.util.concurrent.TimeUnit.MILLISECONDS
        )
        return FutureHandle(f)
    }

    override fun launch(block: suspend () -> Unit): TaskHandle {
        val c = registry.getFor(uuid)
        return if (c != null) {
            var cancelled = false
            c.execute(
                Runnable {
                    if (cancelled) return@Runnable
                    threadController.schedulerScope.launch { block() }
                }
            )
            object : TaskHandle {
                override val id = -1L
                override val isCancelled get() = cancelled
                override val isDone get() = false
                override fun cancel() = run {
                    cancelled = true
                    true
                }
                override fun onComplete(cb: (Throwable?) -> Unit) {}
            }
        } else {
            val job = threadController.schedulerScope.launch { block() }
            object : TaskHandle {
                override val id = -1L
                override val isCancelled get() = !job.isActive
                override val isDone get() = job.isCompleted
                override fun cancel() = run {
                    job.cancel()
                    true
                }
                override fun onComplete(cb: (Throwable?) -> Unit) {
                    job.invokeOnCompletion { cb(it) }
                }
            }
        }
    }

    override suspend fun <T> async(block: suspend () -> T): Deferred<T> = threadController.schedulerScope.async { block() }

    override fun cancelAll(): Int = 0
    override fun close() {}
}

/**
 * Global context-aware scheduler that picks ctx by uuid.
 */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP2", "SE_BAD_FIELD"], justification = "wrapper xd")
public class GlobalContextScheduler(
    private val registry: ContextRegistry,
    private val threadController: ThreadController
) {
    public fun forPlayer(uuid: UUID): Scheduler = ContextScheduler(registry, uuid, threadController)

    public fun forContext(id: Int): Scheduler {
        val ctx = registry.get(id)
        return object : Scheduler {
            override fun execute(task: Runnable): TaskHandle {
                ctx.execute(task)
                return NoopHandle
            }
            override fun execute(delay: Duration, task: Runnable): TaskHandle = ctx.execute(delay, task)
            override fun scheduleAtFixedRate(i: Duration, p: Duration, t: Runnable): TaskHandle = ctx.scheduleAtFixedRate(i, p, t)
            override fun scheduleWithFixedDelay(i: Duration, d: Duration, t: Runnable): TaskHandle {
                val f = threadController.scheduledPool.scheduleWithFixedDelay({
                    ctx.execute(t)
                }, i.toMillis(), d.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
                return FutureHandle(f)
            }
            override fun launch(block: suspend () -> Unit): TaskHandle {
                ctx.execute(Runnable { threadController.schedulerScope.launch { block() } })
                return NoopHandle
            }
            override suspend fun <T> async(block: suspend () -> T): Deferred<T> = threadController.schedulerScope.async { block() }
            override fun cancelAll() = 0
            override fun close() {}
        }
    }
}
