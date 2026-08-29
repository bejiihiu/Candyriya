package kz.bejiihiu.candyriya.plugin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Parsed `plugin.json` from jar root.
 *
 * Example `plugin.json`:
 * ```json
 * {
 *   "id": "myplugin",
 *   "name": "MyPlugin",
 *   "version": "1.0.0",
 *   "main": "com.example.MyPlugin",
 *   "apiVersion": "1",
 *   "depends": ["otherplugin"],
 *   "isolated": true,
 *   "sharedLibraries": ["kotlin.stdlib"]
 * }
 * ```
 *
 * Field rules mirror Velocity but intentionally simpler:
 * - [id] `^[a-z0-9_-]{3,32}$`
 * - [version] semver-ish `^[0-9A-Za-z._-]+$`
 * - [main] fully qualified class name
 * - [apiVersion] current is `"1"` — bump when we break API
 */
@Serializable
public data class PluginDescription(
    val id: String,
    val name: String,
    val version: String,
    val main: String,
    @SerialName("apiVersion")
    val apiVersion: String = "1",
    val depends: List<String> = emptyList(),
    val isolated: Boolean = true,
    val sharedLibraries: List<String> = emptyList(),
    val description: String? = null,
    val authors: List<String> = emptyList()
) {
    public companion object {
        private val ID_REGEX = Regex("^[a-z0-9_-]{3,32}$")
        private val VERSION_REGEX = Regex("^[0-9A-Za-z._\\-]{1,32}$")
        private val MAIN_REGEX = Regex("^[A-Za-z0-9_\\.]+$")
        private val JSON = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        /** Parse & validate from raw json string. Throws [IllegalArgumentException] on bad data. */
        public fun parse(json: String): PluginDescription {
            val desc: PluginDescription = JSON.decodeFromString(json)
            desc.validate()
            return desc
        }

        /** Parse from bytes (jar resource). */
        public fun parse(bytes: ByteArray): PluginDescription = parse(bytes.toString(Charsets.UTF_8))
    }

    public fun validate() {
        require(ID_REGEX.matches(id)) { "plugin id must match $ID_REGEX, got '$id'" }
        require(name.isNotBlank() && name.length <= 64) { "name must be 1..64, got '$name'" }
        require(VERSION_REGEX.matches(version)) { "version must match $VERSION_REGEX, got '$version'" }
        require(MAIN_REGEX.matches(main)) { "main must be qualified class name, got '$main'" }
        require(apiVersion.isNotBlank()) { "apiVersion required" }
        require(depends.size <= 16) { "too many depends: ${depends.size}" }
        for (dep in depends) {
            require(ID_REGEX.matches(dep)) { "depends entry must match $ID_REGEX, got '$dep'" }
            require(dep != id) { "plugin cannot depend on itself: $id" }
        }
        require(sharedLibraries.size <= 32) { "too many sharedLibraries" }
    }
}

