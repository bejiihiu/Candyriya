<<<<<<<< HEAD:network/src/main/kotlin/kz/bejiihiu/candyriya/player/RegisteredServer.kt
package kz.bejiihiu.candyriya.player
========
package kz.bejiihiu.candyriya.server
>>>>>>>> feature/servers:servers/src/main/kotlin/kz/bejiihiu/candiriya/server/RegisteredServer.kt

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

