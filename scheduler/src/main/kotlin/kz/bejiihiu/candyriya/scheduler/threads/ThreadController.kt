package kz.bejiihiu.candyriya.scheduler.threads

import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kz.bejiihiu.candyriya.config.ProxyConfig
import org.apache.logging.log4j.LogManager

/**
 * Owns thread pools for the proxy.
 *
 * - [scheduledPool] uses platform threads (predictable timing, not virtual).
 * - [asyncPool] uses virtual threads when [ProxyConfig.threads.virtual] is true.
 */
public class ThreadController(
    private val config: ProxyConfig
) : AutoCloseable {
    private val logger = LogManager.getLogger(ThreadController::class.java)

    public val scheduledPool: ScheduledExecutorService = createScheduledPool()

    public val asyncPool: ExecutorService = createAsyncPool()

    public val asyncDispatcher: kotlinx.coroutines.CoroutineDispatcher =
        asyncPool.asCoroutineDispatcher()

    /**
     * Dispatcher backed by virtual threads when enabled, otherwise same as [asyncDispatcher].
     */
    public val virtualDispatcher: kotlinx.coroutines.CoroutineDispatcher = asyncDispatcher

    public val schedulerScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + asyncDispatcher + CoroutineName("Candyriya-scheduler"))

    private fun createScheduledPool(): ScheduledExecutorService {
        val factory = ThreadFactoryBuilder()
            .setNameFormat("Candyriya-sched-%d")
            .setDaemon(false)
            .setUncaughtExceptionHandler { thread, ex ->
                logger.error("uncaught exception in {}", thread.name, ex)
            }
            .build()
        val coreSize = config.threads.scheduledCoreSize
        val executor = ScheduledThreadPoolExecutor(coreSize, factory)
        executor.removeOnCancelPolicy = true
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false)
        logger.info("created scheduledPool coreSize={} (platform threads)", coreSize)
        return executor
    }

    @Suppress("TooGenericExceptionCaught")
    private fun createAsyncPool(): ExecutorService {
        val useVirtual = config.threads.virtual
        if (useVirtual) {
            try {
                val vtFactory = Thread.ofVirtual()
                    .name("Candyriya-async-vt-", 0)
                    .factory()
                // yep hack xd - use newThreadPerTaskExecutor with virtual factory
                val exec = Executors.newThreadPerTaskExecutor(vtFactory)
                logger.info("created asyncPool with virtual threads")
                return exec
            } catch (e: Exception) {
                // fallback if virtual not available, shouldn't happen on java 21
                logger.warn("virtual threads not available, falling back to platform pool", e)
            }
        }
        val parallelism = if (config.threads.asyncParallelism > 0) {
            config.threads.asyncParallelism
        } else {
            Runtime.getRuntime().availableProcessors() * 2
        }
        val factory = ThreadFactoryBuilder()
            .setNameFormat("Candyriya-async-%d")
            .setDaemon(false)
            .setUncaughtExceptionHandler { thread, ex ->
                logger.error("uncaught exception in {}", thread.name, ex)
            }
            .build()
        val exec = ThreadPoolExecutor(
            parallelism,
            parallelism,
            60L,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(),
            factory
        )
        exec.allowCoreThreadTimeOut(false)
        logger.info("created asyncPool parallelism={} (platform threads)", parallelism)
        return exec
    }

    /**
     * Creates netty boss group with single platform thread.
     * Uses platform threads (not virtual) to avoid pinning issues with Netty.
     */
    public fun createBossGroup(): EventLoopGroup {
        val factory = createNettyThreadFactory("Candyriya-netty-boss-%d")
        // yep virtual threads go brr xd but netty needs platform threads here
        return NioEventLoopGroup(1, factory)
    }

    /**
     * Creates netty worker group with [ProxyConfig.network.workers] threads.
     * Uses platform threads (not virtual) to avoid pinning issues with Netty.
     */
    public fun createWorkerGroup(): EventLoopGroup {
        val factory = createNettyThreadFactory("Candyriya-netty-worker-%d")
        val workers = config.network.workers
        return if (workers > 0) NioEventLoopGroup(workers, factory) else NioEventLoopGroup(factory)
    }

    /**
     * Creates a platform [ThreadFactory] with given name format for Netty.
     */
    public fun createNettyThreadFactory(nameFormat: String): ThreadFactory {
        return ThreadFactoryBuilder()
            .setNameFormat(nameFormat)
            .setDaemon(false)
            .setUncaughtExceptionHandler { thread, ex ->
                logger.error("uncaught exception in {}", thread.name, ex)
            }
            .build()
    }

    public fun start() {
        logger.info(
            "ThreadController started virtual={} schedCore={} asyncParallelism={}",
            config.threads.virtual,
            config.threads.scheduledCoreSize,
            config.threads.asyncParallelism
        )
    }

    override fun close() {
        logger.info("ThreadController shutting down")
        schedulerScope.cancel()
        shutdownExecutor(asyncPool, "asyncPool")
        shutdownExecutor(scheduledPool, "scheduledPool")
        logger.info("ThreadController closed")
    }

    private fun shutdownExecutor(executor: ExecutorService, name: String) {
        executor.shutdown()
        try {
            val quiet = config.shutdown.quietPeriodMs
            if (quiet > 0) {
                Thread.sleep(quiet)
            }
            val timeout = config.shutdown.timeoutMs
            if (!executor.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
                logger.warn("{} did not terminate in {}ms, forcing shutdown", name, timeout)
                executor.shutdownNow()
            } else {
                logger.info("{} terminated", name)
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            executor.shutdownNow()
        }
    }
}
