package kz.bejiihiu.candiriya

import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kz.bejiihiu.candiriya.config.ProxyConfig
import kz.bejiihiu.candiriya.lifecycle.LifecycleState
import kz.bejiihiu.candiriya.network.NetworkServer
import kz.bejiihiu.candiriya.scheduler.DefaultScheduler
import kz.bejiihiu.candiriya.scheduler.Scheduler
import kz.bejiihiu.candiriya.scheduler.threads.ThreadController
import kz.bejiihiu.candiriya.scheduler.tick.TickScheduler
import org.apache.logging.log4j.LogManager

/**
 * Main orchestrator that coordinates config and network.
 * State machine is guarded by [AtomicReference].
 */
public class Candiriya(
    private val config: ProxyConfig
) {
    private val logger = LogManager.getLogger(Candiriya::class.java)
    private val state = AtomicReference(LifecycleState.STOPPED)
    private val shutdownLatch = CountDownLatch(1)
    private var networkServer: NetworkServer? = null
    private val threadController: ThreadController = ThreadController(config)
    private val scheduler: Scheduler = DefaultScheduler(threadController) { state.get() }
    private val tickScheduler: TickScheduler =
        TickScheduler(threadController, config.scheduler.tickRateMs)

    public fun getState(): LifecycleState = state.get()

    public fun getScheduler(): Scheduler = scheduler

    public fun getTickScheduler(): TickScheduler = tickScheduler

    public fun getThreadController(): ThreadController = threadController

    public fun start() {
        // only STOPPED -> STARTING is valid
        if (!state.compareAndSet(LifecycleState.STOPPED, LifecycleState.STARTING)) {
            throw IllegalStateException("cannot start from ${state.get()}, expected STOPPED")
        }
        logger.info("Candiriya STARTING -> starting network on {}", config.network.bind)
        threadController.start()
        tickScheduler.start()
        // yep, create server lazily here xd — groups come from ThreadController
        val server = NetworkServer(config, threadController)
        networkServer = server
        try {
            server.start().sync()
        } catch (e: Exception) {
            // failed to bind, rollback to STOPPED
            try {
                tickScheduler.close()
                scheduler.close()
                threadController.close()
            } catch (closeEx: Exception) {
                logger.warn("error during rollback close", closeEx)
            }
            state.set(LifecycleState.STOPPED)
            throw e
        }
        if (!state.compareAndSet(LifecycleState.STARTING, LifecycleState.RUNNING)) {
            // this is cursed, fix later :(
            logger.warn("unexpected state during start: {}", state.get())
        }
        logger.info("Candiriya RUNNING on {}", config.network.bind)
        // show that scheduler is actually used, not just exists xd
        scheduler.execute { logger.info("candiriya ready tick={}", tickScheduler.getCurrentTick()) }
        scheduler.scheduleAtFixedRate(Duration.ofSeconds(5), Duration.ofSeconds(5)) {
            logger.debug("tick={}", tickScheduler.getCurrentTick())
        }
    }

    public fun stop() {
        val current = state.get()
        if (current == LifecycleState.STOPPING || current == LifecycleState.STOPPED) {
            // already stopping, ignore
            return
        }
        // try STARTING->STOPPING or RUNNING->STOPPING
        var transitioned = state.compareAndSet(LifecycleState.RUNNING, LifecycleState.STOPPING)
        if (!transitioned) {
            transitioned = state.compareAndSet(LifecycleState.STARTING, LifecycleState.STOPPING)
        }
        if (!transitioned) {
            logger.warn("stop() called in state {}, ignoring", current)
            return
        }
        logger.info("Candiriya STOPPING -> shutting down network")
        try {
            networkServer?.stop()
        } catch (e: Exception) {
            logger.error("error during network shutdown", e)
        }
        try {
            tickScheduler.close()
        } catch (e: Exception) {
            logger.error("error closing tickScheduler", e)
        }
        try {
            scheduler.close()
        } catch (e: Exception) {
            logger.error("error closing scheduler", e)
        }
        try {
            threadController.close()
        } catch (e: Exception) {
            logger.error("error closing threadController", e)
        }
        state.set(LifecycleState.STOPPED)
        logger.info("Candiriya STOPPED")
        shutdownLatch.countDown()
    }

    public fun addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                // shutdown hook runs in separate thread, just call stop
                stop()
            }
        )
    }

    public fun awaitShutdown() {
        shutdownLatch.await()
    }
}
