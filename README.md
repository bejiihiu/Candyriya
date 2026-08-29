# Candiriya

[![CI](https://github.com/bejiihiu/Candyriya/actions/workflows/ci.yml/badge.svg)](https://github.com/bejiihiu/Candyriya/actions/workflows/ci.yml)

Multithreaded Minecraft proxy (Velocity/BungeeCord-inspired) — Phase 0 Foundation.

## Quick start

```bash
./gradlew :launcher:run
# creates ./candiriya.toml with defaults on first run
# binds to 0.0.0.0:25577, logs to console + logs/candiriya.log
# Ctrl+C -> STOPPING -> STOPPED
```

Fat jar:

```bash
./gradlew :launcher:shadowJar
java -jar launcher/build/libs/candiriya.jar --config ./candiriya.toml
```

## Config

`candiriya.toml` (generated from `config/src/main/resources/candiriya.default.toml`):

```toml
[network]
bind = "0.0.0.0:25577"
workers = 0

[shutdown]
quietPeriodMs = 200
timeoutMs = 5000

[logging]
level = "INFO"
```

## Plugins

Candiriya has its own plugin API (not Velocity). See **[PLUGINS.md](./PLUGINS.md)** for the full 20-section guide, plus:
- [`examples/hello-plugin/`](./examples/hello-plugin) — minimal Kotlin plugin (copy-paste starter)
- [`examples/velocity-bridge/`](./examples/velocity-bridge) — sketch showing how one Candiriya plugin can host Velocity jars

Quick peek:

```json
// plugin.json at jar root
{ "id": "hello", "name": "Hello", "version": "1.0.0", "main": "com.example.HelloPlugin" }
```

```kotlin
class HelloPlugin : Plugin {
  override fun onEnable(ctx: PluginContext) {
    ctx.logger.info("hi {}", ctx.description.id)
    ctx.commands.register("hello", MyCommand())
    ctx.scheduler.delayed(Duration.ofSeconds(2)) { ctx.server.broadcast(Component.text("hi")) }
  }
}
```

Build jar with `plugin.json` at root, drop into `plugins/`, restart proxy. `/candiriya plugins` lists it.  
Hybrid ClassLoader (`isolated:true` default, `false` for suites) + per-plugin virtual thread via `ctx.scheduler`. No hot-reload — restart to update jars.

## Modules

- `core` lifecycle `STARTING→RUNNING→STOPPING→STOPPED` + `PluginManager` wiring
- `config` TOML via night-config (`[plugins] directory = "plugins"`)
- `plugin-api` public contracts (`Plugin`, `PluginContext`, `EventBus`, `PluginScheduler`, `ProxyServer`)
- `plugin-loader` private hybrid `PluginClassLoader` + `PluginContainer` per-plugin thread
- `network` Netty bootstrap + `Player` context threading
- `protocol` codec registry stub
- `launcher` main + Log4j2 async + shadow jar
- `permissions` groups / `candiriya.*` wildcards
- `command` `/candiriya` + `/server` + `/glist` + `/send` (now plugin-aware)
- `scheduler` virtual threads + tick (`50ms`) + context registry

## Check

```bash
./gradlew check --parallel
```

Includes: ktlint (android), Checkstyle (google_checks.xml), SpotBugs (MAX/LOW), Checker Framework (nullness), JUnit5.

## Requirements

Java 21, Kotlin 2.4.0
