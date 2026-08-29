package kz.bejiihiu.candyriya.scheduler

// TODO: isolated scheduler for plugins — quota, ownership, bulk cancel on disable, sandbox blocking detection
/**
 * Stub for future per-plugin isolated scheduler.
 *
 * Planned features:
 * - per-plugin quota / rate limiting
 * - ownership tracking via [TaskOwner]
 * - bulk cancel on plugin disable (`cancelAll(pluginId)`)
 * - metrics / blocking detection
 * - sandboxing for untrusted plugin code
 */
public class PluginScheduler(
    private val delegate: Scheduler,
    @Suppress("URF_UNREAD_FIELD") private val owner: TaskOwner
) : Scheduler by delegate {
    // yep, just delegating for now xd
    // TODO: quota, per-plugin cancel, metrics
    init {
        // keep owner for future quota tracking xd
        check(owner.pluginId.isNotEmpty())
    }
}
