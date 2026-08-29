package kz.bejiihiu.candiriya.plugin

import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Scheduler that plugins see. Every task is tagged with `pluginId`
 * so `PluginManager` can `cancelAll(pluginId)` on disable.
 *
 * Implementations delegate to core `ThreadController` pools but expose
 * a stable API for plugins (so Velocity bridge can wrap it as
 * `com.velocitypowered.api.scheduler.Scheduler` later).
 */
public interface PluginScheduler {
    public val pluginId: String

    /** Run now on plugin's dedicated thread (virtual if enabled). */
    public fun execute(task: Runnable): PluginTask

    /** Run after delay */
    public fun delayed(delay: Duration, task: Runnable): PluginTask

    /** Fixed-rate, initialDelay + period. Runs on scheduled pool, task hops to plugin thread. */
    public fun repeating(initialDelay: Duration, period: Duration, task: Runnable): PluginTask

    /** Async via virtual threads, returns future */
    public fun <T> async(block: () -> T): CompletableFuture<T>

    /** Cancel all tasks of this plugin */
    public fun cancelAll(): Int
}

/** Handle that plugins hold. */
public interface PluginTask {
    public fun cancel(): Boolean
    public val isCancelled: Boolean
    public val isDone: Boolean
}

/**
 * Totally standalone impl that doesn't depend on core `Scheduler` class
 * (so `plugin-api` stays free of `:scheduler` dep).
 * `plugin-loader` will wire it to `ThreadController`.
 */
public class DefaultPluginScheduler(
    override val pluginId: String,
    private val asyncExecutor: java.util.concurrent.ExecutorService,
    private val scheduledExecutor: java.util.concurrent.ScheduledExecutorService
) : PluginScheduler {
    private val tasks = ConcurrentHashMap<PluginTaskImpl, Boolean>()

    override fun execute(task: Runnable): PluginTask {
        val t = PluginTaskImpl()
        val wrapped = Runnable {
            try {
                task.run()
            } finally {
                t.markDone()
                tasks.remove(t)
            }
        }
        val f = asyncExecutor.submit(wrapped)
        t.future = f
        tasks[t] = true
        return t
    }

    override fun delayed(delay: Duration, task: Runnable): PluginTask {
        val t = PluginTaskImpl()
        val wrapped = Runnable {
            // hop to plugin thread — don't block scheduled pool xd
            execute {
                try {
                    task.run()
                } finally {
                    t.markDone()
                }
            }
            tasks.remove(t)
        }
        val f = scheduledExecutor.schedule(wrapped, delay.toMillis(), TimeUnit.MILLISECONDS)
        t.scheduled = f
        tasks[t] = true
        return t
    }

    override fun repeating(initialDelay: Duration, period: Duration, task: Runnable): PluginTask {
        require(!period.isZero && !period.isNegative) { "period must be >0" }
        val t = PluginTaskImpl()
        val wrapped = Runnable {
            // each tick hops to plugin thread
            asyncExecutor.submit {
                if (!t.isCancelled) {
                    try {
                        task.run()
                    } catch (_: Exception) {
                    }
                }
            }
        }
        val f = scheduledExecutor.scheduleAtFixedRate(
            wrapped,
            initialDelay.toMillis(),
            period.toMillis(),
            TimeUnit.MILLISECONDS
        )
        t.scheduled = f
        tasks[t] = true
        return t
    }

    override fun <T> async(block: () -> T): CompletableFuture<T> = CompletableFuture.supplyAsync(block, asyncExecutor)

    override fun cancelAll(): Int {
        var c = 0
        for (t in tasks.keys.toList()) {
            if (t.cancel()) c++
        }
        tasks.clear()
        return c
    }

    private class PluginTaskImpl : PluginTask {
        @Volatile var future: java.util.concurrent.Future<*>? = null

        @Volatile var scheduled: ScheduledFuture<*>? = null

        @Volatile private var cancelled = false

        @Volatile private var done = false

        override val isCancelled: Boolean get() = cancelled
        override val isDone: Boolean get() = done || future?.isDone == true || scheduled?.isDone == true

        fun markDone() {
            done = true
        }

        override fun cancel(): Boolean {
            if (cancelled || isDone) return false
            cancelled = true
            future?.cancel(false)
            scheduled?.cancel(false)
            return true
        }
    }
}
