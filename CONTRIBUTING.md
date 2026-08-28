# Contributing to Candiriya

## Build

```bash
./gradlew build
./gradlew run          # runs launcher with default ./candiriya.toml
./gradlew check        # all quality gates
./gradlew :launcher:shadowJar  # fat jar -> launcher/build/libs/candiriya.jar
```

Requirements: Java 21, no extra setup.

## Code style

- Kotlin: Android Kotlin Style Guide https://developer.android.com/kotlin/style-guide (enforced via ktlint `android=true`)
- Java: Google Java Style https://google.github.io/styleguide/javaguide.html (enforced via Checkstyle `google_checks.xml`)
- Run `./gradlew ktlintCheck checkstyleMain spotless?` Actually just `./gradlew check`.

## Before PR

```bash
./gradlew check --parallel
```

Branches: `feature/*`, `fix/*`. Commits: Conventional Commits (`feat:`, `fix:`, `chore:`).

## Project structure

- `core/` lifecycle state machine
- `config/` TOML loading/validation
- `network/` Netty bootstrap
- `protocol/` codec registry stub
- `launcher/` main + shadow jar + log4j2

## Running locally

```bash
./gradlew :launcher:run --args="--config ./candiriya.toml"
# or
java -jar launcher/build/libs/candiriya.jar --config ./candiriya.toml
```

Ctrl+C should print `STOPPING -> STOPPED` within 5s.
