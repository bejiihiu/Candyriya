# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- `scheduler` — Folia-style `TickScheduler` + `ContextRegistry` + `ThreadController` (virtual threads)
- `permissions` — wildcard resolver, `Tristate`, group inheritance, `permissions.toml`
- `command` — `CommandManager` + builtins `candyriya`, `server`, `glist`, `send`, `shutdown` (Velocity-inspired)
- `player` — `PlayerManager`, `RegisteredServer`, `ExecutionContext`
- Backend connection proxy with queue, backpressure, retry, Velocity Modern forwarding
- Startup timing logs, i18n-ready startup locale fix

### Changed
- Renamed `candyriya` → `candyriya` everywhere (package `kz.bejiihiu.candyriya`, config `candyriya.toml`, jar `candyriya.jar`)
- `candyriya.default.toml` now includes `[backend]`, `[security]`, `[threads]`, `[scheduler]` sections

### Fixed
- Startup locale-independent timing
- Various SpotBugs/Checkstyle/ktlint issues

## [0.1.0] - 2026-08-29

### Added
- Phase 0 Foundation: `core` lifecycle `STARTING→RUNNING→STOPPING→STOPPED`, `config` (night-config TOML), `network` (Netty bootstrap), `protocol` (codec registry stub), `launcher` (Log4j2 async + shadow jar)
- CI `ci.yml` + `build.yml`, quality gates: ktlint (android), Checkstyle (google_checks), SpotBugs (MAX/LOW), Checker Framework

[Unreleased]: https://github.com/bejiihiu/Candyriya/compare/main...dev
[0.1.0]: https://github.com/bejiihiu/Candyriya/releases/tag/v0.1.0

