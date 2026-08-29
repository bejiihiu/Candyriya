package kz.bejiihiu.candyriya.config

import com.electronwill.nightconfig.core.file.CommentedFileConfig
import com.electronwill.nightconfig.core.io.ParsingMode
import java.nio.file.Files
import java.nio.file.Path
import kz.bejiihiu.candyriya.server.RegisteredServer
import org.apache.logging.log4j.LogManager

/**
 * Loads [ProxyConfig] from TOML. If file doesn't exist, creates default from resource.
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = ["REC_CATCH_EXCEPTION"], justification = "parsing catches xd")
public object ConfigLoader {
    private val logger = LogManager.getLogger(ConfigLoader::class.java)

    public fun load(path: Path): ProxyConfig {
        if (Files.notExists(path)) {
            logger.info("config not found at {}, creating default", path)
            createDefault(path)
        }
        val fileConfig = CommentedFileConfig.builder(path).parsingMode(ParsingMode.ADD).build()
        fileConfig.load()
        return parse(fileConfig)
    }

    public fun loadAsync(path: Path, executor: (Runnable) -> Unit, callback: (ProxyConfig) -> Unit) {
        executor { callback(load(path)) }
    }

    private fun createDefault(path: Path) {
        // try to copy from classpath resource, fallback to inline default
        val resource = ConfigLoader::class.java.getResourceAsStream("/candyriya.default.toml")
        if (resource != null) {
            resource.use { input ->
                Files.createDirectories(path.parent ?: Path.of("."))
                Files.copy(input, path)
            }
            logger.info("copied default config from resources to {}", path)
        } else {
            Files.createDirectories(path.parent ?: Path.of("."))
            Files.writeString(path, defaultToml())
            logger.info("wrote inline default config to {}", path)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parse(config: CommentedFileConfig): ProxyConfig {
        val bind = config.getOrElse<String>("network.bind", "0.0.0.0:25577")
        val workers = config.getOrElse<Number>("network.workers", 0).toInt()
        val readTimeoutSeconds = config.getOrElse<Number>("network.readTimeoutSeconds", 30).toInt()
        val maxPacketSize = config.getOrElse<Number>("protocol.maxPacketSize", 2097152).toInt()
        val compressionThreshold = config.getOrElse<Number>("protocol.compressionThreshold", 256).toInt()

        // --- servers: [servers] table with string addresses + try list ---
        val serversRaw = mutableMapOf<String, String>()
        // collect all entries under "servers" except known non-server keys
        val excludedKeys =
            setOf("try", "connectTimeoutMs", "retryAttempts", "retryDelayMs", "failoverOnUnexpectedDisconnect", "unavailableCooldownMs")
        try {
            // night-config: check if entry is map-like
            if (config.contains("servers")) {
                val serversTable = config.get<List<String>>("servers.try")
                // we have at least the try key, so servers table exists
            }
        } catch (_: Exception) {}
        // iterate raw config entries: use valueMap view
        try {
            val allKeys: Set<String> = config.valueMap().keys
            // look for keys starting with "servers."
            for (k in allKeys) {
                if (k.startsWith("servers.") && !excludedKeys.contains(k.substringAfter("servers."))) {
                    val name = k.substringAfter("servers.")
                    // skip if contains dot (nested) — only top-level servers.<name>
                    if ("." in name) continue
                    val addr = config.get<String>(k)
                    serversRaw[name] = addr
                }
            }
        } catch (_: Exception) {
            // fallback: try entrySet iteration
        }
        // also try direct config.get for known pattern if raw scan failed (night-config quirks)
        if (serversRaw.isEmpty()) {
            // try to get as map
            try {
                val rawMap = config.get<Map<String, Any>>("servers")
                if (rawMap != null) {
                    for ((k, v) in rawMap) {
                        if (k in excludedKeys) continue
                        if (v is String) serversRaw[k] = v
                    }
                }
            } catch (_: Exception) {}
        }

        val connectTimeoutMs = config.getOrElse<Number>("servers.connectTimeoutMs", 5000).toInt()
        val retryAttempts = config.getOrElse<Number>("servers.retryAttempts", 0).toInt()
        val retryDelayMs = config.getOrElse<Number>("servers.retryDelayMs", 500).toLong()
        val failover = config.getOrElse<Boolean>("servers.failoverOnUnexpectedDisconnect", true)
        val cooldownMs = config.getOrElse<Number>("servers.unavailableCooldownMs", 5000).toLong()
        val tryRaw: List<String> = try {
            config.get<List<String>>("servers.try") ?: emptyList()
        } catch (_: Exception) {
            try {
                val raw = config.get<String>("servers.try")
                if (raw != null) listOf(raw) else emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        }

        val serversMap: MutableMap<String, RegisteredServer> = mutableMapOf()
        for ((name, addr) in serversRaw) {
            require(name.matches(Regex("^[a-zA-Z0-9_-]{1,16}$"))) { "invalid server name '$name'" }
            val host = addr.substringBefore(":")
            val portStr = addr.substringAfterLast(":", "")
            require(host.isNotEmpty() && portStr.isNotEmpty()) { "invalid server address '$addr' for '$name', expected host:port" }
            val port = portStr.toIntOrNull() ?: throw IllegalArgumentException("invalid port '$portStr' for server '$name'")
            require(port in 1..65535) { "port out of range for server '$name': $port" }
            serversMap[name] = RegisteredServer(name, host, port)
        }
        // defaults if empty
        if (serversMap.isEmpty()) {
            serversMap.putAll(
                mapOf(
                    "lobby" to RegisteredServer("lobby", "127.0.0.1", 30066),
                    "factions" to RegisteredServer("factions", "127.0.0.1", 30067),
                    "minigames" to RegisteredServer("minigames", "127.0.0.1", 30068)
                )
            )
        }
        val tryOrder: List<String> = if (tryRaw.isEmpty()) listOf(serversMap.keys.first()) else tryRaw
        require(tryOrder.isNotEmpty()) { "servers.try must not be empty" }
        for (n in tryOrder) {
            require(serversMap.containsKey(n) || serversMap.containsKey(n.lowercase())) {
                "servers.try entry '$n' not found in [servers] map (available: ${serversMap.keys})"
            }
        }

        val onlineMode = config.getOrElse<Boolean>("security.onlineMode", false)
        val forwardingSecret = config.getOrElse<String>("security.forwardingSecret", "")
        val forwardingModeRaw = config.getOrElse<String>("security.forwardingMode", "NONE")
        val forwardingMode = try {
            ForwardingMode.valueOf(forwardingModeRaw.uppercase())
        } catch (_: IllegalArgumentException) {
            ForwardingMode.NONE
        }
        val motd = config.getOrElse<String>("status.motd", StatusConfig.DEFAULT_MOTD)
        val maxPlayers = config.getOrElse<Number>("status.maxPlayers", 100).toInt()
        val versionName = config.getOrElse<String>("status.versionName", "26.1")
        val versionProtocol = config.getOrElse<Number>("status.versionProtocol", 775).toInt()
        val quietPeriodMs = config.getOrElse<Number>("shutdown.quietPeriodMs", 200).toLong()
        val timeoutMs = config.getOrElse<Number>("shutdown.timeoutMs", 5000).toLong()
        val level = config.getOrElse<String>("logging.level", "INFO")
        val virtual = config.getOrElse<Boolean>("threads.virtual", true)
        val scheduledCoreSize = config.getOrElse<Number>("threads.scheduledCoreSize", 2).toInt()
        val asyncParallelism = config.getOrElse<Number>("threads.asyncParallelism", 0).toInt()
        val tickRateMs = config.getOrElse<Number>("scheduler.tickRateMs", 50).toLong()
        val contexts = config.getOrElse<Number>("scheduler.contexts", 4).toInt()
        val pluginsDir = config.getOrElse<String>("plugins.directory", "plugins")
        val pluginsEnableTimeoutMs = config.getOrElse<Number>("plugins.enableTimeoutMs", 10000).toLong()
        val pluginsDisableTimeoutMs = config.getOrElse<Number>("plugins.disableTimeoutMs", 5000).toLong()

        // validation
        val port = try {
            bind.substringAfterLast(":").toInt()
        } catch (e: Exception) {
            throw IllegalArgumentException("invalid bind address '$bind', expected host:port", e)
        }
        require(port in 1..65535) { "port out of range 1-65535: $port" }
        require(workers >= 0) { "workers must be >=0, got $workers" }
        require(readTimeoutSeconds >= 0) { "network.readTimeoutSeconds must be >=0, got $readTimeoutSeconds" }
        require(maxPacketSize in 1..8388608) { "protocol.maxPacketSize must be 1..8388608, got $maxPacketSize" }
        require(maxPlayers >= 0) { "status.maxPlayers must be >=0, got $maxPlayers" }
        require(quietPeriodMs >= 0) { "quietPeriodMs must be >=0" }
        require(timeoutMs >= 0) { "timeoutMs must be >=0" }
        require(scheduledCoreSize >= 1) { "threads.scheduledCoreSize must be >=1, got $scheduledCoreSize" }
        require(asyncParallelism >= 0) { "threads.asyncParallelism must be >=0, got $asyncParallelism" }
        require(tickRateMs in 10..1000) { "scheduler.tickRateMs must be 10..1000, got $tickRateMs" }
        require(contexts in 0..32) { "scheduler.contexts must be 0..32, got $contexts" }
        require(connectTimeoutMs in 100..60000) { "servers.connectTimeoutMs must be 100..60000, got $connectTimeoutMs" }
        require(retryAttempts in 0..10) { "servers.retryAttempts must be 0..10, got $retryAttempts" }
        require(retryDelayMs in 0..10000) { "servers.retryDelayMs must be 0..10000, got $retryDelayMs" }
        require(cooldownMs in 0..60000) { "servers.unavailableCooldownMs must be 0..60000, got $cooldownMs" }

        return ProxyConfig(
            network = NetworkConfig(bind = bind, workers = workers, readTimeoutSeconds = readTimeoutSeconds),
            protocol = ProtocolConfig(maxPacketSize = maxPacketSize, compressionThreshold = compressionThreshold),
            servers = ServersConfig(
                servers = serversMap,
                tryOrder = tryOrder,
                connectTimeoutMs = connectTimeoutMs,
                retryAttempts = retryAttempts,
                retryDelayMs = retryDelayMs,
                failoverOnUnexpectedDisconnect = failover,
                unavailableCooldownMs = cooldownMs
            ),
            security = SecurityConfig(onlineMode = onlineMode, forwardingSecret = forwardingSecret, forwardingMode = forwardingMode),
            status = StatusConfig(motd = motd, maxPlayers = maxPlayers, versionName = versionName, versionProtocol = versionProtocol),
            shutdown = ShutdownConfig(quietPeriodMs = quietPeriodMs, timeoutMs = timeoutMs),
            logging = LoggingConfig(level = level),
            threads = ThreadsConfig(
                virtual = virtual,
                scheduledCoreSize = scheduledCoreSize,
                asyncParallelism = asyncParallelism
            ),
            scheduler = SchedulerConfig(tickRateMs = tickRateMs, contexts = contexts),
            plugins = PluginsConfig(
                directory = pluginsDir,
                enableTimeoutMs = pluginsEnableTimeoutMs,
                disableTimeoutMs = pluginsDisableTimeoutMs
            )
        )
    }

    private fun defaultToml(): String = """
    # Candyriya proxy config - generated default
    # edit me and restart :)

    [network]
    bind = "0.0.0.0:25577"
    workers = 0
    readTimeoutSeconds = 30

    [protocol]
    maxPacketSize = 2097152
    compressionThreshold = 256

    [servers]
    lobby = "127.0.0.1:30066"
    factions = "127.0.0.1:30067"
    minigames = "127.0.0.1:30068"
    try = ["lobby"]
    connectTimeoutMs = 5000
    retryAttempts = 0
    retryDelayMs = 500
    failoverOnUnexpectedDisconnect = true
    unavailableCooldownMs = 5000

    [security]
    onlineMode = false
    forwardingSecret = ""
    forwardingMode = "NONE"

    [status]
    # MOTD shown in server list — MiniMessage format (<green>, <gradient>, etc.)
    motd = "<gradient:#55FF55:#55FFFF>Candyriya 26.1</gradient> <gray>—</gray> <white>proxy</white>"
    maxPlayers = 100
    versionName = "26.1"
    versionProtocol = 775

    [shutdown]
    quietPeriodMs = 200
    timeoutMs = 5000

    [logging]
    level = "INFO"

    [threads]
    virtual = true
    scheduledCoreSize = 2
    asyncParallelism = 0

    [scheduler]
    tickRateMs = 50
    contexts = 4

    [plugins]
    # where to load plugin jars from
    directory = "plugins"
    # per-plugin enable timeout (ms)
    enableTimeoutMs = 10000
    # per-plugin disable timeout (ms)
    disableTimeoutMs = 5000
    """.trimIndent()
}
