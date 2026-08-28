package kz.bejiihiu.candiriya.config

/**
 * Root proxy config. Mirrors the TOML structure.
 */
public data class ProxyConfig(
    val network: NetworkConfig = NetworkConfig(),
    val shutdown: ShutdownConfig = ShutdownConfig(),
    val logging: LoggingConfig = LoggingConfig()
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
