# Configuration

`candyriya.toml` is generated on first run from `config/src/main/resources/candyriya.default.toml`.

## Example

```toml
[network]
bind = "0.0.0.0:25577"
workers = 0
readTimeoutSeconds = 30

[protocol]
maxPacketSize = 2097152
compressionThreshold = 256

[backend]
host = "127.0.0.1"
port = 25565
connectTimeoutMs = 5000
retryAttempts = 0
retryDelayMs = 500

[security]
onlineMode = false
forwardingSecret = ""
forwardingMode = "NONE"

[status]
motd = "<gradient:#55FF55:#55FFFF>Candyriya 26.1</gradient> <gray>—</gray> <white>proxy</white>"
maxPlayers = 100
versionName = "26.1"
versionProtocol = 775

[threads]
virtual = true
scheduledCoreSize = 2
asyncParallelism = 0

[scheduler]
tickRateMs = 50
contexts = 4

[logging]
level = "INFO"

[shutdown]
quietPeriodMs = 200
timeoutMs = 5000
```

## Sections

### `network`

| key | type | default | description |
|---|---|---|---|
| `bind` | String `host:port` | `0.0.0.0:25577` | Address to bind. Port `1..65535` required. |
| `workers` | int `>=0` | `0` | Netty worker threads. `0` = `2 * cpu count`. |
| `readTimeoutSeconds` | int `>=0` | `30` | Read timeout. `0` = disabled. |

### `protocol`

| key | type | default | description |
|---|---|---|---|
| `maxPacketSize` | int `1..8388608` | `2097152` | Max packet size before kick. |
| `compressionThreshold` | int | `256` | ` -1` to disable, `256` is Velocity default. |

### `backend`

Single backend for now. Multi-backend `Map<String, BackendConfig>` planned (like Velocity `[servers]`).

| key | type | default | description |
|---|---|---|---|
| `host` | String | `127.0.0.1` | Backend host. |
| `port` | int | `25565` | Backend port. |
| `connectTimeoutMs` | int `100..60000` | `5000` | Connect timeout. |
| `retryAttempts` | int `0..10` | `0` | Retries on failure. `0` = kick immediately. |
| `retryDelayMs` | long `0..10000` | `500` | Delay between retries. |

### `security`

| key | type | default | description |
|---|---|---|---|
| `onlineMode` | bool | `false` | Mojang auth. |
| `forwardingSecret` | String | `""` | Secret for `BUNGEEGUARD`/`MODERN`. |
| `forwardingMode` | enum | `NONE` | `NONE` / `LEGACY` / `BUNGEEGUARD` / `MODERN`. Modern = Velocity HMAC. |

### `status`

MOTD supports [MiniMessage](https://docs.advntr.dev/minimessage/format.html) (`<gradient>`, `<gray>` etc.).

| key | type | default |
|---|---|---|
| `motd` | String | `"<gradient:#55FF55:#55FFFF>Candyriya 26.1</gradient> ..."` |
| `maxPlayers` | int `>=0` | `100` |
| `versionName` | String | `26.1` |
| `versionProtocol` | int | `775` |

### `threads`

| key | type | default | description |
|---|---|---|---|
| `virtual` | bool | `true` | Use virtual threads for async pool (Java 21+). |
| `scheduledCoreSize` | int `>=1` | `2` | Core size for scheduled pool (platform threads). |
| `asyncParallelism` | int `>=0` | `0` | Parallelism when `virtual=false`. `0` = cpu count. |

### `scheduler`

| key | type | default | description |
|---|---|---|---|
| `tickRateMs` | long `10..1000` | `50` | Tick duration. `50` = 20 tps (Paper/Folia). |
| `contexts` | int `0..32` | `4` | Execution contexts (like Folia regions). `0` = cpu count. |

### `logging` / `shutdown`

Same as `network`.

## Env / CLI

```bash
java -jar candyriya.jar --config ./candyriya.toml
java -jar candyriya.jar --config=/etc/candyriya.toml
```

If `candyriya.toml` missing, default is copied from classpath resource.
