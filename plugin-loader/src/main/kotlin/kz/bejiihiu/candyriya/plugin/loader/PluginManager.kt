package kz.bejiihiu.candyriya.plugin.loader

import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.jar.JarFile
import kz.bejiihiu.candyriya.command.CommandManager
import kz.bejiihiu.candyriya.config.ProxyConfig
import kz.bejiihiu.candyriya.permission.PermissionManager
import kz.bejiihiu.candyriya.player.PlayerManager
import kz.bejiihiu.candyriya.plugin.DefaultEventBus
import kz.bejiihiu.candyriya.plugin.DefaultPluginScheduler
import kz.bejiihiu.candyriya.plugin.EventBus
import kz.bejiihiu.candyriya.plugin.Plugin
import kz.bejiihiu.candyriya.plugin.PluginCommand
import kz.bejiihiu.candyriya.plugin.PluginCommandManager
import kz.bejiihiu.candyriya.plugin.PluginCommandSource
import kz.bejiihiu.candyriya.plugin.PluginContext
import kz.bejiihiu.candyriya.plugin.PluginDescription
import kz.bejiihiu.candyriya.plugin.PluginMessaging
import kz.bejiihiu.candyriya.plugin.ProxyPlayer
import kz.bejiihiu.candyriya.plugin.ProxyServer
import kz.bejiihiu.candyriya.scheduler.threads.ThreadController
import net.kyori.adventure.text.Component
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

/**
 * Central loader — scans `plugins` directory for jars, validates `plugin.json`,
 * creates per-plugin classloader + executor, and drives lifecycle `load -> enable -> disable`.
 *
 * Threading: `loadAll` runs on caller thread (STARTING), but each plugin's `onEnable`
 * runs on its own executor with a timeout so a buggy plugin cannot stall the proxy.
 */
public class PluginManager(
    private val pluginsDir: Path,
    private val threadController: ThreadController,
    private val config: ProxyConfig,
    private val playerManager: PlayerManager,
    private val permissionManager: PermissionManager,
    private val commandManager: CommandManager,
    private val eventBus: EventBus = DefaultEventBus()
) {
    private val logger: Logger = LogManager.getLogger(PluginManager::class.java)
    private val containers = ConcurrentHashMap<String, PluginContainer>()

    // lazy for non-isolated group
    private val sharedLoader: PluginClassLoader? = null
    private var sharedLoaderInstance: PluginClassLoader? = null

    /** Global messaging channels seen by all plugins */
    private val globalChannels = ConcurrentHashMap.newKeySet<String>()

    private val proxyServer: ProxyServer = ProxyServerImpl(config, playerManager)

    public fun getEventBus(): EventBus = eventBus
    public fun getProxyServer(): ProxyServer = proxyServer
    public fun getContainers(): Map<String, PluginContainer> = containers.toMap()
    public fun getPlugin(id: String): PluginContainer? = containers[id.lowercase()]
    public fun getPlugins(): Collection<PluginContainer> = containers.values
    public fun count(): Int = containers.size

    /**
     * Scan and load all jars in [pluginsDir]. Does NOT enable yet.
     * Returns successfully loaded containers (failed jars are logged, not thrown).
     */
    public fun loadAll(): List<PluginContainer> {
        if (Files.notExists(pluginsDir)) {
            Files.createDirectories(pluginsDir)
            logger.info("created plugins dir at {}", pluginsDir.toAbsolutePath())
            return emptyList()
        }
        val jars = Files.list(pluginsDir).use { stream ->
            stream.filter { it.fileName.toString().endsWith(".jar", ignoreCase = true) }.toList()
        }
        if (jars.isEmpty()) {
            logger.info("no plugins found in {}", pluginsDir.toAbsolutePath())
            return emptyList()
        }
        logger.info("scanning {} jars in {}", jars.size, pluginsDir.toAbsolutePath())
        val loaded = mutableListOf<PluginContainer>()
        for (jar in jars) {
            try {
                val c = loadOne(jar) ?: continue
                containers[c.description.id.lowercase()] = c
                loaded.add(c)
                logger.info("loaded plugin {} {} from {}", c.description.id, c.description.version, jar.fileName)
            } catch (e: Exception) {
                logger.error("failed to load plugin jar {}", jar.fileName, e)
            }
        }
        // dependency check — mark missing deps as failed (but don't unload yet)
        for (c in loaded.toList()) {
            for (dep in c.description.depends) {
                if (!containers.containsKey(dep.lowercase())) {
                    logger.error("plugin {} requires missing dependency '{}' — disabling", c.description.id, dep)
                    c.markFailed()
                }
            }
        }
        return loaded.filter { it.state != PluginContainer.State.FAILED }
    }

    private fun loadOne(jar: Path): PluginContainer? {
        JarFile(jar.toFile()).use { jf ->
            val entry = jf.getJarEntry("plugin.json") ?: run {
                logger.warn("skipping {} — no plugin.json at jar root", jar.fileName)
                return null
            }
            val bytes = jf.getInputStream(entry).readBytes()
            val desc = try {
                PluginDescription.parse(bytes)
            } catch (e: Exception) {
                logger.error("invalid plugin.json in {}", jar.fileName, e)
                return null
            }
            val idLower = desc.id.lowercase()
            if (containers.containsKey(idLower)) {
                logger.error("duplicate plugin id '{}' in {}", desc.id, jar.fileName)
                return null
            }
            // prepare data dir
            val dataDir = pluginsDir.resolve(desc.id)
            Files.createDirectories(dataDir)

            // classloader — hybrid, isolated or shared
            val jarUrl = jar.toUri().toURL()
            val cl = if (!desc.isolated) {
                getOrCreateSharedLoader(jarUrl)
            } else {
                PluginClassLoader(arrayOf(jarUrl), this::class.java.classLoader)
            }
            // instantiate main class
            val clazz = cl.loadClass(desc.main)
            val instance = try {
                val ctor = clazz.getDeclaredConstructor()
                ctor.isAccessible = true
                ctor.newInstance() as Plugin
            } catch (e: Exception) {
                logger.error("failed to instantiate main class {} for {}", desc.main, desc.id, e)
                try {
                    if (desc.isolated) cl.close()
                } catch (_: Exception) {}
                return null
            }
            // per-plugin executor + scheduler
            val executor = PluginContainer.createExecutor(desc.id, config.threads.virtual)
            val scheduler = DefaultPluginScheduler(desc.id, executor, threadController.scheduledPool)

            val container = PluginContainer(
                description = desc,
                instance = instance,
                classLoader = cl,
                jarPath = jar,
                dataDirectory = dataDir,
                executor = executor,
                scheduler = scheduler
            )
            // call onLoad on plugin thread with timeout
            runOnPluginThread(container, Duration.ofSeconds(10), "onLoad") {
                instance.onLoad()
            }
            if (container.state == PluginContainer.State.FAILED) return null
            return container
        }
    }

    private fun getOrCreateSharedLoader(firstJarUrl: java.net.URL): PluginClassLoader {
        synchronized(this) {
            val existing = sharedLoaderInstance
            if (existing != null) {
                // add new jar to shared loader's classpath via reflection on URLClassLoader.addURL
                try {
                    val m = java.net.URLClassLoader::class.java.getDeclaredMethod("addURL", java.net.URL::class.java)
                    m.isAccessible = true
                    m.invoke(existing, firstJarUrl)
                } catch (_: Exception) {}
                return existing
            }
            val cl = PluginClassLoader(arrayOf(firstJarUrl), this::class.java.classLoader)
            sharedLoaderInstance = cl
            return cl
        }
    }

    /**
     * Enable all loaded plugins. Each `onEnable` runs on its own thread with 10s timeout.
     * Failed plugins are left in FAILED state and skip enable.
     */
    public fun enableAll() {
        if (containers.isEmpty()) return
        logger.info("enabling {} plugins", containers.size)
        for (c in containers.values.sortedBy { it.description.id }) {
            if (c.state == PluginContainer.State.FAILED) {
                logger.warn("skipping enable for failed plugin {}", c.description.id)
                continue
            }
            val ctx = createContext(c)
            runOnPluginThread(c, Duration.ofSeconds(10), "onEnable") {
                c.instance.onEnable(ctx)
            }
            if (c.state != PluginContainer.State.FAILED) {
                c.markEnabled()
                logger.info("enabled plugin {} {}", c.description.id, c.description.version)
            }
        }
        // fire proxy init after all enabled
        try {
            eventBus.fire(kz.bejiihiu.candyriya.plugin.ProxyInitializeEvent())
        } catch (_: Exception) {}
    }

    /**
     * Disable all in reverse order. Cancels tasks, unregisters events/commands.
     */
    public fun disableAll() {
        if (containers.isEmpty()) return
        logger.info("disabling {} plugins", containers.size)
        try {
            eventBus.fire(kz.bejiihiu.candyriya.plugin.ProxyShutdownEvent())
        } catch (_: Exception) {}
        for (c in containers.values.sortedByDescending { it.description.id }) {
            try {
                if (c.state == PluginContainer.State.ENABLED) {
                    runOnPluginThread(c, Duration.ofSeconds(5), "onDisable") {
                        c.instance.onDisable()
                    }
                }
            } catch (e: Exception) {
                logger.error("error disabling {}", c.description.id, e)
            } finally {
                try {
                    eventBus.unregisterAll(c.description.id)
                } catch (_: Exception) {}
                try {
                    unregisterCommands(c.description.id)
                } catch (_: Exception) {}
                try {
                    c.scheduler.cancelAll()
                } catch (_: Exception) {}
                c.markDisabled()
                logger.info("disabled plugin {}", c.description.id)
            }
        }
    }

    // close classloaders + executors after disable
    public fun closeAll() {
        for (c in containers.values) {
            try {
                c.close()
            } catch (_: Exception) {}
        }
        containers.clear()
        try {
            sharedLoaderInstance?.close()
        } catch (_: Exception) {}
        sharedLoaderInstance = null
    }

    // wiring helpers

    private fun createContext(container: PluginContainer): PluginContext {
        val id = container.description.id
        return object : PluginContext {
            override val description = container.description
            override val logger: Logger = container.logger
            override val dataDirectory: Path = container.dataDirectory
            override val server: ProxyServer = proxyServer
            override val events: EventBus = eventBus
            override val scheduler = container.scheduler
            override val commands: PluginCommandManager = ContextCommandManager(id)
            override val permissions: kz.bejiihiu.candyriya.plugin.PermissionRegistry = ContextPermissions()
            override val messaging: PluginMessaging = ContextMessaging(id)
        }
    }

    private fun runOnPluginThread(container: PluginContainer, timeout: Duration, phase: String, block: () -> Unit) {
        val latch = CountDownLatch(1)
        var error: Throwable? = null
        container.executor.submit {
            try {
                block()
            } catch (e: Throwable) {
                error = e
            } finally {
                latch.countDown()
            }
        }
        val ok = try {
            latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
        if (!ok) {
            logger.error("plugin {} {} timed out ({}s)", container.description.id, phase, timeout.toSeconds())
            container.markFailed()
        } else if (error != null) {
            logger.error("plugin {} failed in {}", container.description.id, phase, error)
            container.markFailed()
        }
    }

    private fun unregisterCommands(pluginId: String) {
        // remove aliases owned by this plugin — we track via ContextCommandManager's set
        val owned = ownedCommands[pluginId.lowercase()] ?: return
        for (alias in owned.toList()) {
            try {
                commandManager.unregister(alias)
            } catch (_: Exception) {}
        }
        ownedCommands.remove(pluginId.lowercase())
    }

    private val ownedCommands = ConcurrentHashMap<String, MutableSet<String>>()

    private inner class ContextCommandManager(private val pluginId: String) : PluginCommandManager {
        override fun register(alias: String, command: PluginCommand, vararg extraAliases: String) {
            val wrapped = object : kz.bejiihiu.candyriya.command.Command {
                override val permission: String? = command.permission
                override val description: String = command.description
                override val usage: String = command.usage
                override fun execute(source: kz.bejiihiu.candyriya.command.CommandSource, args: Array<String>) {
                    val ps = wrapSource(source)
                    // dispatch on plugin thread to keep threading contract
                    val container = containers[pluginId.lowercase()]
                    if (container != null && container.state == PluginContainer.State.ENABLED) {
                        container.executor.submit {
                            try {
                                command.execute(ps, args)
                            } catch (e: Exception) {
                                container.logger.error("command /{} failed", alias, e)
                            }
                        }
                    } else {
                        try {
                            command.execute(ps, args)
                        } catch (_: Exception) {}
                    }
                }
                override fun suggest(source: kz.bejiihiu.candyriya.command.CommandSource, args: Array<String>): List<String> {
                    return try {
                        command.suggest(wrapSource(source), args)
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
            }
            commandManager.register(alias, wrapped, *extraAliases)
            ownedCommands.computeIfAbsent(pluginId.lowercase()) { ConcurrentHashMap.newKeySet() }.add(alias.lowercase())
            for (a in extraAliases) ownedCommands[pluginId.lowercase()]!!.add(a.lowercase())
        }

        override fun unregister(alias: String): Boolean {
            val ok = commandManager.unregister(alias)
            ownedCommands[pluginId.lowercase()]?.remove(alias.lowercase())
            return ok
        }

        override fun ownedAliases(): Set<String> = ownedCommands[pluginId.lowercase()]?.toSet() ?: emptySet()

        private fun wrapSource(source: kz.bejiihiu.candyriya.command.CommandSource): PluginCommandSource = object : PluginCommandSource {
            override val name: String = source.name
            override val isConsole: Boolean = source.isConsole
            override fun hasPermission(permission: String): Boolean = source.hasPermission(permission)
            override fun sendMessage(component: Component) = source.sendMessage(component)
            override fun asPlayer(): ProxyPlayer? {
                // if source is PlayerSource, try to resolve ProxyPlayer
                return try {
                    val m = source.javaClass.getMethod("uuid")
                    val uuid = m.invoke(source) as java.util.UUID
                    proxyServer.getPlayer(uuid).orElse(null)
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    private inner class ContextPermissions : kz.bejiihiu.candyriya.plugin.PermissionRegistry {
        override fun has(player: ProxyPlayer, permission: String): Boolean {
            val subject = kz.bejiihiu.candyriya.permission.PlayerSubject(player.uuid, player.username, permissionManager)
            return permissionManager.permissionValue(subject, permission) == kz.bejiihiu.candyriya.permission.Tristate.TRUE
        }
        override fun has(source: PluginCommandSource, permission: String): Boolean =
            if (source.isConsole) true else source.hasPermission(permission)
    }

    private inner class ContextMessaging(private val pluginId: String) : PluginMessaging {
        override fun registerChannel(channel: String): Boolean {
            require(channel.matches(Regex("^[a-z0-9_-]+:[a-z0-9/_-]+$"))) { "channel must be 'namespace:name', got '$channel'" }
            return globalChannels.add(channel.lowercase())
        }
        override fun unregisterChannel(channel: String): Boolean = globalChannels.remove(channel.lowercase())
        override fun send(player: ProxyPlayer, channel: String, data: ByteArray): Boolean {
            if (!globalChannels.contains(channel.lowercase())) return false
            // real send will go through player's connection — stub logs for now
            LogManager.getLogger(
                "candiriya-messaging"
            ).debug("plugin {} -> {} channel {} {} bytes", pluginId, player.username, channel, data.size)
            return true
        }
        override fun channels(): Set<String> = globalChannels.toSet()
    }
}

