# AGENTS.md

## Project

Candyriya — a Bukkit/Spigot/Paper server implementation running on Forge, NeoForge, and Fabric mod loaders. Fork of [Arclight](https://github.com/IzzelAliz/Arclight). Targets **Minecraft 1.21.1**, Java 21.

## Patching Convention

**CRITICAL: Read this before making any changes.**

Candyriya is a **minimal fork** of Arclight. The strategy is:

1. **Keep `io.izzel.arclight` code intact** — do NOT rename packages or classes in the original Arclight codebase. This is required because `mixin-tools` annotation processor (`io.izzel.arclight:mixin-tools`) has hardcoded `io.izzel.arclight` package references. Renaming breaks the build.

2. **New code goes in `kz.bejiihiu.candyriya`** — brand info, Candyriya-specific features, utilities.

3. **Patches to Arclight code use this pattern:**

```java
// Candyriya start - short description of what this does
...modified lines...
// Candyriya end
```

4. **Call Brand.java for user-visible strings** — server name, logger names, crash reports, etc.

```java
// Candyriya start - brand
return kz.bejiihiu.candyriya.Brand.NAME;
// Candyriya end
```

### What is OK to change

- `settings.gradle` — `rootProject.name`
- `build.gradle` — `group`, `version`, manifest attributes, jar names
- `bootstrap/build.gradle` — jar `archiveBaseName`, manifest attributes
- User-visible strings (server name, logger names, crash reports) — use `Brand.java` or inline `"Candyriya"`
- `i18n-config/src/main/resources/META-INF/i18n/*.conf` — URLs and "Arclight" branded strings → replace with Candyriya
- `mods.toml`, `neoforge.mods.toml` — `displayName` and `description` (NOT `modId`)
- `fabric.mod.json` — `name`, `authors`, `description` (NOT `id`)
- `.github/workflows/*` — fully replace with our own
- `.github/ISSUE_TEMPLATE/*` — rebrand to Candyriya
- `README.md`, `AGENTS.md`, `LICENSE` — fully replace
- Files in `kz.bejiihiu.candyriya/` — this is our namespace
- Java code comments with upstream issue links — OK to keep as historical references

### What is NOT OK to change

- Package names in `io.izzel.arclight.*` — breaks mixin-tools AP
- Class names like `ArclightServer`, `ArclightConnector`, etc. — breaks mixin-tools AP
- Mixin JSON configs (`mixins.arclight.*.json`) — modId is `arclight`
- `mods.toml`, `neoforge.mods.toml` — modId is `arclight`
- `buildSrc/` — the `ArclightGradlePlugin` is hardcoded infrastructure
- `gradle/libs.versions.toml` library aliases (artifact module names stay as `io.izzel.arclight`)
- System property names like `arclight.version` — used in runtime code
- Directory names like `.arclight/` — used in bootstrap extraction

## Build commands

```bash
# full build (all platforms)
./gradlew cleanBuild build collect

# PR check (same as CI, without upload)
./gradlew cleanBuild build collect --no-daemon --stacktrace

# single subproject
./gradlew :arclight-forge:build
./gradlew :arclight-neoforge:build
./gradlew :arclight-fabric:build
```

**Java 21** is required for building.

Build output jars land in `build/libs/` via the `collect` task (copies from `bootstrap:forgeJar`, `neoforgeJar`, `fabricJar`).

## Module layout

| Module               | Role                                                                                                                      |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| `arclight-common`    | Shared code: Bukkit API bridging, Mixin processors, remapper, access wideners. All platform code depends on this.         |
| `arclight-forge`     | Forge-specific mixins and adapters                                                                                         |
| `arclight-neoforge`  | NeoForge-specific mixins and adapters                                                                                      |
| `arclight-fabric`    | Fabric-specific mixins and adapters                                                                                        |
| `bootstrap`          | Server launcher, fat-jar assembly (`forgeJar`/`neoforgeJar`/`fabricJar`), installer info generation                       |
| `installer`          | Runtime installer libraries (embedded into bootstrap jar)                                                                  |
| `i18n-config`        | Internationalization config (HOCON-based)                                                                                  |
| `buildSrc`           | Custom Gradle plugin (`ArclightGradlePlugin`) — handles Spigot build, mapping processing, jar remapping/relocation        |

Note: module directories remain `arclight-*` to preserve upstream compatibility.

## Architecture

- **Architectury + Loom** — multi-loader build via `architectury-plugin` + `dev.architectury.loom`
- **Spigot reversion** — first build downloads Spigot BuildTools, builds Spigot, processes mappings. Cached in `~/.gradle`.
- **Mixin-heavy** — Sponge Mixin extensively used. Mixin configs: `src/main/resources/mixins.arclight.*.json`
- **Runtime remapping** — Bukkit plugin classes remapped at runtime via `RemappingClassLoader`
- **Base package** — `io.izzel.arclight` (original, untouched). Brand/overlay code in `kz.bejiihiu.candyriya`
- **Build ID versioning** — version = `git rev-list --count HEAD`

## Key versions

All pinned in `gradle/libs.versions.toml`:

- Minecraft 1.21.1, Forge 52.1.14, NeoForge 21.1.228, Fabric Loader 0.19.2
- Spigot reversion 4344, Bukkit API v1_21_R1
- Mixin 0.8.5, Lombok 1.18.38

## CI

### Workflows

- **gradle.yml** — runs on push to version branches (`v*`): `cleanBuild build collect` + creates GitHub release
- **pr.yml** — runs on PRs: `cleanBuild build collect` + uploads artifact
- **release.yml** — runs on push to `main`: builds and creates GitHub release
- **auto-merge.yml** — runs on push to version branches (`v*`): auto-creates PR and merges to `main`
- **sync-upstream.yml** — runs weekly (Monday 12:00 UTC) + manual trigger: fetches upstream Arclight, creates PR if no conflicts, opens issue if conflicts

### Branch strategy

- **`v1.21.1`** — main development branch for Minecraft 1.21.1
- **`main`** — production branch. Auto-merges from `v1.21.1`. Releases created from here.

### Commit message tags

- `[ci ignore]` — skip CI build and auto-merge
- `[ci beta]` — mark release as beta (prerelease)
- `[ci unstable]` — mark release as unstable (prerelease)
- `[ci release]` — mark release as stable

## Upstream sync

To pull changes from upstream Arclight:

```bash
git fetch upstream master
git merge upstream/master
```

Conflicts will only appear in:
- Files you patched with `// Candyriya start/end` comments
- Workflow files (`.github/workflows/`)
- `settings.gradle`, `build.gradle`, `bootstrap/build.gradle`
- `README.md`, `AGENTS.md`

All original `io.izzel.arclight` code merges cleanly since we don't rename it.
