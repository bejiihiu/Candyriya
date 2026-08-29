# Candiriya Plugin System — Full Documentation

> **Status:** implemented in `feature/plugin-system`. Plugins load at `STARTING`, enable before `NetworkServer` binds, disable at `STOPPING`. No hot-reload (restart required).  
> **Modules:** `:plugin-api` (public contracts), `:plugin-loader` (private loader/manager), `:core` (integration), `:command` ( updated `/candiriya plugins`).

---

## Table of Contents

1. [Quick Start (5 minutes)](#1-quick-start)
2. [Mental Model — How Candiriya differs from Velocity/Bungee](#2-mental-model)
3. [Project Layout & Modules](#3-project-layout)
4. [plugin.json — The Descriptor](#4-pluginjson)
5. [Lifecycle — load → enable → disable](#5-lifecycle)
6. [ClassLoader — Hybrid Isolation](#6-classloader)
7. [Threading — Per-Plugin Thread + Scheduler](#7-threading)
8. [EventBus — Annotation & Lambda](#8-eventbus)
9. [ProxyServer & Player Handles](#9-proxyserver--player-handles)
10. [Commands — PluginCommandManager](#10-commands)
11. [Permissions — PermissionRegistry](#11-permissions)
12. [Messaging — Plugin Channels](#12-messaging)
13. [Config & Data Folder](#13-config--data-folder)
14. [Scheduler — PluginScheduler in Depth](#14-scheduler)
15. [Building a Plugin — Gradle (Kotlin & Java)](#15-building-a-plugin)
16. [Installing, Logs, Debugging](#16-installing-logs-debugging)
17. [Velocity Bridge — Running Velocity Plugins Inside Candiriya](#17-velocity-bridge)
18. [Best Practices & Pitfalls](#18-best-practices--pitfalls)
19. [API Stability & Versioning](#19-api-stability)
20. [FAQ](#20-faq)
21. [Reference — All Interfaces at a Glance](#21-reference)

---

## 1. Quick Start

### 1.1 Minimal Kotlin plugin

`src/main/resources/plugin.json`

```json
{
  "id": "hello",
  "name": "HelloPlugin",
  "version": "1.0.0",
  "main": "com.example.HelloPlugin",
  "apiVersion": "1"
}
```

`src/main/kotlin/com/example/HelloPlugin.kt`

```kotlin
package com.example

import kz.bejiihiu.candiriya.plugin.Plugin
import kz.bejiihiu.candiriya.plugin.PluginContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

class HelloPlugin : Plugin {
    override fun onEnable(ctx: PluginContext) {
        ctx.logger.info("Hello from {}!", ctx.description.id)

        ctx.commands.register("hello", object : kz.bejiihiu.candiriya.plugin.PluginCommand {
            override val permission: String? = null
            override val description = "says hi"
            override val usage = ""
            override fun execute(source: kz.bejiihiu.candiriya.plugin.PluginCommandSource, args: Array<String>) {
                source.sendMessage(Component.text("hi ${source.name}", NamedTextColor.GREEN))
            }
        })

        ctx.scheduler.delayed(java.time.Duration.ofSeconds(2)) {
            ctx.server.broadcast(Component.text("Hello after 2s!"))
        }
    }
}
```

### 1.2 Build

`build.gradle.kts` (plugin is **not** part of the proxy build — build it standalone):

```kotlin
plugins { kotlin("jvm") version "2.4.0" }
repositories { mavenCentral() }
dependencies {
    compileOnly(project(":plugin-api")) // or publish plugin-api to mavenLocal and use maven coords
    compileOnly("org.apache.logging.log4j:log4j-api:2.24.3")
    compileOnly("net.kyori:adventure-api:4.24.0")
}
tasks.jar {
    archiveBaseName.set("hello-plugin")
    from("src/main/resources") { include("plugin.json") }
}
```

```bash
./gradlew jar
cp build/libs/hello-plugin-1.0.0.jar /path/to/candiriya/plugins/
./gradlew :launcher:run   # or java -jar candiriya.jar
# logs: [candiriya-plugin-hello] INFO Hello from hello!
# in game / console: /hello, /candiriya plugins -> HelloPlugin v1.0.0 [ENABLED]
```

Full working example lives in `examples/hello-plugin/` in this repo.

---

## 2. Mental Model

| Concept | Velocity/Bungee | Candiriya |
|---|---|---|
| Descriptor | `velocity-plugin.json` / `plugin.yml` | `plugin.json` inside jar root (TOML not used here to keep plugin jars zero-dependency) |
| Main class | annotated `@Plugin` or `extends Plugin` | `implements Plugin` (interface with `onLoad/onEnable/onDisable`) |
| ClassLoader | child-first or shaded | **Hybrid** child-first (see §6) — shared lib set is parent-first |
| Thread per plugin | no, all on main | **yes, one dedicated thread per plugin** (virtual if `threads.virtual=true`) |
| Scheduler | `ProxyServer.getScheduler()` shared | `ctx.scheduler: PluginScheduler` — auto-tagged, auto-cancelled |
| Events | `EventManager` global | `ctx.events: EventBus` scoped + `unregisterAll(pluginId)` on disable |
| Commands | `CommandManager` global | `ctx.commands: PluginCommandManager` scoped, auto-unregistered |
| Messaging | `ChannelIdentifier` | `ctx.messaging: PluginMessaging` — `namespace:name` channels |
| Player | `Player` mutable from any thread | `ProxyPlayer` handle + `player.execute {}` to hop to player's context |

**Goal:** you write a normal Jar, Candiriya gives you a `PluginContext` with everything scoped to your `id`. When your plugin disables, its tasks/listeners/commands vanish automatically — you don't leak.

**Velocity bridge:** because `Plugin` is just an interface and `PluginManager` never imports Velocity types, a *single* Candiriya plugin can *host* an entire Velocity `PluginManager` inside its own ClassLoader. See §17.

---

## 3. Project Layout

```
candiriya/
  settings.gradle.kts          // includes :plugin-api, :plugin-loader
  config/
    src/main/resources/candiriya.default.toml // [plugins] directory = "plugins"
  plugin-api/                  // PUBLIC, explicitApi(), no core deps
    src/main/kotlin/kz/bejiihiu/candiriya/plugin/
      Plugin.kt                // Plugin + PluginContext
      PluginDescription.kt     // plugin.json parser
      ProxyServer.kt           // ProxyServer / ProxyPlayer / RegisteredBackend
      EventBus.kt              // Event, Cancellable, EventBus, Subscribe, DefaultEventBus + built-ins
      PluginScheduler.kt       // PluginScheduler / PluginTask / DefaultPluginScheduler
      PluginExtras.kt          // PluginCommandManager, PermissionRegistry, PluginMessaging
  plugin-loader/               // PRIVATE, depends on plugin-api + scheduler + config + network
    src/main/kotlin/kz/bejiihiu/candiriya/plugin/loader/
      PluginClassLoader.kt     // hybrid child-first
      PluginContainer.kt       // holder + per-plugin executor
      ProxyServerImpl.kt       // PlayerManager → ProxyServer adapter
      PluginManager.kt         // scan, load, enable, disable, command/messaging wiring
  core/
    src/main/kotlin/kz/bejiihiu/candiriya/Candiriya.kt // creates PluginManager, loadAll -> enableAll -> disableAll
  command/
    src/main/kotlin/kz/bejiihiu/candiriya/command/builtin/CandiriyaCommand.kt // /candiriya plugins now lists real plugins
  examples/
    hello-plugin/              // copy-paste starter
    velocity-bridge/           // sketch for hosting Velocity jars
```

**Dependency rule:** `plugin-api` **never** depends on `plugin-loader` / `core` / `network`. A plugin jar compiled against `plugin-api` has zero transitive deps except `adventure-api`, `guava`, `kotlinx-serialization`, `log4j-api` (all `compileOnly` at runtime — proxy provides them).

---

## 4. plugin.json

Must sit at **jar root** (`src/main/resources/plugin.json` → jar entry `plugin.json`). Parsed with `kotlinx.serialization` + validated.

### 4.1 Schema

```json
{
  "id": "myplugin",                 // required, ^[a-z0-9_-]{3,32}$  lowercase
  "name": "MyPlugin",               // required, 1..64 chars, display name for /candiriya plugins
  "version": "1.0.0",               // required, ^[0-9A-Za-z._-]{1,32}$  (semver-ish, no spaces)
  "main": "com.example.MyPlugin",  // required, fully qualified class name with no-arg constructor
  "apiVersion": "1",                // optional, default "1". Bump when we break API.
  "description": "does X",          // optional
  "authors": ["you"],               // optional
  "depends": ["otherplugin"],       // optional, 0..16 ids that must be loaded before this one
  "isolated": true,                 // optional, default true. false → shared ClassLoader
  "sharedLibraries": []             // optional, extra prefixes forced to parent-first (rare)
}
```

### 4.2 Validation errors (fail fast on load)

- invalid `id` / `version` / `main`
- `depends` contains self or unknown id (plugin marked `FAILED`, never enabled)
- duplicate `id` across two jars (second jar skipped, error logged)
- missing `plugin.json` (jar skipped, warn)
- `main` class not found or does not implement `Plugin` or lacks no-arg constructor (jar skipped, error)

### 4.3 Example full

```json
{
  "id": "chatfilter",
  "name": "ChatFilter",
  "version": "2.3.1",
  "main": "kz.example.ChatFilterPlugin",
  "apiVersion": "1",
  "description": "Filters chat and forwards to Discord",
  "authors": ["bejiihiu", "alice"],
  "depends": ["database"],
  "isolated": true,
  "sharedLibraries": ["com.example.shared"]
}
```

---

## 5. Lifecycle

```
File scan          plugin.onLoad()          plugin.onEnable(ctx)          proxy RUNNING
   |                      |                         |                           |
   |  loadAll()           |  enableAll()            |                           |   stop()
   |  (STARTING,          |  (still STARTING,        |                           |   disableAll()
   v   before bind)       v   before bind)          v                           v   (STOPPING)
[DISCOVER *.jar] -> [instantiate main] -> [LOADED] -> [ENABLED] -> ... -> [DISABLED] -> close ClassLoader
                                      \-> [FAILED] if exception/timeout
```

**Timings:** each phase runs on the plugin's **own thread** with a timeout (`plugins.enableTimeoutMs` default 10s, `disableTimeoutMs` 5s from `candiriya.toml [plugins]`). If your `onEnable` blocks longer, the proxy logs `plugin X timed out` and marks `FAILED` (won't receive events/commands).

**Order:** `depends` guarantees topological enable order; disable is reverse. Events `ProxyInitializeEvent` and `ProxyShutdownEvent` fire on the `EventBus` after all enables / before any disables.

**What to do where:**

- `onLoad()` — parse config, validate `plugin.json` extras, create data dir structure. **No** proxy APIs yet (`ctx` is null). Keep it <1s.
- `onEnable(ctx)` — register events, commands, channels, schedule tasks, open DB connections. This is where 99% of code lives.
- `onDisable()` — flush caches, close DB. You don't need to unregister listeners or cancel tasks — `PluginManager` does it right after you return.

**Restart-only:** `PluginManager.closeAll()` closes each `URLClassLoader` and shuts down its executor. Hot-reload is intentionally not supported (Java `URLClassLoader` leaks + Netty state). Restart the proxy to update jars.

---

## 6. ClassLoader — Hybrid Isolation

### 6.1 Why hybrid?

- **Pure parent-first** (`URLClassLoader` default) → two plugins with different `okhttp:4.x` vs `okhttp:5.x` collide — first wins, second crashes.
- **Pure child-first** → `kotlin.Metadata`, `adventure Key`, `guava` singletons duplicate — `ClassCastException: class kotlin.collections… cannot be cast`.

**Hybrid = best of both:** curated shared prefixes are parent-first, everything else child-first.

```kotlin
DEFAULT_SHARED = [
  "kotlin.", "kotlinx.",
  "net.kyori.",                     // adventure
  "com.google.common.",             // guava
  "org.apache.logging.log4j.", "org.slf4j.",
  "kz.bejiihiu.candiriya.plugin.",  // plugin-api itself
  "java.", "jdk.", "sun."
]
```

You can extend it per-plugin:

```json
{ "sharedLibraries": ["com.example.sharedlib"] }
```

Then `com.example.sharedlib.Foo` will be parent-first (all plugins see the proxy's copy). Useful if you shade a common lib into the proxy and want plugins to share it.

### 6.2 isolated true vs false

- **`isolated: true` (default)** — each jar gets its own `PluginClassLoader(arrayOf(jarUrl), parent)`. Full isolation, safe for most plugins. Overhead ~1 ClassLoader + 1 thread per plugin (cheap).
- **`isolated: false`** — jars share one `PluginClassLoader` that accumulates URLs via `addURL`. All `isolated:false` plugins see each other's classes. Useful for a suite of plugins that must share internal classes without shading. Disabled plugins still keep the shared loader alive (only closed when last plugin closes).

**Recommendation:** leave default `true`. Set `false` only for a coordinated suite you control.

---

## 7. Threading — Per-Plugin Thread + Scheduler

### 7.1 The rule

- **Never block Netty or Tick threads.** If you do I/O, sleep, or heavy compute, use `ctx.scheduler`.
- Every plugin gets **one dedicated thread** (`candiriya-plugin-<id>-#`) backed by virtual threads when `threads.virtual=true` (default). It behaves like a single-threaded `Executor` — tasks queue, run sequentially. So you can use non-concurrent collections inside your plugin without locking *if you stay on your thread*.
- `ctx.events` dispatches on **caller thread** (usually network thread). If your listener touches mutable plugin state, either make it thread-safe or hop: `ctx.scheduler.execute { /* mutate */ }`.
- `ProxyPlayer.execute {}` hops to the player's **context thread** (Folia-like region). Use it when you mutate player state (e.g., after async DB fetch, hop back to modify player).

### 7.2 Thread factories backing it

`PluginContainer.createExecutor(id, useVirtual)`:

```kotlin
if (useVirtual) Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("candiriya-plugin-$id-",0).factory())
else Executors.newSingleThreadExecutor { Thread(it, "candiriya-plugin-$id").apply { isDaemon=true } }
```

`ctx.scheduler: DefaultPluginScheduler` delegates to that executor + the proxy's shared `scheduledPool` (platform threads, 2 threads) for timers.

**Timeout:** if `onEnable/onDisable/onLoad` never returns, `PluginManager` times out and marks `FAILED`. Don't `Thread.sleep(Long.MAX_VALUE)` there.

### 7.3 Example — correct async pattern

```kotlin
override fun onEnable(ctx: PluginContext) {
    ctx.events.on(ctx.description.id, PlayerJoinEvent::class.java) { event ->
        // this runs on network thread — don't block here
        ctx.scheduler.async {
            val data = database.load(event.player.uuid) // I/O on plugin thread (virtual, cheap to block)
            // hop to player's context before touching player
            event.player.execute {
                event.player.sendMessage(Component.text("Your rank: ${data.rank}"))
            }
        }
    }
}
```

**Anti-pattern:**

```kotlin
ctx.events.on(...) { event ->
    Thread.sleep(5000) // blocks network thread — players lag, watchdog fires
}
```

---

## 8. EventBus

### 8.1 Core types

```kotlin
interface Event
interface Cancellable : Event { var isCancelled: Boolean }
enum class EventPriority { LOWEST, LOW, NORMAL, HIGH, HIGHEST } // LOWEST fires first
@Retention(RUNTIME) annotation class Subscribe(val priority: EventPriority = NORMAL)
```

Built-ins: `ProxyInitializeEvent`, `ProxyShutdownEvent`, `PlayerJoinEvent(val player)`, `PlayerDisconnectEvent(val player)`, `PlayerChooseBackendEvent(val player, var target) : Cancellable`, `PluginMessageEvent`.

### 8.2 Registering

**Annotation style** (good for multiple handlers in one class):

```kotlin
class MyListener(private val ctx: PluginContext) {
    @Subscribe(priority = EventPriority.HIGH)
    fun onJoin(e: PlayerJoinEvent) {
        ctx.logger.info("{} joined", e.player.username)
    }
    @Subscribe
    fun onLeave(e: PlayerDisconnectEvent) {
        ctx.logger.info("{} left", e.player.username)
    }
}
ctx.events.register(ctx.description.id, MyListener(ctx))
```

**Lambda style** (good for one-off):

```kotlin
val handle = ctx.events.on(ctx.description.id, PlayerJoinEvent::class.java) { e ->
    e.player.sendMessage(Component.text("hi"))
}
// optional: early unregister
handle.close()
```

Both auto-unregister on `disableAll()` via `unregisterAll(pluginId)`.

### 8.3 Firing

```kotlin
val event = PlayerChooseBackendEvent(player, targetServer)
ctx.events.fire(event) // synchronous, listeners run on your thread in priority order
if (!event.isCancelled) connect(event.target)

ctx.events.fireAsync(event) // runs on async pool, returns CompletableFuture<T>
```

If a listener throws, it is caught and logged; the rest still fire.

### 8.4 Ordering

`DefaultEventBus` keeps a `CopyOnWriteArrayList` sorted by `priority.ordinal`. `on()` and `register()` insert and resort (cheap — plugin count is small). Firing snapshots the list first (`toList()`) so a listener can register inside a fire without CME.

---

## 9. ProxyServer & Player Handles

### 9.1 ProxyServer

```kotlin
interface ProxyServer {
    val config: ProxyConfig          // snapshot, never mutates for plugins
    val version: String              // "26.1"
    fun getPlayers(): Collection<ProxyPlayer>
    fun getPlayer(uuid: UUID): Optional<ProxyPlayer>
    fun getPlayer(name: String): Optional<ProxyPlayer> // case-insensitive
    fun getPlayerCount(): Int
    fun getServers(): Map<String, RegisteredBackend>
    fun getServer(name: String): Optional<RegisteredBackend>
    fun broadcast(Component)
    fun isOnlineMode(): Boolean
}
```

Access: `ctx.server`.

**Important:** `getPlayers()` returns a **snapshot copy** — iterating while players join/leave is safe. `ProxyServerImpl` delegates to `PlayerManager.all().map { wrap(it) }`.

### 9.2 ProxyPlayer

```kotlin
interface ProxyPlayer {
    val uuid: UUID
    val username: String
    val currentServer: RegisteredBackend?
    val isOnline: Boolean
    fun sendMessage(Component)
    fun disconnect(Component)
    fun hasPermission(String): Boolean // stub — real check is via PermissionRegistry
    fun execute(Runnable)             // hop to player's context
    fun getRawHandle(): Any           // escape hatch -> core Player, avoid if possible
}
```

`sendMessage` is currently a **stub** that logs at debug (chat packet wiring is TODO). `disconnect` works (sends `MinecraftPacket 0x00` with MiniMessage).

### 9.3 RegisteredBackend

```kotlin
interface RegisteredBackend {
    val name: String
    val host: String
    val port: Int
    fun sendPlayer(ProxyPlayer): Boolean // stub until multi-backend lands
}
```

Currently single `default` backend from `candiriya.toml [backend]`. Map-shaped so future `[servers]` doesn't break API.

---

## 10. Commands

### 10.1 PluginCommand

```kotlin
interface PluginCommand {
    val permission: String?
    val description: String
    val usage: String
    fun execute(source: PluginCommandSource, args: Array<String>)
    fun suggest(source: PluginCommandSource, args: Array<String>): List<String> = emptyList()
}

interface PluginCommandSource {
    val name: String
    val isConsole: Boolean
    fun hasPermission(String): Boolean
    fun sendMessage(Component)
    fun asPlayer(): ProxyPlayer?
}
```

### 10.2 Registering

```kotlin
ctx.commands.register("mycmd", myCmd, "myalias", "mcalias")
// or
ctx.commands.unregister("mycmd")
ctx.commands.ownedAliases() // set of aliases this plugin owns
```

Ownership is tracked in `PluginManager.ownedCommands: ConcurrentHashMap<pluginId, MutableSet<alias>>`. On `disable`, all owned aliases are `commandManager.unregister()` automatically — you don't leak.

### 10.3 Execution threading

When a player runs `/mycmd`, core `CommandManager` finds the wrapped command and:

- checks `hasPermission` synchronously
- then **dispatches `execute` on your plugin thread** (`container.executor.submit { command.execute(...) }`)

So `execute` always runs on `candiriya-plugin-<id>` — you can touch plugin state without locking.

`suggest` (tab-complete) runs on caller thread (no hop) — keep it fast & side-effect-free.

### 10.4 Built-in `/candiriya plugins`

`CandiriyaCommand` now takes `pluginsProvider: (() -> List<PluginInfo>)?` from core and shows:

```
> candiriya plugins
Plugins (2):
- HelloPlugin (hello) v1.0.0 [ENABLED]
- ChatFilter (chatfilter) v2.3.1 [FAILED]
```

No provider → old placeholder text. This avoids `command` depending on `plugin-loader`.

---

## 11. Permissions

### 11.1 PermissionRegistry

```kotlin
interface PermissionRegistry {
    fun has(player: ProxyPlayer, permission: String): Boolean
    fun has(source: PluginCommandSource, permission: String): Boolean
}
```

Access: `ctx.permissions`.

Impl delegates to core `PermissionManager.permissionValue(PlayerSubject, perm) == Tristate.TRUE`, which honors groups, wildcards (`candiriya.*`), ops, and external `PermissionProvider` (e.g., LuckPerms bridge you write).

**Pattern:** give your command a permission node:

```kotlin
object MyCmd : PluginCommand {
    override val permission = "myplugin.cmd"
    ...
}
```

Then `source.hasPermission("myplugin.cmd")` inside handler (core already checks before `execute`, but you may want finer checks).

---

## 12. Messaging

Plugin messages (`minecraft:brand` / `velocity:player_info` style). Minimal stub.

```kotlin
interface PluginMessaging {
    fun registerChannel(channel: String): Boolean   // "myplugin:data" — namespace:name
    fun unregisterChannel(channel: String): Boolean
    fun send(player: ProxyPlayer, channel: String, data: ByteArray): Boolean
    fun channels(): Set<String>
}
```

- `registerChannel` validates `^[a-z0-9_-]+:[a-z0-9/_-]+$`, stores in `globalChannels: ConcurrentHashMap.newKeySet`.
- `send` currently only checks `globalChannels.contains` and logs — actual netty write will be wired to `PlayerConnection.sendPacket` with the `minecraft:plugin_message` packet once protocol framing lands. Channel API is stable; wiring change is invisible to plugins.

Listen for incoming plugin messages via:

```kotlin
ctx.events.on(ctx.description.id, PluginMessageEvent::class.java) { e ->
    if (e.channel == "myplugin:data") {
        val payload = String(e.data)
        ...
    }
}
```

---

## 13. Config & Data Folder

Each plugin gets `dataDirectory = plugins/<id>/` (created on `loadOne`). No auto config framework is bundled (to keep `plugin-api` small), but helper is suggested:

```kotlin
interface PluginConfig {
    val directory: Path
    fun loadOrCreate(fileName: String = "config.toml", defaultResource: String?): Path
}
```

**Simplest:** roll your own with `nightconfig` (proxy already depends on it — you can shade it):

```kotlin
val configFile = ctx.dataDirectory.resolve("config.toml")
if (Files.notExists(configFile)) {
    javaClass.getResourceAsStream("/default-config.toml")!!.use { Files.copy(it, configFile) }
}
val cfg = CommentedFileConfig.builder(configFile).build().also { it.load() }
val greeting = cfg.get<String>("greeting")
```

Or use `kotlinx.serialization` for JSON.

**Tip:** never hardcode `Paths.get("plugins/...")` — always use `ctx.dataDirectory`. Isolates tests and respects `candiriya.toml [plugins] directory`.

---

## 14. Scheduler

### 14.1 PluginScheduler

```kotlin
interface PluginScheduler {
    val pluginId: String
    fun execute(Runnable): PluginTask
    fun delayed(Duration, Runnable): PluginTask
    fun repeating(Duration, Duration, Runnable): PluginTask
    fun <T> async(Callable<T>): CompletableFuture<T>
    fun cancelAll(): Int
}

interface PluginTask {
    fun cancel(): Boolean
    val isCancelled: Boolean
    val isDone: Boolean
}
```

### 14.2 Impl

`DefaultPluginScheduler(pluginId, asyncExecutor, scheduledExecutor)`:

- `execute` → `asyncExecutor.submit(wrapped)` (plugin thread)
- `delayed` → `scheduledExecutor.schedule(wrapped, delay)` → hops to `asyncExecutor`
- `repeating` → `scheduledExecutor.scheduleAtFixedRate` → each tick hops to `asyncExecutor`
- `async` → `CompletableFuture.supplyAsync(block, asyncExecutor)`

All tasks are tracked in `ConcurrentHashMap<PluginTaskImpl, Boolean>` so `cancelAll()` can kill them on disable.

### 14.3 Core vs Plugin scheduler

- Core `Scheduler` (`DefaultScheduler`) → used by proxy internals, no owner.
- `PluginScheduler` → tags every task with `pluginId`, so `PluginManager.disableAll()` can `cancelAll()` without touching other plugins' tasks.
- `TickScheduler` / `ContextScheduler` are **not** exposed to plugins directly (to prevent `tick 50ms` stalls). If you need tick alignment, use `repeating(50ms, 50ms)` on `PluginScheduler` — it's close enough and doesn't block the tick thread.

### 14.4 Coroutine interop

`asyncExecutor` also backs a `CoroutineDispatcher` (`asyncPool.asCoroutineDispatcher()` in `ThreadController`). If you prefer coroutines:

```kotlin
ctx.scheduler.async { blockingCall() } // future
// or inside a coroutine scope you launch yourself:
CoroutineScope(ctx.scheduler.asDispatcher()).launch { ... }
```

Virtual threads mean blocking inside `async` is cheap (parking, not carrier pinning, as long as you don't `synchronized` on a monitor — prefer `ReentrantLock`).

---

## 15. Building a Plugin

### 15.1 Kotlin — minimal `build.gradle.kts`

```kotlin
plugins { kotlin("jvm") version "2.4.0" }

repositories { mavenCentral() }

dependencies {
    compileOnly(project(":plugin-api")) // within monorepo
    // or out-of-repo: compileOnly("kz.bejiihiu:candiriya-plugin-api:26.1")
    compileOnly("org.apache.logging.log4j:log4j-api:2.24.3")
    compileOnly("net.kyori:adventure-api:4.24.0")
}

tasks.jar {
    archiveBaseName.set("my-plugin")
    from("src/main/resources") { include("plugin.json") }
    // shade deps if you need them at runtime — exclude plugin-api itself
    // from(configurations.runtimeClasspath.get().map { if (it.name.contains("plugin-api")) null else zipTree(it) })
}
```

`settings.gradle.kts` for out-of-repo plugin:

```kotlin
includeBuild("/path/to/candiriya") // or publish plugin-api to mavenLocal
```

### 15.2 Java — minimal `pom.xml` snippet

```xml
<dependency>
  <groupId>kz.bejiihiu.candiriya</groupId>
  <artifactId>plugin-api</artifactId>
  <version>26.1</version>
  <scope>provided</scope>
</dependency>
```

```java
package com.example;

import kz.bejiihiu.candiriya.plugin.Plugin;
import kz.bejiihiu.candiriya.plugin.PluginContext;
import org.jetbrains.annotations.NotNull;

public class MyJavaPlugin implements Plugin {
    @Override
    public void onEnable(@NotNull PluginContext ctx) {
        ctx.getLogger().info("Hello from Java!");
        ctx.getCommands().register("myjava", new MyCommand(), "mj");
    }
}
```

Java works because `plugin-api` is Kotlin but exposes pure JVM interfaces — `PluginClassLoader` just does `loadClass(main).getDeclaredConstructor().newInstance()`.

### 15.3 plugin.json location

```
my-plugin/
  src/main/resources/plugin.json   // Gradle copies to build/resources/main/plugin.json
  build/libs/my-plugin-1.0.0.jar  // jar contains plugin.json at root (not in BOOT-INF)
```

Verify:

```bash
jar tf build/libs/my-plugin-1.0.0.jar | grep plugin.json
# plugin.json
unzip -l build/libs/my-plugin-1.0.0.jar
```

If `plugin.json` is missing, `PluginManager` logs `skipping X — no plugin.json at jar root` and ignores the jar.

---

## 16. Installing, Logs, Debugging

### 16.1 Installing

1. Copy jar to `plugins/` (or `config [plugins] directory` value).
2. Restart proxy (`./gradlew :launcher:run` or `java -jar candiriya.jar`).
3. Look for:

```
INFO  PluginManager - scanning 1 jars in /.../plugins
INFO  PluginManager - loaded plugin hello 1.0.0 from hello-plugin-1.0.0.jar
INFO  PluginManager - enabling 1 plugins
INFO  candiriya-plugin-hello - Hello from Hello v1.0.0!
INFO  PluginManager - enabled plugin hello 1.0.0
INFO  Candiriya RUNNING on 0.0.0.0:25577
```

### 16.2 Logs

- Each plugin gets `Logger getLogger("candiriya-plugin-<id>")` via `ctx.logger`. Its output goes through `log4j2` same as proxy (console + `logs/candiriya.log` shaped by `[logging] level`).
- Core `PluginManager` logs at `INFO` for load/enable/disable, `ERROR` for timeouts/duplicate ids/missing deps.
- Chatty `debug` for broadcasts/messaging goes to `org.apache.logging.log4j.LogManager.getLogger("candiriya-messaging")` — enable with `level=DEBUG` or `<Logger name="candiriya-messaging" level="debug"/>` in `log4j2.xml`.

### 16.3 /candiriya plugins

```
> candiriya plugins
Plugins (1):
- HelloPlugin (hello) v1.0.0 [ENABLED]

> candiriya plugins (failed dep)
Plugins (2):
- Database (database) v1.0 [ENABLED]
- ChatFilter (chatfilter) v2.3.1 [FAILED]
```

Permission: `candiriya.command.plugins` (default `op` group + console).

### 16.4 Debugging checklist

- **Jar not loaded:** check `plugins/` path, ensure file ends `.jar`, contains `plugin.json` at root, logs for `skipping`.
- **Main class not found:** `main` must be fully qualified, jar must contain that class, no `isolated:false` hiding it. Check `java -cp my.jar com.example.MyPlugin` manually.
- **NoSuchMethodException:** plugin class needs public no-arg constructor (`class Foo : Plugin` Kotlin gives it by default; Java must have `public Foo() {}`).
- **ClassCastException `cannot be cast to Plugin`:** your `main` doesn't implement `kz.bejiihiu.candiriya.plugin.Plugin` — import the right `Plugin`, not `org.bukkit.plugin.Plugin`.
- **`onEnable` not called:** look for `plugin X timed out (10s)` — you blocked without using scheduler.
- **Commands not showing:** check `ownedAliases()` after `register`; verify you didn't typo alias that already exists (core throws `alias already registered`).
- **Events not firing:** did you `register` with correct `pluginId`? Using `ctx.events.fire` from core's `ProxyInitializeEvent` only fires after all enables — listeners registered in `onEnable` will catch it.

---

## 17. Velocity Bridge — Running Velocity Plugins Inside Candiriya

You asked: *"good api so I can write a plugin that in the future can run Velocity plugins"*. The API is designed for exactly that — you don't need core to vendor Velocity. Write **one** Candiriya plugin that acts as a **Velocity host**.

### 17.1 Why it works

- `PluginManager` never references `com.velocitypowered.api.*`. It's just `URLClassLoader + Plugin` reflection.
- You can add `velocity-api:3.4.0` as `compileOnly` to *your* plugin, embed Velocity's `PluginManager` (or copy its ~500 LOC) inside your jar, and delegate.

### 17.2 Sketch

Full sketch in `examples/velocity-bridge/src/main/kotlin/com/example/bridge/VelocityBridgePlugin.kt`:

```kotlin
class VelocityBridgePlugin : Plugin {
    override fun onEnable(ctx: PluginContext) {
        val dir = ctx.dataDirectory.resolve("velocity-plugins")
        Files.createDirectories(dir)
        // load each velocity jar with its own child-first loader parented to *your* loader
        for (jar in Files.list(dir).filter { it.toString().endsWith(".jar") }) {
            val velocityDesc = readVelocityPluginJson(jar) // com.velocitypowered.api.plugin.PluginDescription
            val cl = PluginClassLoader(arrayOf(jar.toUri().toURL()), this::class.java.classLoader)
            val instance = cl.loadClass(velocityDesc.main).getDeclaredConstructor().newInstance()
            // adapt Candiriya events to Velocity events
            ctx.events.on(ctx.description.id, PlayerJoinEvent::class.java) { e ->
                velocityEventManager.fire(PostLoginEvent(adaptPlayer(e.player)))
            }
            // adapt Candiriya commands to Velocity commands
            // ...
        }
    }
}
```

**Key seams to adapt (all available via `ctx`):**

- `ctx.server.getPlayers()` → `ProxyServer.getAllPlayers()` (Velocity)
- `ctx.server.getServer("default")` → `ProxyServer.getServer("...")`
- `ctx.events.register/on/fire` → `VelocityEventManager`
- `ctx.commands.register` → `CommandManager.register` (Velocity's Brigadier)
- `ctx.scheduler` → `Scheduler` (wrap `PluginScheduler` as Velocity's `Scheduler`)

You own the mapping — core stays agnostic. Start with one Velocity plugin you care about, adapt its events/commands manually, then generalize.

### 17.3 What not to do

- Don't try to put Velocity jars directly into `plugins/` and expect Candiriya to load them — Candiriya only understands `plugin.json`, not `velocity-plugin.json`. They must go into `plugins/velocity-bridge/velocity-plugins/` and be loaded by your bridge plugin.

---

## 18. Best Practices & Pitfalls

### Do

- **Stay on `ctx.scheduler` for I/O.** Database, HTTP, file reads — always via `ctx.scheduler.execute/delayed/async`. Virtual threads make blocking cheap, but only on your plugin thread, not network.
- **Hop to `player.execute {}` before touching player.** Player's `context` is the only thread that may mutate its `state/server` safely.
- **Validate `plugin.json` locally.** Run a `PluginDescription.parse(Files.readString(Path.of("src/main/resources/plugin.json")))` in a unit test.
- **Shade deps under your namespace.** If you need `okhttp`, relocate to `com.example.myplugin.shaded.okhttp` via ShadowJar to avoid colliding with another plugin's version (isolated loaders help, but shading is still safer).
- **Log with `ctx.logger`.** Not `println` or `LogManager.getLogger(MyClass::class.java)` (that logger won't carry plugin prefix).
- **Depend explicitly.** Use `"depends": ["database"]` rather than relying on filesystem scan order (which is unspecified).

### Don't

- **Don't block `onEnable`.** `onEnable` has 10s timeout — doing `Thread.sleep(15000)` marks you FAILED.
- **Don't call `Thread.sleep` on network/tick threads.** Only inside `ctx.scheduler` tasks.
- **Don't use `synchronized` with virtual threads** (carrier pinning). Use `ReentrantLock` / `ConcurrentHashMap` / `AtomicReference`.
- **Don't store `PluginContext` beyond `onDisable`.** Its `server/playerManager` references become stale after close.
- **Don't use `isolated:false` for one-off plugins.** Keep isolation; only suite plugins need shared loader.
- **Don't hardcode `plugins/` path.** Use `ctx.dataDirectory`.

---

## 19. API Stability

- `apiVersion: "1"` — current. Plugins declaring `apiVersion=1` load. If we bump to `2` with breaking changes, `1` plugins will log a warning but still load until we decide to enforce (TODO: add `apiVersion` check in `PluginManager`).
- `plugin-api` uses `explicitApi()` — every `public` declaration is intentional. We will not break `public` without major version bump.
- `plugin-loader` is **internal** — don't compile against it from plugins. Only `plugin-api`.

Changelog for future `apiVersion 2` candidates (not yet): multi-backend `getServers()`, real chat packets, `PluginConfig` helper, `apiVersion` enforcement.

---

## 20. FAQ

**Q: Can I use Java instead of Kotlin?**  
Yes — `Plugin` is a vanilla JVM interface. Java plugins compile and load identically. See §15.2. Use Kotlin only for the build file if you like.

**Q: Can I hot-reload with `/candiriya reload`?**  
No — `reload` currently only reloads `permissions.toml`. Plugins require restart (close ClassLoader + executor). Hot-reload causes `LinkageError` and resource leaks in Netty.

**Q: How many plugins can I load?**  
No hard limit. Each `isolated:true` plugin costs ~1 ClassLoader + 1 virtual thread. Dozens are fine. Hundreds — consider `isolated:false` suite to share loader.

**Q: Does my plugin block the proxy tick?**  
No if you use `ctx.scheduler`. `repeating(50ms)` on `PluginScheduler` runs on the scheduled pool but hops to your thread — tick (`TickScheduler`) never sees it.

**Q: Can two plugins share a library like `kotlinx-serialization`?**  
Yes — it's `DEFAULT_SHARED`, parent-first, so one copy from the proxy jar is shared. Don't shade it into your plugin jar; mark `compileOnly`.

**Q: Player `sendMessage` does nothing?**  
Known stub — chat delivery via `minecraft:plugin_message` packet is not wired yet in `ProxyServerImpl.PlayerAdapter`. It logs at DEBUG. `disconnect` works. Track `protocol` module for full chat.

**Q: How do I debug ClassLoader issues?**  
Run with `-Dlog4j.configurationFile=...` and set `level=DEBUG`. `PluginManager` logs `loaded plugin ... from ...` and `sharedLoader` actions. To inspect isolation: `ctx.logger.info("loaded from {}", javaClass.classLoader); ctx.logger.info("plugin api loader {}", Plugin::class.java.classLoader)`.

---

## 21. Reference — All Interfaces at a Glance

```kotlin
// plugin-api/src/main/kotlin/kz/bejiihiu/candiriya/plugin/

interface Plugin { fun onLoad(); fun onEnable(ctx: PluginContext); fun onDisable() }

data class PluginDescription(
    val id: String, val name: String, val version: String, val main: String,
    val apiVersion: String, val depends: List<String>, val isolated: Boolean,
    val sharedLibraries: List<String>, val description: String?, val authors: List<String>
) { companion object { fun parse(json: String): PluginDescription; fun parse(bytes: ByteArray) } }

interface PluginContext {
    val description: PluginDescription
    val logger: Logger
    val dataDirectory: Path
    val server: ProxyServer
    val events: EventBus
    val scheduler: PluginScheduler
    val commands: PluginCommandManager
    val permissions: PermissionRegistry
    val messaging: PluginMessaging
}

interface ProxyServer {
    val config: ProxyConfig; val version: String
    fun getPlayers(): Collection<ProxyPlayer>
    fun getPlayer(uuid: UUID): Optional<ProxyPlayer>
    fun getPlayer(name: String): Optional<ProxyPlayer>
    fun getPlayerCount(): Int
    fun getServers(): Map<String, RegisteredBackend>
    fun getServer(name: String): Optional<RegisteredBackend>
    fun broadcast(Component); fun isOnlineMode(): Boolean
}
interface ProxyPlayer { val uuid: UUID; val username: String; ... fun execute(Runnable) }
interface RegisteredBackend { val name: String; val host: String; val port: Int; fun sendPlayer(ProxyPlayer): Boolean }

interface Event; interface Cancellable : Event { var isCancelled: Boolean }
enum class EventPriority { LOWEST, LOW, NORMAL, HIGH, HIGHEST }
annotation class Subscribe(val priority: EventPriority = NORMAL)
interface EventBus {
    fun register(pluginId: String, listener: Any)
    fun <T: Event> on(pluginId: String, eventClass: Class<T>, priority: EventPriority, handler: (T)->Unit): AutoCloseable
    fun unregisterAll(pluginId: String)
    fun <T: Event> fire(event: T): T
    fun <T: Event> fireAsync(event: T): CompletableFuture<T>
}
// built-ins: ProxyInitializeEvent, ProxyShutdownEvent, PlayerJoinEvent, PlayerDisconnectEvent, PlayerChooseBackendEvent, PluginMessageEvent

interface PluginScheduler {
    val pluginId: String
    fun execute(Runnable): PluginTask
    fun delayed(Duration, Runnable): PluginTask
    fun repeating(Duration, Duration, Runnable): PluginTask
    fun <T> async(Callable<T>): CompletableFuture<T>
    fun cancelAll(): Int
}
interface PluginTask { fun cancel(): Boolean; val isCancelled: Boolean; val isDone: Boolean }

interface PluginCommandManager {
    fun register(alias: String, command: PluginCommand, vararg extraAliases: String)
    fun unregister(alias: String): Boolean
    fun ownedAliases(): Set<String>
}
interface PluginCommand { val permission: String?; val description: String; val usage: String; fun execute(source: PluginCommandSource, args: Array<String>); fun suggest(...): List<String> }
interface PluginCommandSource { val name: String; val isConsole: Boolean; fun hasPermission(String): Boolean; fun sendMessage(Component); fun asPlayer(): ProxyPlayer? }

interface PermissionRegistry { fun has(player: ProxyPlayer, permission: String): Boolean; fun has(source: PluginCommandSource, permission: String): Boolean }

interface PluginMessaging {
    fun registerChannel(String): Boolean; fun unregisterChannel(String): Boolean
    fun send(ProxyPlayer, String, ByteArray): Boolean; fun channels(): Set<String>
}
```

Config (`candiriya.toml`):

```toml
[plugins]
directory = "plugins"
enableTimeoutMs = 10000
disableTimeoutMs = 5000
```

That's it — happy plugging. If something is unclear, ping `examples/hello-plugin/` and `PluginManager.kt:38` for the ground truth.
