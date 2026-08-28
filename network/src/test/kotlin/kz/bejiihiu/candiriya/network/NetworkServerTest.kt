package kz.bejiihiu.candiriya.network

import kz.bejiihiu.candiriya.config.NetworkConfig
import kz.bejiihiu.candiriya.config.ProxyConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

public class NetworkServerTest {

    @Test
    public fun `server binds to ephemeral port`() {
        // bind to 0 to get random free port
        val config = ProxyConfig(network = NetworkConfig(bind = "127.0.0.1:0"))
        val server = NetworkServer(config)
        val future = server.start()
        try {
            assertThat(future.channel().isActive).isTrue()
            assertThat(future.channel().localAddress()).isNotNull()
        } finally {
            server.stop()
        }
    }

    @Test
    public fun `server stops gracefully`() {
        val config = ProxyConfig(network = NetworkConfig(bind = "127.0.0.1:0"))
        val server = NetworkServer(config)
        server.start()
        server.stop()
        // after stop, groups should be shutdown - no exception means ok
    }
}
