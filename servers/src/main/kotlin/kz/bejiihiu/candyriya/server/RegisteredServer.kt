package kz.bejiihiu.candyriya.server

/**
 * Backend server descriptor — like Velocity's RegisteredServer / ServerInfo.
 * Lives in :servers module now, not :network.
 */
public data class RegisteredServer(
    val name: String,
    val host: String,
    val port: Int
) {
    public fun address(): String = "$host:$port"
}


