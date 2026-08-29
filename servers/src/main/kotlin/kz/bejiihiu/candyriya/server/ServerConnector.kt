package kz.bejiihiu.candyriya.server

import java.util.concurrent.CompletableFuture
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.apache.logging.log4j.LogManager

/**
 * Handles player -> server connection logic.
 * Used by Player.connect() sugar and ConnectionRequest impl.
 *
 * This is intentionally decoupled from netty — it delegates actual
 * BackendConnection creation to a callback so :servers stays netty-free.
 */
public class ServerConnector(
    private val registry: ServerRegistry
) {
    private val logger = LogManager.getLogger(ServerConnector::class.java)

    /**
     * Callback that does real netty connect.
     * Should call onSuccess(channel) or onFailed(cause).
     */
    public fun interface ConnectCallback {
        public fun doConnect(server: RegisteredServer, onSuccess: () -> Unit, onFailed: (Throwable) -> Unit)
    }

    /**
     * Decide next server for fallback chain after [failedServer].
     * Returns null if no available fallback (→ kick).
     */
    public fun nextFallback(failedServer: RegisteredServer?): RegisteredServer? = registry.fallbackFor(failedServer)

    /**
     * Validate connect target.
     */
    public fun validate(current: RegisteredServer?, target: RegisteredServer): ConnectionResult? {
        if (current != null && current.name.equals(target.name, ignoreCase = true)) {
            return ConnectionResult(ConnectionResult.Status.ALREADY_CONNECTED, null, target)
        }
        if (!registry.isAvailable(target)) {
            return ConnectionResult(
                ConnectionResult.Status.SERVER_DISCONNECTED,
                Component.text("Server ${target.name} is currently unavailable", NamedTextColor.RED),
                target
            )
        }
        return null
    }

    /**
     * Try servers in [tryOrder] sequentially until one succeeds.
     * Uses [callback] for each attempt. Returns future that completes on first success or last failure.
     */
    public fun connectWithFallback(
        current: RegisteredServer?,
        initialTarget: RegisteredServer,
        callback: ConnectCallback
    ): CompletableFuture<ConnectionResult> {
        val future = CompletableFuture<ConnectionResult>()
        val attempted = mutableListOf<RegisteredServer>()

        fun attempt(server: RegisteredServer) {
            attempted.add(server)
            val validation = validate(current, server)
            if (validation != null && validation.status == ConnectionResult.Status.ALREADY_CONNECTED) {
                future.complete(validation)
                return
            }
            if (validation != null && validation.status == ConnectionResult.Status.SERVER_DISCONNECTED) {
                // try next fallback
                val next = nextFallback(server)
                if (next != null && next !in attempted) {
                    logger.info("server {} unavailable, trying fallback {}", server.name, next.name)
                    attempt(next)
                } else {
                    future.complete(validation)
                }
                return
            }

            callback.doConnect(server, {
                future.complete(ConnectionResult(ConnectionResult.Status.SUCCESS, null, server))
            }, { cause ->
                registry.markUnavailable(server)
                val next = nextFallback(server)
                if (next != null && next !in attempted) {
                    logger.info("failed to connect to {} ({}), trying fallback {}", server.name, cause.message, next.name)
                    attempt(next)
                } else {
                    future.complete(
                        ConnectionResult(
                            ConnectionResult.Status.SERVER_DISCONNECTED,
                            Component.text("Could not connect to ${server.name}: ${cause.message ?: "unknown"}", NamedTextColor.RED),
                            server
                        )
                    )
                }
            })
        }

        attempt(initialTarget)
        return future
    }
}

