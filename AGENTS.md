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

**Java 21** is required for building. If your system Java is a different version, use Java 21 from Prism Launcher:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.11\"
./gradlew cleanBuild build collect
```

Build output jars land in `build/libs/` via the `collect` task (copies from `bootstrap:forgeJar`, `neoforgeJar`, `fabricJar`).

## Module layout

| Module               | Role                                                                                                                      |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| `candyriya-common`   | Shared code: Bukkit API bridging, Mixin processors, remapper, access wideners. All platform code depends on this.         |
| `candyriya-forge`    | Forge-specific mixins and adapters                                                                                        |
| `candyriya-neoforge` | NeoForge-specific mixins and adapters                                                                                     |
| `candyriya-fabric`   | Fabric-specific mixins and adapters                                                                                       |
| `bootstrap`          | Server launcher, fat-jar assembly (`forgeJar`/`neoforgeJar`/`fabricJar`), installer info generation, async catcher config |
| `installer`          | Runtime installer libraries (embedded into bootstrap jar)                                                                 |
| `i18n-config`        | Internationalization config (HOCON-based)                                                                                 |
| `buildSrc`           | Custom Gradle plugin (`CandyriyaGradlePlugin`) — handles Spigot build, mapping processing, jar remapping/relocation       |

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

### Workflows

- **gradle.yml** — runs on push to version branches (`v*`): `cleanBuild build collect uploadFiles`
- **pr.yml** — runs on PRs: `cleanBuild build collect` + uploads artifact
- **release.yml** — runs on push to `main`: builds and creates GitHub release
- **auto-merge.yml** — runs on push to version branches (`v*`): automatically creates PR and merges to `main`

### Branch strategy

- **`v1.21.1`** — main development branch for Minecraft 1.21.1. All development happens here.
- **`main`** — production branch. Automatically receives merges from `v1.21.1` via auto-merge workflow. Releases are created from this branch.

When adding support for new Minecraft versions, create a new branch (e.g., `v1.21.4`) and develop there.

### Commit message tags

Use these tags in commit messages to control CI behavior:

- `[ci ignore]` — skip CI build and auto-merge for this commit
- `[ci beta]` — mark release as beta (prerelease)
- `[ci unstable]` — mark release as unstable (prerelease)
- `[ci release]` — mark release as stable (default)

Examples:

```bash
git commit -m "fix: some bugfix [ci ignore]"  # won't trigger CI
git commit -m "feat: experimental feature [ci beta]"  # beta release
git commit -m "release: stable release [ci release]"  # stable release
```

## Versioning

Candyriya uses **build IDs** instead of semantic versioning. The version is automatically determined by the number of commits in the repository (`git rev-list --count HEAD`).

For example:

- Commit #1 → version 1
- Commit #100 → version 100
- Commit #1234 → version 1234

This means every commit automatically increments the version number.

## Releases

Releases are created automatically when code is merged to `main` branch. The auto-merge workflow handles this:

1. Push to `v1.21.1` → builds project
2. Auto-merge workflow creates PR to `main` and merges it
3. Push to `main` → creates GitHub release with build artifacts

Release type is determined by commit message tags:

- `[ci beta]` → prerelease (beta)
- `[ci unstable]` → prerelease (unstable)
- `[ci release]` or no tag → stable release

Build ID is automatically calculated from commit count (`git rev-list --count HEAD`).

## Tools for fixing bugs and finding mappings

When fixing issues from Arclight or debugging Minecraft-related problems, use these tools:

### 1. **javap** — decompile .class files

Use Java's built-in `javap` to inspect class structure, methods, and bytecode:

```powershell
# Find Java 21 javap (project uses Java 21 but javap works on any version)
& "C:\Program Files\Java\jdk-21.0.11\bin\javap.exe" -p -c "path\to\class.class"

# Java 21 from Prism Launcher
& "C:\Program Files\Java\jdk-21.0.11\bin\javap.exe" -p -c "path\to\class.class"

# View method signatures only
javap -p "path\to\class.class"

# View full bytecode with line numbers
javap -p -c -l "path\to\class.class"
```

**Where to find .class files:**

- Extract from Minecraft jars in `.gradle\loom-cache\minecraftMaven\`
- Use `Add-Type -AssemblyName System.IO.Compression.FileSystem; [System.IO.Compression.ZipFile]::ExtractToDirectory()` to extract jars

### 2. **mappings.dev** — find method/field names across mapping namespaces

Website: https://mappings.dev

Shows mappings for:

- **Mojang** (official, used by NeoForge)
- **Searge** (SRG, used by Forge)
- **Yarn** (used by Fabric)
- **Intermediary** (used by Fabric)

Example: `https://mappings.dev/1.21.1/net/minecraft/server/ServerFunctionLibrary.html`

### 3. **Firecrawl** — web search and scraping

```
firecrawl_search — search GitHub, forums, documentation
firecrawl_scrape — extract content from specific URLs
```

Use to:

- Find similar fixes in other projects (Arclight, Forge, NeoForge, Fabric)
- Search for error messages and solutions
- Read documentation from mappings.dev, NeoForge docs, etc.

### 4. **GitHub search** — find existing fixes

Search patterns:

- `site:github.com "ClassName" mixin @Redirect`
- `site:github.com/IzzelAliz/Arclight "error message"`
- `site:github.com "methodName" mixin minecraft`

### 5. **Gradle cache locations**

```
# Minecraft merged jars (Mojang mappings)
.gradle\loom-cache\minecraftMaven\net\minecraft\minecraft-merged-*\

# Forge merged jars (SRG mappings)
.gradle\loom-cache\minecraftMaven\net\minecraft\forge-*-minecraft-merged-*\

# Spigot BuildTools output
.gradle\Candyriya\candyriya_cache\buildtools\
```

### Common workflow for fixing issues:

1. **Read the issue** — understand the error message and stack trace
2. **Find the class** — use `javap` on extracted Minecraft jars to see method signatures
3. **Check mappings** — use mappings.dev to find Mojang/Searge/Yarn names
4. **Search for solutions** — use firecrawl to find similar fixes in other projects
5. **Write the mixin** — use correct method names from mappings
6. **Test compilation** — run `./gradlew :candyriya-common:compileJava`
