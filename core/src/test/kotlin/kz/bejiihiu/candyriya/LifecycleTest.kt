package kz.bejiihiu.candyriya

import kz.bejiihiu.candyriya.config.ProxyConfig
import kz.bejiihiu.candyriya.lifecycle.LifecycleState
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

public class LifecycleTest {

    @Test
    public fun `initial state is stopped`() {
        val c =
            Candyriya(
                ProxyConfig(
                    network = kz.bejiihiu.candyriya.config.NetworkConfig(bind = "127.0.0.1:0")
                )
            )
        assertThat(c.getState()).isEqualTo(LifecycleState.STOPPED)
    }

    @Test
    public fun `start transitions to running`() {
        val c =
            Candyriya(
                ProxyConfig(
                    network = kz.bejiihiu.candyriya.config.NetworkConfig(bind = "127.0.0.1:0")
                )
            )
        c.start()
        try {
            assertThat(c.getState()).isEqualTo(LifecycleState.RUNNING)
        } finally {
            c.stop()
        }
        assertThat(c.getState()).isEqualTo(LifecycleState.STOPPED)
    }

    @Test
    public fun `double start throws`() {
        val c =
            Candyriya(
                ProxyConfig(
                    network = kz.bejiihiu.candyriya.config.NetworkConfig(bind = "127.0.0.1:0")
                )
            )
        c.start()
        try {
            assertThatThrownBy { c.start() }
                .isInstanceOf(IllegalStateException::class.java)
        } finally {
            c.stop()
        }
    }

    @Test
    public fun `stop is idempotent`() {
        val c =
            Candyriya(
                ProxyConfig(
                    network = kz.bejiihiu.candyriya.config.NetworkConfig(bind = "127.0.0.1:0")
                )
            )
        c.start()
        c.stop()
        // second stop should not throw
        c.stop()
        assertThat(c.getState()).isEqualTo(LifecycleState.STOPPED)
    }
}
