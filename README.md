# Candyriya

A Bukkit server implementation on common mod loaders.

Fork of [Arclight](https://github.com/IzzelAliz/Arclight) with continued development and community-driven improvements.

[![Downloads count](https://img.shields.io/github/downloads/bejiihiu/Candyriya/total?style=flat-square)](https://github.com/bejiihiu/Candyriya/releases) ![License](https://img.shields.io/github/license/bejiihiu/Candyriya?style=flat-square)

## About

Candyriya is a fork of the Arclight project, bringing Bukkit/Spigot/Paper plugin support to modded Minecraft servers running on Forge, NeoForge, and Fabric.

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

## Support

- Report issues: [GitHub Issues](https://github.com/bejiihiu/Candyriya/issues)

## Contributing

We welcome contributions! Feel free to submit PRs.

When patching Arclight code, use the `// Candyriya start` / `// Candyriya end` comment pattern to mark changes.

## Acknowledgments

- Original [Arclight](https://github.com/IzzelAliz/Arclight) project by IzzelAliz and contributors
- All [Arclight contributors](https://github.com/IzzelAliz/Arclight/graphs/contributors)

## License

This project is licensed under [GPL v3](LICENSE) - see the original [Arclight license](https://github.com/IzzelAliz/Arclight/blob/master/LICENSE).
