package kz.bejiihiu.candiriya.protocol

import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

public class MinecraftPacketDecoderTest {

    @Test
    public fun `decodes packet id and payload`() {
        val ch = EmbeddedChannel(MinecraftPacketDecoder())
        val frame = Unpooled.buffer()
        VarInt.writeVarInt(frame, 0x2A)
        frame.writeBytes(byteArrayOf(0x11, 0x22, 0x33))
        ch.writeInbound(frame)
        val packet = ch.readInbound<MinecraftPacket>()
        assertThat(packet).isNotNull
        assertThat(packet!!.id).isEqualTo(0x2A)
        assertThat(packet.data.readableBytes()).isEqualTo(3)
        packet.data.release()
        ch.finishAndReleaseAll()
    }

    @Test
    public fun `empty payload`() {
        val ch = EmbeddedChannel(MinecraftPacketDecoder())
        val frame = Unpooled.buffer()
        VarInt.writeVarInt(frame, 0x00)
        ch.writeInbound(frame)
        val packet = ch.readInbound<MinecraftPacket>()
        assertThat(packet!!.id).isEqualTo(0x00)
        assertThat(packet.data.readableBytes()).isEqualTo(0)
        packet.data.release()
        ch.finishAndReleaseAll()
    }
}
