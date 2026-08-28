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

## Modules

- `core` lifecycle `STARTING→RUNNING→STOPPING→STOPPED`
- `config` TOML via night-config
- `network` Netty bootstrap (no-op handler)
- `protocol` codec registry stub
- `launcher` main + Log4j2 async + shadow jar

## Check

```bash
./gradlew check --parallel
```

Includes: ktlint (android), Checkstyle (google_checks.xml), SpotBugs (MAX/LOW), Checker Framework (nullness), JUnit5.

## Requirements

Java 21, Kotlin 2.4.0
