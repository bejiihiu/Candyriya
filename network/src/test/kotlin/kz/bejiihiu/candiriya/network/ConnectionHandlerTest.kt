package kz.bejiihiu.candiriya.network

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import kz.bejiihiu.candiriya.config.ProxyConfig
import kz.bejiihiu.candiriya.protocol.ConnectionState
import kz.bejiihiu.candiriya.protocol.MinecraftPacketDecoder
import kz.bejiihiu.candiriya.protocol.MinecraftVarintFrameDecoder
import kz.bejiihiu.candiriya.protocol.MinecraftVarintLengthEncoder
import kz.bejiihiu.candiriya.protocol.StringUtil
import kz.bejiihiu.candiriya.protocol.VarInt
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

public class ConnectionHandlerTest {

    private fun createHandshake(proto: Int = 767, addr: String = "localhost", port: Int = 25565, nextState: Int = 1): ByteBuf {
        val payload = Unpooled.buffer()
        VarInt.writeVarInt(payload, 0x00) // packet id
        VarInt.writeVarInt(payload, proto)
        StringUtil.writeString(payload, addr, 255)
        payload.writeShort(port)
        VarInt.writeVarInt(payload, nextState)
        val framed = Unpooled.buffer()
        VarInt.writeVarInt(framed, payload.readableBytes())
        framed.writeBytes(payload)
        payload.release()
        return framed
    }

    private fun createStatusRequest(): ByteBuf {
        val payload = Unpooled.buffer()
        VarInt.writeVarInt(payload, 0x00)
        val framed = Unpooled.buffer()
        VarInt.writeVarInt(framed, payload.readableBytes())
        framed.writeBytes(payload)
        payload.release()
        return framed
    }

    private fun createPing(value: Long = 123456L): ByteBuf {
        val payload = Unpooled.buffer()
        VarInt.writeVarInt(payload, 0x01)
        payload.writeLong(value)
        val framed = Unpooled.buffer()
        VarInt.writeVarInt(framed, payload.readableBytes())
        framed.writeBytes(payload)
        payload.release()
        return framed
    }

    @Test
    public fun `handshake transitions to STATUS`() {
        val config = ProxyConfig()
        val ch = EmbeddedChannel(
            MinecraftVarintFrameDecoder(config.protocol.maxPacketSize),
            MinecraftPacketDecoder(),
            MinecraftVarintLengthEncoder(),
            ConnectionHandler(config)
        )
        val handler = ch.pipeline().get(ConnectionHandler::class.java)
        ch.writeInbound(createHandshake(nextState = 1))
        assertThat(handler.state).isEqualTo(ConnectionState.STATUS)
        ch.finishAndReleaseAll()
    }

    @Test
    public fun `handshake transitions to LOGIN`() {
        val config = ProxyConfig()
        val ch = EmbeddedChannel(
            MinecraftVarintFrameDecoder(config.protocol.maxPacketSize),
            MinecraftPacketDecoder(),
            MinecraftVarintLengthEncoder(),
            ConnectionHandler(config)
        )
        val handler = ch.pipeline().get(ConnectionHandler::class.java)
        ch.writeInbound(createHandshake(nextState = 2))
        assertThat(handler.state).isEqualTo(ConnectionState.LOGIN)
        ch.finishAndReleaseAll()
    }

    @Test
    public fun `status request returns json with motd`() {
        val config = ProxyConfig()
        val ch = EmbeddedChannel(
            MinecraftVarintFrameDecoder(config.protocol.maxPacketSize),
            MinecraftPacketDecoder(),
            MinecraftVarintLengthEncoder(),
            ConnectionHandler(config)
        )
        ch.writeInbound(createHandshake(nextState = 1))
        ch.writeInbound(createStatusRequest())
        // read outbound: should be status response frame
        val out = ch.readOutbound<ByteBuf>()
        assertThat(out).isNotNull
        val len = VarInt.readVarInt(out!!)
        val id = VarInt.readVarInt(out)
        assertThat(id).isEqualTo(0x00)
        val json = StringUtil.readString(out)
        // motd is MiniMessage gradient, serialized per-char — check structure, not raw tag
        assertThat(json).contains(config.status.versionName)
        assertThat(json).contains("\"description\"")
        assertThat(json).contains("\"players\"")
        out.release()
        ch.finishAndReleaseAll()
    }

    @Test
    public fun `ping returns pong`() {
        val config = ProxyConfig()
        val ch = EmbeddedChannel(
            MinecraftVarintFrameDecoder(config.protocol.maxPacketSize),
            MinecraftPacketDecoder(),
            MinecraftVarintLengthEncoder(),
            ConnectionHandler(config)
        )
        ch.writeInbound(createHandshake(nextState = 1))
        // consume status request first
        ch.writeInbound(createStatusRequest())
        ch.readOutbound<ByteBuf>()?.release()
        ch.writeInbound(createPing(999L))
        val out = ch.readOutbound<ByteBuf>()
        assertThat(out).isNotNull
        VarInt.readVarInt(out!!)
        val id = VarInt.readVarInt(out)
        assertThat(id).isEqualTo(0x01)
        assertThat(out.readLong()).isEqualTo(999L)
        out.release()
        ch.finishAndReleaseAll()
    }

    @Test
    public fun `login disconnects`() {
        val config = ProxyConfig()
        val ch = EmbeddedChannel(
            MinecraftVarintFrameDecoder(config.protocol.maxPacketSize),
            MinecraftPacketDecoder(),
            MinecraftVarintLengthEncoder(),
            ConnectionHandler(config)
        )
        ch.writeInbound(createHandshake(nextState = 2))
        // send any login packet (id 0x00 login start)
        val payload = Unpooled.buffer()
        VarInt.writeVarInt(payload, 0x00)
        StringUtil.writeString(payload, "testplayer", 16)
        val framed = Unpooled.buffer()
        VarInt.writeVarInt(framed, payload.readableBytes())
        framed.writeBytes(payload)
        payload.release()
        ch.writeInbound(framed)
        val out = ch.readOutbound<ByteBuf>()
        assertThat(out).isNotNull
        out!!.release()
        ch.finishAndReleaseAll()
    }

    @Test
    public fun `any protocolVersion accepted`() {
        val config = ProxyConfig()
        val ch = EmbeddedChannel(
            MinecraftVarintFrameDecoder(config.protocol.maxPacketSize),
            MinecraftPacketDecoder(),
            MinecraftVarintLengthEncoder(),
            ConnectionHandler(config)
        )
        val handler = ch.pipeline().get(ConnectionHandler::class.java)
        ch.writeInbound(createHandshake(proto = 340, nextState = 1))
        assertThat(handler.state).isEqualTo(ConnectionState.STATUS)
        ch.finishAndReleaseAll()
    }
}
