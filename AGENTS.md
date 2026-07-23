# AGENTS.md

## Project

Candyriya — a Bukkit/Spigot/Paper server implementation running on Forge, NeoForge, and Fabric mod loaders. Fork of [Arclight](https://github.com/IzzelAliz/Arclight). Targets **Minecraft 1.21.1**, Java 21.

## Build commands

```bash
# full build (all platforms)
./gradlew cleanBuild build collect

# PR check (same as CI, without upload)
./gradlew cleanBuild build collect --no-daemon --stacktrace

# single subproject
./gradlew :candyriya-forge:build
./gradlew :candyriya-neoforge:build
./gradlew :candyriya-fabric:build

# run production server (per platform)
./gradlew :bootstrap:runProdForge
./gradlew :bootstrap:runProdNeoforge
./gradlew :bootstrap:runProdFabric
```

Build output jars land in `build/libs/` via the `collect` task (copies from `bootstrap:forgeJar`, `neoforgeJar`, `fabricJar`).

## Module layout

| Module | Role |
|---|---|
| `candyriya-common` | Shared code: Bukkit API bridging, Mixin processors, remapper, access wideners. All platform code depends on this. |
| `candyriya-forge` | Forge-specific mixins and adapters |
| `candyriya-neoforge` | NeoForge-specific mixins and adapters |
| `candyriya-fabric` | Fabric-specific mixins and adapters |
| `bootstrap` | Server launcher, fat-jar assembly (`forgeJar`/`neoforgeJar`/`fabricJar`), installer info generation, async catcher config |
| `installer` | Runtime installer libraries (embedded into bootstrap jar) |
| `i18n-config` | Internationalization config (HOCON-based) |
| `buildSrc` | Custom Gradle plugin (`CandyriyaGradlePlugin`) — handles Spigot build, mapping processing, jar remapping/relocation |

## Architecture notes

- **Architectury + Loom** — multi-loader build via `architectury-plugin` + `dev.architectury.loom`. Platform projects use `transformProduction<Platform>` configurations to consume common code.
- **Spigot reversion** — first build downloads Spigot BuildTools, builds Spigot, processes mappings, remaps the jar. This is cached in `~/.gradle` (controlled by `CandyriyaGradlePlugin`). If mappings are stale, delete the Spigot cache.
- **Mixin-heavy** — the project uses Sponge Mixin extensively. Mixin configs live in `src/main/resources/mixins.candyriya.*.json` per platform. Custom annotation processors in `candyriya-common/.../mod/mixins/` handle `@TransformAccess`, `@RenameInto`, `@LoadIfMod`, `@OnlyInPlatform`, etc.
- **Runtime remapping** — Bukkit plugin classes are remapped at runtime via `RemappingClassLoader` in `candyriya-common`. Mappings are generated per-platform (SRG for Forge, intermediary for Fabric, Mojang for NeoForge).
- **Access wideners** — defined in `candyriya-common/src/main/resources/candyriya.accesswidener`, plus `bukkit.at` (access transformer) and `extra_mapping.tsrg` for additional unmapping.
- **Base package** — `kz.bejiihiu.candyriya` (renamed from `io.izzel.arclight`). The buildSrc plugin class is still named `ArclightGradlePlugin` internally but imported as `CandyriyaGradlePlugin` in root `build.gradle`.

## Key versions

All pinned in `gradle/libs.versions.toml`:
- Minecraft 1.21.1, Forge 52.1.14, NeoForge 21.1.228, Fabric Loader 0.19.2
- Spigot reversion 4344, Bukkit API v1_21_R1
- Mixin 0.8.5, Lombok 1.18.38

## Gotchas

- `cleanBuild` must run before `build` — it clears `build/libs` first. CI always does `cleanBuild build collect`.
- The old AppVeyor config (`appveyor-19.yml`) builds twice due to a MixinGradle bug — if you hit weird incremental build failures, try `./gradlew clean` first.
- `runProd*` tasks use `run_prod/<platform>/` as working dir (override via `Candyriya_PROD_DIR` env var). They pass `nogui` and set 4G heap.
- No test suite exists — there are no `src/test` directories. Verification is build-only.
- Lombok is used project-wide (`compileOnly` + `annotationProcessor` in all subprojects).
- Platform source sets in `bootstrap` (`applaunch`, `forge`, `neoforge`, `fabric`) are separate from main — `applaunch` compiles to Java 8 for bootstrap compatibility.

## CI

- **gradle.yml** — runs on push to any branch: `cleanBuild build collect uploadFiles`
- **pr.yml** — runs on PRs: `cleanBuild build collect` + uploads artifact
- **release.yml** — runs on tag push (`v*`): builds and creates GitHub release

## Versioning

Candyriya uses **build IDs** instead of semantic versioning. The version is automatically determined by the number of commits in the repository (`git rev-list --count HEAD`).

For example:
- Commit #1 → version 1
- Commit #100 → version 100
- Commit #1234 → version 1234

This means every commit automatically increments the version number.

## Releases

Create releases by pushing tags:

```bash
git tag v123
git push origin v123
```

The release workflow will automatically build and create a GitHub release with the appropriate build ID.

### Commit message tags

Use these tags in commit messages to control CI behavior:

- `[ci ignore]` — skip CI for this commit
- `[ci beta]` — build as beta release
- `[ci unstable]` — build as unstable release
- `[ci release]` — build as stable release
