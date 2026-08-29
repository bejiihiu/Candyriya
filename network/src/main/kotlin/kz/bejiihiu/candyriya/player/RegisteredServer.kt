package kz.bejiihiu.candyriya.player

/**
 * Backend server descriptor — like Velocity's RegisteredServer / ServerInfo.
 * For now single backend, but structure ready for Map<String, RegisteredServer>.
 */
public data class RegisteredServer(
    val name: String,
    val host: String,
    val port: Int
) {
    public fun address(): String = "$host:$port"
}
