# Contributing to Candyriya

Thanks for taking the time to contribute! Your support is appreciated.

## Setting up a development environment

This isn't hard — clone the repo in your favorite IDE and have a backend test server (Paper/Spigot) ready behind the proxy.

```bash
git clone https://github.com/bejiihiu/Candyriya.git
cd Candyriya
./gradlew build
```

Requirements: **Java 21**, no extra setup. Gradle wrapper does the rest.

## Actually working on the code

It is strongly recommended you are familiar with:

- Minecraft protocol basics
- Java/Kotlin + Netty
- Libraries used: [Netty](https://netty.io), [Guava](https://github.com/google/guava), [night-config](https://github.com/TheElectronWill/night-config), [Kyori Adventure](https://docs.advntr.dev/), [Checker Framework](https://checkerframework.org/)

You can still work without deep knowledge, but it can be risky.

Candyriya follows:

- **Kotlin:** [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide) — enforced via `ktlint` with `android=true`
- **Java:** [Google Java Style](https://google.github.io/styleguide/javaguide.html) — enforced via Checkstyle `google_checks.xml`

The build will fail if any style issue is found. Run locally before pushing.

## Notes on the build

To reduce bugs and ensure quality, we run on every commit and PR:

- **ktlint** — Kotlin formatting
- **Checkstyle** — Java formatting (`config/checkstyle/google_checks.xml`)
- **SpotBugs** (`MAX` / `LOW`) — common errors; build fails on issue
- **Checker Framework** — nullness
- **JUnit 5** — unit tests

```bash
./gradlew check --parallel
# or single tasks:
./gradlew ktlintCheck
./gradlew checkstyleMain
./gradlew spotbugsMain
```

We also use **Spotless** for license headers (`HEADER.txt`). Run `./gradlew spotlessApply` if headers are missing.

## Project structure

```
core/          lifecycle state machine (AtomicReference)
config/        TOML loading/validation (night-config)
network/       Netty bootstrap, handlers, backend queue
protocol/      codec registry, VarInt, encryption, compression
permissions/   wildcard + group inheritance
command/       manager + builtins (candyriya, server, glist, send, shutdown)
scheduler/     ThreadController, TickScheduler, ContextRegistry (Folia-style)
launcher/      Main.kt, Log4j2 async, shadowJar
buildSrc/      convention plugins (candyriya.quality)
config/checkstyle/  google_checks.xml
```

## Running locally

```bash
./gradlew :launcher:run --args="--config ./candyriya.toml"
# or
java -jar launcher/build/libs/candyriya.jar --config ./candyriya.toml
```

The proxy binds to `0.0.0.0:25577` by default. `Ctrl+C` should print `STOPPING -> STOPPED` within 5s.

Useful commands in console:

```
help
candyriya info
candyriya plugins
server <name>
glist
send <player|all> <server>
shutdown
```

## Before PR

```bash
./gradlew check --parallel
```

Checklist:

- [ ] `./gradlew check` passes
- [ ] Tests added/updated if needed
- [ ] Docs updated (`README.md`, `docs/` if config changes)
- [ ] Commit messages follow Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`)
- [ ] Branch from `dev` — `feature/*` or `fix/*`
- [ ] No `candyriya` typos (it's `candyriya` :))

## Pull Requests

- Target `dev` (we merge `dev` → `main` on release)
- Keep PRs small and focused — one feature/fix per PR
- Describe **why**, not just what — link related issues
- CI must be green (GitHub Actions)

## Code style tips

- Prefer `val` over `var`, avoid `!!` — use Checker Framework nullness
- Keep Netty handlers small, testable
- Scheduler tasks via `Candyriya.getScheduler()` / `TickScheduler`, not raw threads
- Log via `LogManager.getLogger(...)`, respect `logging.level`

## Getting help

- [Discord](https://discord.gg/yJk5qdR7wn) — `#candyriya-help`
- Issues — use templates (Bug Report / Feature Request)
- Security — see `SECURITY.md`, don't open public issues for exploits

## License

By contributing, you agree your contributions are licensed under GPL-3.0 (see `LICENSE`).

