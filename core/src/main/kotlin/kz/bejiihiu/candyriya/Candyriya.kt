package kz.bejiihiu.candyriya

import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kz.bejiihiu.candyriya.command.CommandManager
import kz.bejiihiu.candyriya.command.builtin.CandyriyaCommand
import kz.bejiihiu.candyriya.command.builtin.GlistCommand
import kz.bejiihiu.candyriya.command.builtin.SendCommand
import kz.bejiihiu.candyriya.command.builtin.ServerCommand
import kz.bejiihiu.candyriya.command.builtin.ShutdownCommand
import kz.bejiihiu.candyriya.config.ProxyConfig
import kz.bejiihiu.candyriya.lifecycle.LifecycleState
import kz.bejiihiu.candyriya.network.NetworkServer
import kz.bejiihiu.candyriya.permission.PermissionManager
import kz.bejiihiu.candyriya.permission.PermissionsFile
import kz.bejiihiu.candyriya.player.PlayerManager
import kz.bejiihiu.candyriya.scheduler.DefaultScheduler
import kz.bejiihiu.candyriya.scheduler.Scheduler
import kz.bejiihiu.candyriya.scheduler.context.ContextRegistry
import kz.bejiihiu.candyriya.scheduler.threads.ThreadController
import kz.bejiihiu.candyriya.scheduler.tick.TickScheduler
import org.apache.logging.log4j.LogManager

/**
 * Main orchestrator that coordinates config and network.
 * State machine is guarded by [AtomicReference].
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = ["EI_EXPOSE_REP"], justification = "exposed for tests xd")
public class Candyriya(
    private val config: ProxyConfig,
    private val permissionsFile: Path = Paths.get("permissions.toml")
) {
    private val logger = LogManager.getLogger(Candyriya::class.java)
    private val state = AtomicReference(LifecycleState.STOPPED)
    private val shutdownLatch = CountDownLatch(1)
    private var networkServer: NetworkServer? = null
    private val threadController: ThreadController = ThreadController(config)
    private val scheduler: Scheduler = DefaultScheduler(threadController) { state.get() }
    private val tickScheduler: TickScheduler =
        TickScheduler(threadController, config.scheduler.tickRateMs)
    private val contextRegistry: ContextRegistry = ContextRegistry(config, threadController)
    private val playerManager: PlayerManager = PlayerManager(contextRegistry)
    private val permissionManager: PermissionManager = PermissionManager(permissionsFile)
    private val commandManager: CommandManager = CommandManager()

    public fun getState(): LifecycleState = state.get()

    public fun getScheduler(): Scheduler = scheduler

    public fun getTickScheduler(): TickScheduler = tickScheduler

    public fun getThreadController(): ThreadController = threadController

    public fun getContextRegistry(): ContextRegistry = contextRegistry

    public fun getPlayerManager(): PlayerManager = playerManager

    public fun getPermissionManager(): PermissionManager = permissionManager

    public fun getCommandManager(): CommandManager = commandManager

    public fun getPermissionsFile(): Path = permissionsFile

    public fun start() {
        // only STOPPED -> STARTING is valid
        if (!state.compareAndSet(LifecycleState.STOPPED, LifecycleState.STARTING)) {
            throw IllegalStateException("cannot start from ${state.get()}, expected STOPPED")
        }
        logger.info("Candyriya STARTING -> starting network on {}", config.network.bind)
        // init permissions — file first, then defaults
        try {
            PermissionsFile.ensureExists(permissionsFile)
            permissionManager.loadFromFile(permissionsFile)
        } catch (e: Exception) {
            logger.warn("failed to init permissions", e)
        }
        // register builtin commands — mirrors Velocity's built-ins but with candyriya name
        try {
            val candyriyaCmd = CandyriyaCommand(commandManager, permissionManager, permissionsFile)
            commandManager.register("candyriya", candyriyaCmd, "candiriya", "velocity")
            commandManager.register("server", ServerCommand(playerManager, config))
            commandManager.register("glist", GlistCommand(playerManager))
            commandManager.register("send", SendCommand(playerManager, config))
            commandManager.register("shutdown", ShutdownCommand { stop() })
            logger.info("registered builtin commands: candyriya, server, glist, send, shutdown")
        } catch (e: Exception) {
            logger.warn("failed to register builtin commands", e)
        }
        threadController.start()
        tickScheduler.start()
        // yep, create server lazily here xd — groups come from ThreadController
        val server = NetworkServer(
            config,
            threadController,
            scheduler = scheduler,
            tickScheduler = tickScheduler,
            contextRegistry = contextRegistry,
            playerManager = playerManager
        )
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
        logger.info("Candyriya RUNNING on {}", config.network.bind)
        logger.info("contexts={} players={}", contextRegistry.size(), playerManager.count())
        // show that scheduler is actually used, not just exists xd
        scheduler.execute { logger.info("Candyriya ready tick={} contexts={}", tickScheduler.getCurrentTick(), contextRegistry.size()) }
        scheduler.scheduleAtFixedRate(Duration.ofSeconds(5), Duration.ofSeconds(5)) {
            logger.debug("tick={} players={} ctxStats={}", tickScheduler.getCurrentTick(), playerManager.count(), contextRegistry.stats())
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
        logger.info("Candyriya STOPPING -> shutting down network")
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
            contextRegistry.close()
        } catch (e: Exception) {
            logger.error("error closing contextRegistry", e)
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
        logger.info("Candyriya STOPPED")
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
