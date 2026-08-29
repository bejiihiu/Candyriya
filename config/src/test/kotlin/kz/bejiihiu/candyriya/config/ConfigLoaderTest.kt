package kz.bejiihiu.candyriya.config

import java.nio.file.Files
import java.nio.file.Path
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

public class ConfigLoaderTest {

    @Test
    public fun `default config is created when missing`(@TempDir tmp: Path) {
        val path = tmp.resolve("Candyriya.toml")
        assertThat(Files.exists(path)).isFalse()
        val config = ConfigLoader.load(path)
        assertThat(Files.exists(path)).isTrue()
        assertThat(config.network.bind).isEqualTo("0.0.0.0:25577")
        assertThat(config.shutdown.quietPeriodMs).isEqualTo(200)
    }

    @Test
    public fun `valid config loads correctly`(@TempDir tmp: Path) {
        val path = tmp.resolve("Candyriya.toml")
        Files.writeString(
            path,
            """
      [network]
      bind = "127.0.0.1:12345"
      workers = 2

      [shutdown]
      quietPeriodMs = 100
      timeoutMs = 1000

      [logging]
      level = "DEBUG"
            """.trimIndent()
        )
        val config = ConfigLoader.load(path)
        assertThat(config.network.bind).isEqualTo("127.0.0.1:12345")
        assertThat(config.network.workers).isEqualTo(2)
        assertThat(config.shutdown.quietPeriodMs).isEqualTo(100)
        assertThat(config.shutdown.timeoutMs).isEqualTo(1000)
        assertThat(config.logging.level).isEqualTo("DEBUG")
    }

    @Test
    public fun `invalid port fails`(@TempDir tmp: Path) {
        val path = tmp.resolve("Candyriya.toml")
        Files.writeString(
            path,
            """
      [network]
      bind = "0.0.0.0:99999"
            """.trimIndent()
        )
        assertThatThrownBy { ConfigLoader.load(path) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("port")
    }
}
