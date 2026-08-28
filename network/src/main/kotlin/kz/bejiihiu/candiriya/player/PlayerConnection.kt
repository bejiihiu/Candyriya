package kz.bejiihiu.candiriya.player

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import java.net.SocketAddress
import kz.bejiihiu.candiriya.protocol.MinecraftPacket
import kz.bejiihiu.candiriya.protocol.StringUtil
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer

/**
 * Abstraction over netty Channel — so Player doesn't leak netty types everywhere.
 * Keeps domain pure, testable with fake impl.
 */
public interface PlayerConnection {
    public val remoteAddress: SocketAddress?
    public val isActive: Boolean
    public fun sendPacket(packet: MinecraftPacket)
    public fun disconnect(reason: Component? = null)
    public fun close()
}

public class NettyPlayerConnection(
    private val channel: Channel
) : PlayerConnection {
    override val remoteAddress: SocketAddress? get() = channel.remoteAddress()
    override val isActive: Boolean get() = channel.isActive

    override fun sendPacket(packet: MinecraftPacket) {
        if (channel.isActive) {
            channel.writeAndFlush(packet)
        } else {
            packet.data.release()
        }
    }

    override fun disconnect(reason: Component?) {
        if (!channel.isActive) return
        if (reason == null) {
            channel.close()
            return
        }
        val json = GsonComponentSerializer.gson().serialize(reason)
        val buf = Unpooled.buffer()
        StringUtil.writeString(buf, json)
        val pkt = MinecraftPacket(0x00, buf)
        channel.writeAndFlush(pkt).addListener {
            buf.release()
            channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
        }
    }

    override fun close() {
        if (channel.isOpen) channel.close()
    }

    public fun channel(): Channel = channel
}

@SuppressFBWarnings(value = ["EI_EXPOSE_REP"], justification = "test fake xd")
public class FakePlayerConnection(
    override val remoteAddress: SocketAddress? = null
) : PlayerConnection {
    public val sent: MutableList<MinecraftPacket> = mutableListOf()
    public var closed: Boolean = false
    public var disconnectReason: Component? = null
    override val isActive: Boolean get() = !closed
    override fun sendPacket(packet: MinecraftPacket) {
        sent.add(packet)
    }
    override fun disconnect(reason: Component?) {
        disconnectReason = reason
        closed = true
    }
    override fun close() {
        closed = true
    }
}
