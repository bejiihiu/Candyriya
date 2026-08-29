package kz.bejiihiu.candyriya.player

/**
 * Player-level state machine — higher than [kz.bejiihiu.candyriya.protocol.ConnectionState].
 * AUTHENTICATING etc. happen on async pool then hop back to context thread.
 */
public enum class PlayerState {
    HANDSHAKE,
    LOGIN,
    AUTHENTICATING,
    CONNECTING,
    PLAYING,
    DISCONNECTING,
    DISCONNECTED
}

/**
 * Valid transitions — be strict like Folia thread checks xd
 */
public object PlayerStateTransitions {
    private val allowed: Map<PlayerState, Set<PlayerState>> = mapOf(
        PlayerState.HANDSHAKE to setOf(
            PlayerState.LOGIN,
            PlayerState.DISCONNECTING,
            PlayerState.DISCONNECTED
        ),
        PlayerState.LOGIN to setOf(
            PlayerState.AUTHENTICATING,
            PlayerState.CONNECTING,
            PlayerState.PLAYING,
            PlayerState.DISCONNECTING,
            PlayerState.DISCONNECTED
        ),
        PlayerState.AUTHENTICATING to setOf(
            PlayerState.CONNECTING,
            PlayerState.DISCONNECTING,
            PlayerState.DISCONNECTED
        ),
        PlayerState.CONNECTING to setOf(
            PlayerState.PLAYING,
            PlayerState.DISCONNECTING,
            PlayerState.DISCONNECTED
        ),
        PlayerState.PLAYING to setOf(
            PlayerState.DISCONNECTING,
            PlayerState.DISCONNECTED
        ),
        PlayerState.DISCONNECTING to setOf(PlayerState.DISCONNECTED),
        PlayerState.DISCONNECTED to emptySet()
    )

    public fun can(from: PlayerState, to: PlayerState): Boolean = allowed[from]?.contains(to) == true

    public fun require(from: PlayerState, to: PlayerState) {
        check(can(from, to)) { "invalid player state transition $from -> $to" }
    }
}
