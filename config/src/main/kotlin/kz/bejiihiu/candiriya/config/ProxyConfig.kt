package kz.bejiihiu.candiriya.config

/**
 * Root proxy config. Mirrors the TOML structure.
 */
public data class ProxyConfig(
    val network: NetworkConfig = NetworkConfig(),
    val protocol: ProtocolConfig = ProtocolConfig(),
    val status: StatusConfig = StatusConfig(),
    val backend: BackendConfig = BackendConfig(),
    val security: SecurityConfig = SecurityConfig(),
    val shutdown: ShutdownConfig = ShutdownConfig(),
    val logging: LoggingConfig = LoggingConfig(),
    val threads: ThreadsConfig = ThreadsConfig(),
    val scheduler: SchedulerConfig = SchedulerConfig(),
    val plugins: PluginsConfig = PluginsConfig()
)

public data class NetworkConfig(
    val bind: String = "0.0.0.0:25577",
    val workers: Int = 0,
    val readTimeoutSeconds: Int = 30
) {
    public fun host(): String = bind.substringBefore(":")

    public fun port(): Int = bind.substringAfterLast(":").toInt()
}

public data class ProtocolConfig(
    val maxPacketSize: Int = 2097152,
    val compressionThreshold: Int = 256
)

public data class BackendConfig(
    val host: String = "127.0.0.1",
    val port: Int = 25565,
    val connectTimeoutMs: Int = 5000,
    val retryAttempts: Int = 0,
    val retryDelayMs: Long = 500
) {
    // TODO: picks up when we add multi-backend like velocity's [servers] map
    // for now single backend, but structure is ready for Map<String, BackendConfig> xd
}

public enum class ForwardingMode {
    NONE,
    LEGACY,
    BUNGEEGUARD,
    MODERN
}

public data class SecurityConfig(
    val onlineMode: Boolean = false,
    val forwardingSecret: String = "",
    val forwardingMode: ForwardingMode = ForwardingMode.NONE
)

public data class StatusConfig(
    val motd: String = DEFAULT_MOTD,
    val maxPlayers: Int = 100,
    val versionName: String = "26.1",
    val versionProtocol: Int = 775
) {
    public companion object {
        public const val DEFAULT_MOTD: String =
            "<gradient:#55FF55:#55FFFF>Candiriya 26.1</gradient>" +
                " <gray>—</gray> <white>proxy</white>"
    }
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
    val tickRateMs: Long = 50,
    val contexts: Int = 4
)

public data class PluginsConfig(
    val directory: String = "plugins",
    val enableTimeoutMs: Long = 10000,
    val disableTimeoutMs: Long = 5000
)
