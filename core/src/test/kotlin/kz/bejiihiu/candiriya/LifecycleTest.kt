package kz.bejiihiu.candiriya

import kz.bejiihiu.candiriya.config.ProxyConfig
import kz.bejiihiu.candiriya.lifecycle.LifecycleState
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

public class LifecycleTest {

    @Test
    public fun `initial state is stopped`() {
        val c =
            Candiriya(
                ProxyConfig(
                    network = kz.bejiihiu.candiriya.config.NetworkConfig(bind = "127.0.0.1:0")
                )
            )
        assertThat(c.getState()).isEqualTo(LifecycleState.STOPPED)
    }

    @Test
    public fun `start transitions to running`() {
        val c =
            Candiriya(
                ProxyConfig(
                    network = kz.bejiihiu.candiriya.config.NetworkConfig(bind = "127.0.0.1:0")
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
            Candiriya(
                ProxyConfig(
                    network = kz.bejiihiu.candiriya.config.NetworkConfig(bind = "127.0.0.1:0")
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
            Candiriya(
                ProxyConfig(
                    network = kz.bejiihiu.candiriya.config.NetworkConfig(bind = "127.0.0.1:0")
                )
            )
        c.start()
        c.stop()
        // second stop should not throw
        c.stop()
        assertThat(c.getState()).isEqualTo(LifecycleState.STOPPED)
    }
}
