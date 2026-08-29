package kz.bejiihiu.candyriya.plugin

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Marker for all proxy events.
 * Plugins should subclass this and fire via [EventBus.fire].
 */
public interface Event

/**
 * Events that can be cancelled like Bukkit/Velocity's `Cancellable`.
 */
public interface Cancellable : Event {
    public var isCancelled: Boolean
}

/** Priority like Velocity — lower ordinal runs first. */
public enum class EventPriority {
    LOWEST,
    LOW,
    NORMAL,
    HIGH,
    HIGHEST
}

/**
 * Annotation for method listeners (reflection path).
 *
 * ```kotlin
 * class MyListener {
 *   @Subscribe(priority = EventPriority.HIGH)
 *   fun onJoin(e: PlayerJoinEvent) { ... }
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class Subscribe(
    val priority: EventPriority = EventPriority.NORMAL
)

/**
 * Very small, synchronous-by-default EventBus.
 * Each plugin's listeners are auto-unregistered on disable via [PluginManager].
 *
 * Threading: `fire` runs on caller's thread; if you need per-plugin thread,
 * wrap listener with `context.scheduler.execute { }` or use `fireAsync`.
 */
public interface EventBus {
    /** Register all `@Subscribe` methods of [listener] for [pluginId]. */
    public fun register(pluginId: String, listener: Any)

    /** Register lambda for event type [T]. Returns handle to unregister. */
    public fun <T : Event> on(
        pluginId: String,
        eventClass: Class<T>,
        priority: EventPriority = EventPriority.NORMAL,
        handler: (T) -> Unit
    ): AutoCloseable

    /** Unregister all listeners of [pluginId] */
    public fun unregisterAll(pluginId: String)

    /** Fire event synchronously — listeners run in priority order on caller thread. */
    public fun <T : Event> fire(event: T): T

    /** Fire event asynchronously via global async pool, returns future completed after all listeners. */
    public fun <T : Event> fireAsync(event: T): CompletableFuture<T>
}

/** Built-in proxy events. Plugins can also define their own `Event`s. */
public class ProxyInitializeEvent : Event
public class ProxyShutdownEvent : Event

public class PlayerJoinEvent(
    public val player: ProxyPlayer
) : Event

public class PlayerDisconnectEvent(
    public val player: ProxyPlayer
) : Event

public class PlayerChooseBackendEvent(
    public val player: ProxyPlayer,
    public var target: RegisteredBackend?
) : Cancellable {
    override var isCancelled: Boolean = false
}

public class PluginMessageEvent(
    public val player: ProxyPlayer?,
    public val channel: String,
    public val data: ByteArray
) : Cancellable {
    override var isCancelled: Boolean = false
}

/** Internal impl — keep in api so `plugin-loader` can reuse without exposing internals to plugins. */
public class DefaultEventBus : EventBus {
    private data class Holder(
        val pluginId: String,
        val eventClass: Class<*>,
        val priority: EventPriority,
        val handler: (Event) -> Unit,
        val source: Any?
    )

    private val holders = CopyOnWriteArrayList<Holder>()
    private val byPlugin = ConcurrentHashMap<String, MutableList<Holder>>()

    override fun register(pluginId: String, listener: Any) {
        require(pluginId.isNotBlank())
        for (method in listener.javaClass.methods) {
            val ann = method.getAnnotation(Subscribe::class.java) ?: continue
            require(method.parameterCount == 1) { "@Subscribe ${method.name} must have 1 param" }
            val eventClass = method.parameterTypes[0]
            require(Event::class.java.isAssignableFrom(eventClass)) {
                "@Subscribe param must be Event, got $eventClass"
            }
            @Suppress("UNCHECKED_CAST")
            val handler: (Event) -> Unit = { e ->
                method.invoke(listener, e)
            }
            val h = Holder(pluginId, eventClass, ann.priority, handler, listener)
            holders.add(h)
            byPlugin.computeIfAbsent(pluginId) { mutableListOf() }.add(h)
            holders.sortBy { it.priority.ordinal }
        }
    }

    override fun <T : Event> on(pluginId: String, eventClass: Class<T>, priority: EventPriority, handler: (T) -> Unit): AutoCloseable {
        @Suppress("UNCHECKED_CAST")
        val h = Holder(pluginId, eventClass, priority, handler as (Event) -> Unit, null)
        holders.add(h)
        byPlugin.computeIfAbsent(pluginId) { mutableListOf() }.add(h)
        holders.sortBy { it.priority.ordinal }
        return AutoCloseable {
            holders.remove(h)
            byPlugin[pluginId]?.remove(h)
        }
    }

    override fun unregisterAll(pluginId: String) {
        val list = byPlugin.remove(pluginId) ?: return
        holders.removeAll(list.toSet())
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Event> fire(event: T): T {
        val clazz = event::class.java
        // snapshot to avoid CME if listener registers inside fire xd
        val snapshot = holders.toList()
        for (h in snapshot) {
            if (h.eventClass.isAssignableFrom(clazz)) {
                try {
                    h.handler(event)
                } catch (_: Exception) {
                    // не валим весь прокси из-за кривого плагина, лог в loader'е
                }
            }
        }
        return event
    }

    override fun <T : Event> fireAsync(event: T): CompletableFuture<T> = CompletableFuture.supplyAsync { fire(event) }
}

