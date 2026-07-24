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

### Comment style

All patches must follow this exact format:

```java
// Candyriya start - short description [issue-ref]
...modified lines...
// Candyriya end
```

**Examples:**

Fixing a bug:
```java
// Candyriya start - fix crash on modded entity spawn [Candyriya#42]
if (entity instanceof ModEntity mod) {
    return mod.getCustomData();
}
// Candyriya end
```

Adding a feature:
```java
// Candyriya start - add custom config option for tick rate [Candyriya#67]
int tickRate = CandyriyaConfig.get().tickRate;
// Candyriya end
```

Branding:
```java
// Candyriya start - brand
return kz.bejiihiu.candyriya.Brand.NAME;
// Candyriya end
```

### Issue references

When fixing a bug from Arclight or adding a Candyriya-specific feature, always reference the issue:

- **Arclight issues**: `// Candyriya start - fix xyz [Arclight#1467]`
- **Candyriya issues**: `// Candyriya start - fix xyz [Candyriya#42]`
- **No issue**: `// Candyriya start - fix xyz` (only if truly standalone)

For Javadoc, use `@see` links:
```java
/**
 * Returns the NMS Item for a given Bukkit Material, including modded materials.
 * Used as a fallback when CraftMagicNumbers.getItem() returns null for mod items.
 * @see <a href="https://github.com/IzzelAliz/Arclight/issues/1467">Arclight#1467</a>
 */
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

## Branching workflow

**Work strictly in `v1.21.1`.**

For fixes and features, always create a branch or worktree, then merge back:

```bash
# Create a branch for the fix/feature
git checkout v1.21.1
git checkout -b fix/some-bug

# ... do work ...

# Commit with [ci beta] or [ci release]
git commit -m "fix: description [Candyriya#42] [ci beta]"

# Merge back to v1.21.1
git checkout v1.21.1
git merge fix/some-bug
git push origin v1.21.1

# Clean up
git branch -d fix/some-bug
```

**Naming conventions for branches:**

- `fix/description` — bug fixes
- `feat/description` — new features
- `chore/description` — maintenance, cleanup, refactoring

**After push to `v1.21.1`:**

- CI builds and creates a GitHub release automatically
- `auto-merge.yml` creates a PR to merge into `main`

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

## Tools for fixing bugs and finding mappings

JDK Path: `C:\Program Files\Java\jdk-21.0.11\`

### 1. javap — decompile .class files

```powershell
# Find Java 21 javap
& "C:\Program Files\Java\jdk-21.0.11\bin\javap.exe" -p -c "path\to\class.class"

# View method signatures only
javap -p "path\to\class.class"

# View full bytecode with line numbers
javap -p -c -l "path\to\class.class"
```

Where to find .class files:
- Extract from Minecraft jars in `.gradle\loom-cache\minecraftMaven\`
- Use `Add-Type -AssemblyName System.IO.Compression.FileSystem; [System.IO.Compression.ZipFile]::ExtractToDirectory()` to extract jars

### 2. mappings.dev — find method/field names across mapping namespaces

Website: https://mappings.dev

Shows mappings for:
- **Mojang** (official, used by NeoForge)
- **Searge** (SRG, used by Forge)
- **Yarn** (used by Fabric)
- **Intermediary** (used by Fabric)

Example: `https://mappings.dev/1.21.1/net/minecraft/server/ServerFunctionLibrary.html`

### 3. Firecrawl — web search and scraping

- `firecrawl_search` — search GitHub, forums, documentation
- `firecrawl_scrape` — extract content from specific URLs

Use to:
- Find similar fixes in other projects (Arclight, Forge, NeoForge, Fabric)
- Search for error messages and solutions
- Read documentation from mappings.dev, NeoForge docs, etc.

### 4. GitHub search — find existing fixes

Search patterns:
- `site:github.com "ClassName" mixin @Redirect`
- `site:github.com/IzzelAliz/Arclight "error message"`
- `site:github.com "methodName" mixin minecraft`

### 5. Gradle cache locations

```powershell
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
6. **Test compilation** — run `./gradlew :arclight-common:compileJava`

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

Use these tags in commit messages to control CI behavior:

- `[ci ignore]` — skip CI for this commit
- `[ci beta]` — build as beta release (prerelease)
- `[ci unstable]` — build as unstable release (prerelease)
- `[ci release]` — build as stable release

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
