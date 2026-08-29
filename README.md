# Candyriya

[![CI](https://img.shields.io/github/actions/workflow/status/bejiihiu/Candyriya/ci.yml?branch=dev&label=CI)](https://github.com/bejiihiu/Candyriya/actions/workflows/ci.yml)
[![Build](https://img.shields.io/github/actions/workflow/status/bejiihiu/Candyriya/build.yml?branch=dev&label=build)](https://github.com/bejiihiu/Candyriya/actions/workflows/build.yml)
[![Java 21](https://img.shields.io/badge/java-21-blue.svg)](https://adoptium.net/)
[![Kotlin 2.4](https://img.shields.io/badge/kotlin-2.4-purple.svg)](https://kotlinlang.org/)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Discord](https://img.shields.io/discord/1412345678901234567?label=discord&logo=discord)](https://discord.gg/yJk5qdR7wn)

The modern, next-generation Minecraft server proxy — multithreaded, Velocity/BungeeCord-inspired, built from the ground up for scalability and flexibility.

> **Status:** Phase 0 Foundation. Core lifecycle, networking, and protocol scaffolding are in place. Not yet production-ready — follow `dev` for active development.

## Goals

- **Codebase you can dive into** — consistently follows best practices for Kotlin/Java projects, easy to navigate and extend.
- **High performance** — handle thousands of players on one proxy; Netty + virtual threads (Java 21) + Folia-style tick scheduler.
- **Fresh API, no baggage** — built from scratch to avoid design mistakes of older proxies, with a clean command/permission system.
- **First-class interop** — Velocity Modern forwarding, BungeeGuard/Legacy support planned; Paper/Spigot backends.

Inspired by [PaperMC/Velocity](https://github.com/PaperMC/Velocity) and [SpigotMC/BungeeCord](https://github.com/SpigotMC/BungeeCord) — big respect <3

## Building

Candyriya is built with [Gradle](https://gradle.org/). Use the wrapper (`./gradlew`) — CI does.

```bash
./gradlew build          # full build
./gradlew check --parallel  # all quality gates
```

Quality gates (all must pass):

- **ktlint** (Android style, `android=true`)
- **Checkstyle** (`google_checks.xml`)
- **SpotBugs** (`MAX` / `LOW`)
- **Checker Framework** (nullness)
- **JUnit 5** + **ArchUnit** (where applicable)

## Running

Build the fat jar and run:

```bash
./gradlew :launcher:shadowJar
java -jar launcher/build/libs/candyriya.jar --config ./candyriya.toml
```

Or run directly via Gradle (creates `candyriya.toml` with defaults on first run):

```bash
./gradlew :launcher:run
# binds to 0.0.0.0:25577, logs to console + logs/candyriya.log
# Ctrl+C -> STOPPING -> STOPPED (within 5s)
```

The proxy will generate `candyriya.toml` from `config/src/main/resources/candyriya.default.toml` if missing.

## Configuration

`candyriya.toml` (annotated defaults):

```toml
[network]
bind = "0.0.0.0:25577"
workers = 0              # 0 = 2 * cpu count
readTimeoutSeconds = 30

[protocol]
maxPacketSize = 2097152
compressionThreshold = 256

[backend]
host = "127.0.0.1"
port = 25565
connectTimeoutMs = 5000
retryAttempts = 0
retryDelayMs = 500

[security]
onlineMode = false
forwardingSecret = ""
forwardingMode = "NONE"  # NONE / LEGACY / BUNGEEGUARD / MODERN

[status]
motd = "<gradient:#55FF55:#55FFFF>Candyriya 26.1</gradient> <gray>—</gray> <white>proxy</white>"
maxPlayers = 100
versionName = "26.1"
versionProtocol = 775

[threads]
virtual = true
scheduledCoreSize = 2
asyncParallelism = 0

[scheduler]
tickRateMs = 50          # 50ms = 20 tps, like Paper/Folia
contexts = 4

[logging]
level = "INFO"
```

See [`docs/CONFIG.md`](docs/CONFIG.md) for full reference.

## Plugins

Candyriya has its own plugin API (not Velocity). See **[PLUGINS.md](./PLUGINS.md)** for the full 20-section guide, plus:
- [`examples/hello-plugin/`](./examples/hello-plugin) — minimal Kotlin plugin (copy-paste starter)
- [`examples/velocity-bridge/`](./examples/velocity-bridge) — sketch showing how one Candyriya plugin can host Velocity jars

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

Build jar with `plugin.json` at root, drop into `plugins/`, restart proxy. `/candyriya plugins` lists it.  
Hybrid ClassLoader (`isolated:true` default, `false` for suites) + per-plugin virtual thread via `ctx.scheduler`. No hot-reload — restart to update jars.

## Modules

| Module | Responsibility |
|---|---|
| `core` | Lifecycle `STARTING → RUNNING → STOPPING → STOPPED` (`AtomicReference`) + `PluginManager` wiring |
| `config` | TOML via `night-config`, validation, `candyriya.default.toml` (`[plugins] directory = "plugins"`) |
| `plugin-api` | Public contracts (`Plugin`, `PluginContext`, `EventBus`, `PluginScheduler`, `ProxyServer`) |
| `plugin-loader` | Private hybrid `PluginClassLoader` + `PluginContainer` per-plugin thread |
| `network` | Netty bootstrap, pipeline, backend connection queue/backpressure |
| `protocol` | Codec registry, VarInt, packet codecs, encryption/compression |
| `permissions` | Wildcard + group inheritance, `Tristate` |
| `command` | Brigadier-style manager, `candyriya` + `server`/`glist`/`send` builtins (now plugin-aware) |
| `scheduler` | Virtual threads, `ThreadController`, `TickScheduler` (Folia-like) |
| `launcher` | `Main.kt` + Log4j2 async + shadow jar |

```
core → config → network → protocol → scheduler → permissions → command → launcher
```

## Requirements

- **Java 21** (Temurin/Zulu recommended)
- **Kotlin 2.4.0** (via Gradle)
- No extra setup — `./gradlew build` is enough

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md). TL;DR:

```bash
./gradlew check --parallel   # must pass before PR
```

- Branches: `feature/*`, `fix/*`
- Commits: [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `chore:`)
- Style: Kotlin = Android Kotlin Style Guide, Java = Google Java Style

Join us on [Discord](https://discord.gg/yJk5qdR7wn) for help.

## License

Licensed under **GPL-3.0** — see [LICENSE](LICENSE).

This project is not affiliated with Mojang, PaperMC, or SpigotMC.
