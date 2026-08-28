package kz.bejiihiu.candiriya.config

import com.electronwill.nightconfig.core.file.CommentedFileConfig
import com.electronwill.nightconfig.core.io.ParsingMode
import java.nio.file.Files
import java.nio.file.Path
import org.apache.logging.log4j.LogManager

/**
 * Loads [ProxyConfig] from TOML. If file doesn't exist, creates default from resource.
 *
 * Synchronous [load] is fine on startup. For future reloads use [loadAsync] via scheduler.
 */
public object ConfigLoader {
    private val logger = LogManager.getLogger(ConfigLoader::class.java)

    // TODO: for future reloads this should go through scheduler — sync is ok only on startup xd
    public fun load(path: Path): ProxyConfig {
        if (Files.notExists(path)) {
            logger.info("config not found at {}, creating default", path)
            createDefault(path)
        }
        val fileConfig = CommentedFileConfig.builder(path).parsingMode(ParsingMode.ADD).build()
        fileConfig.load()
        return parse(fileConfig)
    }

    /**
     * Async wrapper for future reloads. Delegates to [load] via scheduler.
     * Keeps sync [load] intact. Pass `scheduler::execute` or any executor.
     *
     * Example: `ConfigLoader.loadAsync(path, scheduler::execute) { cfg -> ... }`
     */
    public fun loadAsync(
        path: Path,
        executor: (Runnable) -> Unit,
        callback: (ProxyConfig) -> Unit
    ) {
        // yep, just dispatch via scheduler, no new threads here xd
        executor { callback(load(path)) }
    }

    private fun createDefault(path: Path) {
        // try to copy from classpath resource, fallback to inline default
        val resource = ConfigLoader::class.java.getResourceAsStream("/candiriya.default.toml")
        if (resource != null) {
            resource.use { input ->
                Files.createDirectories(path.parent ?: Path.of("."))
                Files.copy(input, path)
            }
            logger.info("copied default config from resources to {}", path)
        } else {
            // fallback inline - this is cursed but better than nothing xd
            Files.createDirectories(path.parent ?: Path.of("."))
            Files.writeString(path, defaultToml())
            logger.info("wrote inline default config to {}", path)
        }
    }

    private fun parse(config: CommentedFileConfig): ProxyConfig {
        val bind = config.getOrElse<String>("network.bind", "0.0.0.0:25577")
        val workers = config.getOrElse<Number>("network.workers", 0).toInt()
        val quietPeriodMs = config.getOrElse<Number>("shutdown.quietPeriodMs", 200).toLong()
        val timeoutMs = config.getOrElse<Number>("shutdown.timeoutMs", 5000).toLong()
        val level = config.getOrElse<String>("logging.level", "INFO")
        val virtual = config.getOrElse<Boolean>("threads.virtual", true)
        val scheduledCoreSize = config.getOrElse<Number>("threads.scheduledCoreSize", 2).toInt()
        val asyncParallelism = config.getOrElse<Number>("threads.asyncParallelism", 0).toInt()
        val tickRateMs = config.getOrElse<Number>("scheduler.tickRateMs", 50).toLong()

        // validation
        val port = try {
            bind.substringAfterLast(":").toInt()
        } catch (e: Exception) {
            throw IllegalArgumentException("invalid bind address '$bind', expected host:port", e)
        }
        require(port in 1..65535) { "port out of range 1-65535: $port" }
        require(workers >= 0) { "workers must be >=0, got $workers" }
        require(quietPeriodMs >= 0) { "quietPeriodMs must be >=0" }
        require(timeoutMs >= 0) { "timeoutMs must be >=0" }
        require(
            scheduledCoreSize >= 1
        ) { "threads.scheduledCoreSize must be >=1, got $scheduledCoreSize" }
        require(
            asyncParallelism >= 0
        ) { "threads.asyncParallelism must be >=0, got $asyncParallelism" }
        require(tickRateMs in 10..1000) { "scheduler.tickRateMs must be 10..1000, got $tickRateMs" }

        return ProxyConfig(
            network = NetworkConfig(bind = bind, workers = workers),
            shutdown = ShutdownConfig(quietPeriodMs = quietPeriodMs, timeoutMs = timeoutMs),
            logging = LoggingConfig(level = level),
            threads = ThreadsConfig(
                virtual = virtual,
                scheduledCoreSize = scheduledCoreSize,
                asyncParallelism = asyncParallelism
            ),
            scheduler = SchedulerConfig(tickRateMs = tickRateMs)
        )
    }

    private fun defaultToml(): String = """
    # Candiriya proxy config - generated default
    # edit me and restart :)

    [network]
    # address to bind, format host:port
    bind = "0.0.0.0:25577"
    # netty worker threads, 0 = 2 * cpu count
    workers = 0

    [shutdown]
    # quiet period for netty graceful shutdown
    quietPeriodMs = 200
    # timeout for netty graceful shutdown
    timeoutMs = 5000

    [logging]
    # log level: TRACE, DEBUG, INFO, WARN, ERROR
    level = "INFO"

    [threads]
    # use virtual threads for async pool (java 21+)
    virtual = true
    # core size for scheduled pool (platform threads)
    scheduledCoreSize = 2
    # parallelism for async pool when virtual=false, 0 = cpu count
    asyncParallelism = 0

    [scheduler]
    # tick duration in ms (50ms = 20 tps, like Paper/Folia)
    tickRateMs = 50
    """.trimIndent()
}
