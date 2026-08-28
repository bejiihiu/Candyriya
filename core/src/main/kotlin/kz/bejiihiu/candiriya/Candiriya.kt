package kz.bejiihiu.candiriya

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kz.bejiihiu.candiriya.config.ProxyConfig
import kz.bejiihiu.candiriya.lifecycle.LifecycleState
import kz.bejiihiu.candiriya.network.NetworkServer
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

    public fun getState(): LifecycleState = state.get()

    public fun start() {
        // only STOPPED -> STARTING is valid
        if (!state.compareAndSet(LifecycleState.STOPPED, LifecycleState.STARTING)) {
            throw IllegalStateException("cannot start from ${state.get()}, expected STOPPED")
        }
        logger.info("Candiriya STARTING -> starting network on {}", config.network.bind)
        // yep, create server lazily here xd
        val server = NetworkServer(config)
        networkServer = server
        try {
            server.start().sync()
        } catch (e: Exception) {
            // failed to bind, rollback to STOPPED
            state.set(LifecycleState.STOPPED)
            throw e
        }
        if (!state.compareAndSet(LifecycleState.STARTING, LifecycleState.RUNNING)) {
            // this is cursed, fix later :(
            logger.warn("unexpected state during start: {}", state.get())
        }
        logger.info("Candiriya RUNNING on {}", config.network.bind)
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
