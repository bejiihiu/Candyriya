package kz.bejiihiu.candiriya.network.session

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import java.security.KeyPair
import java.util.UUID
import kz.bejiihiu.candiriya.config.ProxyConfig
import kz.bejiihiu.candiriya.protocol.ConnectionState
import kz.bejiihiu.candiriya.protocol.EncryptionUtil
import org.apache.logging.log4j.LogManager

@SuppressFBWarnings(
    value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2", "DE_MIGHT_IGNORE"],
    justification = "session fields intentional"
)
public class ProxySession(
    public val config: ProxyConfig,
    public var clientChannel: Channel? = null,
    public var backendChannel: Channel? = null,
    public var state: ConnectionState = ConnectionState.HANDSHAKE,
    public var protocolVersion: Int = -1,
    public var serverAddress: String = "",
    public var serverPort: Int = 25565,
    public var username: String = "",
    public var uuid: UUID? = null,
    public var verifyToken: ByteArray? = null,
    public val keyPair: KeyPair = EncryptionUtil.generateKeyPair()
) {
    private val logger = LogManager.getLogger(ProxySession::class.java)

    public fun setClient(ctx: ChannelHandlerContext) {
        clientChannel = ctx.channel()
    }

    public fun setBackend(ch: Channel) {
        backendChannel = ch
    }

    @SuppressFBWarnings(value = ["DE_MIGHT_IGNORE"], justification = "close ignore")
    public fun closeBoth(reason: String? = null) {
        try {
            if (reason != null) logger.info("closing session {} reason={}", username, reason)
        } catch (_: Exception) {}
        try {
            clientChannel?.close()
        } catch (_: Exception) {}
        try {
            backendChannel?.close()
        } catch (_: Exception) {}
    }
}
