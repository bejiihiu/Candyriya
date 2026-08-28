package kz.bejiihiu.candiriya.protocol

public enum class ConnectionState {
    HANDSHAKE,
    STATUS,
    LOGIN,
    CONFIGURATION,
    PLAY,
    CLOSED
}
