package kz.bejiihiu.candiriya.config

/**
 * Root proxy config. Mirrors the TOML structure.
 */
public data class ProxyConfig(
    val network: NetworkConfig = NetworkConfig(),
    val shutdown: ShutdownConfig = ShutdownConfig(),
    val logging: LoggingConfig = LoggingConfig(),
    val threads: ThreadsConfig = ThreadsConfig(),
    val scheduler: SchedulerConfig = SchedulerConfig()
)

public data class NetworkConfig(
    val bind: String = "0.0.0.0:25577",
    val workers: Int = 0
) {
    public fun host(): String = bind.substringBefore(":")

    public fun port(): Int = bind.substringAfterLast(":").toInt()
}

public data class ShutdownConfig(
    val quietPeriodMs: Long = 200,
    val timeoutMs: Long = 5000
)

public data class LoggingConfig(
    val level: String = "INFO"
)

public data class ThreadsConfig(
    val virtual: Boolean = true,
    val scheduledCoreSize: Int = 2,
    val asyncParallelism: Int = 0
)

public data class SchedulerConfig(
    val tickRateMs: Long = 50
)
