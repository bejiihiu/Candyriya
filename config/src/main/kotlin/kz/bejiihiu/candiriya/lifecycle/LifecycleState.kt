package kz.bejiihiu.candiriya.lifecycle

/**
 * Lifecycle states for the proxy.
 * Transitions: STOPPED -> STARTING -> RUNNING -> STOPPING -> STOPPED
 */
public enum class LifecycleState {
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED
}
