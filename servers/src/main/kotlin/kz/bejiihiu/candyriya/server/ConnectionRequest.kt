package kz.bejiihiu.candyriya.server

import java.util.concurrent.CompletableFuture

/**
 * Fluent builder for moving a player to another server.
 * Mirrors Velocity's ConnectionRequestBuilder but uses our scheduler/context.
 *
 * Usage:
 * ```
 * player.createConnectionRequest(registry.get("lobby")!!).connect()
 * player.connect(lobby) // sugar
 * ```
 */
public interface ConnectionRequest {
    public fun getServer(): RegisteredServer

    public fun connect(): CompletableFuture<ConnectionResult>

    public fun connectWithIndication(): CompletableFuture<Boolean>

    public fun fireAndForget()
}

