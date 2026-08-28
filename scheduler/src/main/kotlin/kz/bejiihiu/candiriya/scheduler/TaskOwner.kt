package kz.bejiihiu.candiriya.scheduler

/**
 * Ownership marker for tasks. Nullable for now, will be used to isolate per-plugin schedulers.
 */
public data class TaskOwner(
    val pluginId: String
)
