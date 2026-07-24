# Candyriya

Community-first fork of [Arclight](https://github.com/IzzelAliz/Arclight) — fast bugfixes, open PRs, active maintenance.

[![Downloads count](https://img.shields.io/github/downloads/bejiihiu/Candyriya/total?style=flat-square)](https://github.com/bejiihiu/Candyriya/releases) ![License](https://img.shields.io/github/license/bejiihiu/Candyriya?style=flat-square)
[![bStats Servers](https://img.shields.io/bstats/servers/32849?style=flat-square&label=servers)](https://bstats.org/plugin/server-implementation/Candyriya/32849)
[![bStats Players](https://img.shields.io/bstats/players/32849?style=flat-square&label=players)](https://bstats.org/plugin/server-implementation/Candyriya/32849)

## About

Candyriya is a community-driven fork of [Arclight](https://github.com/IzzelAliz/Arclight) — a Bukkit/Spigot/Paper plugin support layer for modded Minecraft servers running on Forge, NeoForge, and Fabric.

The original Arclight project has been an incredible piece of work by IzzelAliz over many years. This fork exists to continue that legacy — providing quick bugfixes, accepting community PRs, and keeping things running. I have no intention of "replacing" Arclight or taking its community. I just want to make sure people who depend on this kind of server have a maintained option.

### What Candyriya is

- **Community-first** — PRs are reviewed and merged quickly. Open an issue, get a fix.
- **Fast iteration** — bugfixes ship as beta builds within hours, not weeks.
- **Transparent** — AI agents assist with development, but all code is reviewed and tested by a human before merge.
- **Backward-compatible** — same plugin API, same mod compatibility, same Arclight internals.

### What Candyriya is not

- Not a competing project — this is a continuation of Arclight's work.
- Not fully automated — AI helps with code, humans do the review and testing.

## Community

- **Telegram**: [devfolia.t.me](https://t.me/devfolia)
- **Issues**: [GitHub Issues](https://github.com/bejiihiu/Candyriya/issues)
- **Discussions**: [GitHub Discussions](https://github.com/bejiihiu/Candyriya/discussions)

## Download

Downloads are available at [GitHub Releases](https://github.com/bejiihiu/Candyriya/releases).

## Supported Versions

- **Minecraft 1.21.1** (primary target)
- Forge, NeoForge, and Fabric support

## Installing

- Download the jar from [Releases](https://github.com/bejiihiu/Candyriya/releases):
  - `candyriya-forge-1.21.1.jar`
  - `candyriya-neoforge-1.21.1.jar`
  - `candyriya-fabric-1.21.1.jar`
- Launch with command `java -jar candyriya-<loader>-1.21.1.jar nogui`
  - The `nogui` argument will disable the server control panel.

## Building from Source

**Java 21** is required.

```bash
git clone https://github.com/bejiihiu/Candyriya.git
cd Candyriya
./gradlew cleanBuild build collect
```

Build output jars land in `build/libs/`.

## Contributing

We welcome contributions! Feel free to submit PRs.

## Acknowledgments

- Original [Arclight](https://github.com/IzzelAliz/Arclight) project by IzzelAliz and contributors
- All [Arclight contributors](https://github.com/IzzelAliz/Arclight/graphs/contributors)

## License

This project is licensed under [GPL v3](LICENSE) - see the original [Arclight license](https://github.com/IzzelAliz/Arclight/blob/master/LICENSE).
