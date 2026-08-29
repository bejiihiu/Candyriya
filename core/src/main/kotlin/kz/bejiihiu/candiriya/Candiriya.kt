package kz.bejiihiu.candiriya

import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kz.bejiihiu.candiriya.command.CommandManager
import kz.bejiihiu.candiriya.command.builtin.CandiriyaCommand
import kz.bejiihiu.candiriya.command.builtin.GlistCommand
import kz.bejiihiu.candiriya.command.builtin.SendCommand
import kz.bejiihiu.candiriya.command.builtin.ServerCommand
import kz.bejiihiu.candiriya.command.builtin.ShutdownCommand
import kz.bejiihiu.candiriya.config.ConfigLoader
import kz.bejiihiu.candiriya.config.ProxyConfig
import kz.bejiihiu.candiriya.lifecycle.LifecycleState
import kz.bejiihiu.candiriya.network.NetworkServer
import kz.bejiihiu.candiriya.permission.PermissionManager
import kz.bejiihiu.candiriya.permission.PermissionsFile
import kz.bejiihiu.candiriya.player.PlayerManager
import kz.bejiihiu.candiriya.scheduler.DefaultScheduler
import kz.bejiihiu.candiriya.scheduler.Scheduler
import kz.bejiihiu.candiriya.scheduler.context.ContextRegistry
import kz.bejiihiu.candiriya.scheduler.threads.ThreadController
import kz.bejiihiu.candiriya.scheduler.tick.TickScheduler
import kz.bejiihiu.candiriya.server.ServerRegistry
import org.apache.logging.log4j.LogManager

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = ["EI_EXPOSE_REP"], justification = "exposed for tests xd")
public class Candiriya(
    private var config: ProxyConfig,
    private val permissionsFile: Path = Paths.get("permissions.toml"),
    private val configPath: Path = Paths.get("candiriya.toml")
) {
    private val logger = LogManager.getLogger(Candiriya::class.java)
    private val state = AtomicReference(LifecycleState.STOPPED)
    private val shutdownLatch = CountDownLatch(1)
    private var networkServer: NetworkServer? = null
    private val threadController: ThreadController = ThreadController(config)
    private val scheduler: Scheduler = DefaultScheduler(threadController) { state.get() }
    private val tickScheduler: TickScheduler = TickScheduler(threadController, config.scheduler.tickRateMs)
    private val contextRegistry: ContextRegistry = ContextRegistry(config, threadController)
    private val serverRegistry: ServerRegistry = ServerRegistry(
        servers = config.servers.servers,
        tryOrder = config.servers.tryOrder,
        unavailableCooldownMs = config.servers.unavailableCooldownMs
    )
    private val playerManager: PlayerManager = PlayerManager(contextRegistry, serverRegistry)
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
    public fun getServerRegistry(): ServerRegistry = serverRegistry
    public fun getConfig(): ProxyConfig = config
    public fun getConfigPath(): Path = configPath

    /** Full reload: candiriya.toml + permissions.toml + server registry. */
    public fun reload(): Result<String> {
        return try {
            val newConfig = ConfigLoader.load(configPath)
            config = newConfig
            serverRegistry.update(newConfig.servers.servers, newConfig.servers.tryOrder)
            playerManager.setServerRegistry(serverRegistry)
            try {
                PermissionsFile.ensureExists(permissionsFile)
                permissionManager.loadFromFile(permissionsFile)
            } catch (e: Exception) {
                logger.warn("failed to reload permissions", e)
            }
            logger.info("reload complete: servers={} try={}", serverRegistry.names(), newConfig.servers.tryOrder)
            Result.success("Reloaded: ${serverRegistry.count()} servers, try=${newConfig.servers.tryOrder}")
        } catch (e: Exception) {
            logger.warn("reload failed", e)
            Result.failure(e)
        }
    }

    public fun start() {
        if (!state.compareAndSet(LifecycleState.STOPPED, LifecycleState.STARTING)) {
            throw IllegalStateException("cannot start from ${state.get()}, expected STOPPED")
        }
        logger.info("Candiriya STARTING -> starting network on {}", config.network.bind)
        try {
            PermissionsFile.ensureExists(permissionsFile)
            permissionManager.loadFromFile(permissionsFile)
        } catch (e: Exception) {
            logger.warn("failed to init permissions", e)
        }
        try {
            val candiriyaCmd = CandiriyaCommand(commandManager, permissionManager, permissionsFile, configPath, this)
            commandManager.register("candiriya", candiriyaCmd, "candyriya", "candirya")
            commandManager.register("server", ServerCommand(playerManager, serverRegistry))
            commandManager.register("glist", GlistCommand(playerManager))
            commandManager.register("send", SendCommand(playerManager, serverRegistry))
            commandManager.register("shutdown", ShutdownCommand { stop() })
            logger.info("registered builtin commands: candyriya, server, glist, send, shutdown")
        } catch (e: Exception) {
            logger.warn("failed to register builtin commands", e)
        }
        threadController.start()
        tickScheduler.start()
        val server = NetworkServer(
            config,
            threadController,
            scheduler = scheduler,
            tickScheduler = tickScheduler,
            contextRegistry = contextRegistry,
            playerManager = playerManager,
            serverRegistry = serverRegistry
        )
        networkServer = server
        try {
            server.start().sync()
        } catch (e: Exception) {
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
            logger.warn("unexpected state during start: {}", state.get())
        }
        logger.info("Candiriya RUNNING on {} servers={} try={}", config.network.bind, serverRegistry.names(), config.servers.tryOrder)
        logger.info("contexts={} players={}", contextRegistry.size(), playerManager.count())
        scheduler.execute { logger.info("candiriya ready tick={} contexts={}", tickScheduler.getCurrentTick(), contextRegistry.size()) }
        scheduler.scheduleAtFixedRate(Duration.ofSeconds(5), Duration.ofSeconds(5)) {
            logger.debug(
                "tick={} players={} ctxStats={} servers={}",
                tickScheduler.getCurrentTick(),
                playerManager.count(),
                contextRegistry.stats(),
                serverRegistry.count()
            )
        }
    }

    public fun stop() {
        val current = state.get()
        if (current == LifecycleState.STOPPING || current == LifecycleState.STOPPED) return
        var transitioned = state.compareAndSet(LifecycleState.RUNNING, LifecycleState.STOPPING)
        if (!transitioned) transitioned = state.compareAndSet(LifecycleState.STARTING, LifecycleState.STOPPING)
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
        logger.info("Candiriya STOPPED")
        shutdownLatch.countDown()
    }

    public fun addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread { stop() })
    }

    public fun awaitShutdown() {
        shutdownLatch.await()
    }
}
