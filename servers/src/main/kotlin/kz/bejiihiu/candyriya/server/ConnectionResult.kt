package kz.bejiihiu.candyriya.server

import net.kyori.adventure.text.Component

/**
 * Result of a connection attempt — single attempt to a backend.
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2"], justification = "data holder xd")
public data class ConnectionResult(
    val status: Status,
    val reason: Component? = null,
    val server: RegisteredServer? = null
) {
    public enum class Status {
        SUCCESS,
        ALREADY_CONNECTED,
        SERVER_DISCONNECTED,
        CANCELLED
    }

    public val isSuccess: Boolean get() = status == Status.SUCCESS
}

