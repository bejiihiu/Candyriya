package kz.bejiihiu.candyriya.protocol

import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

public class MinecraftVarintLengthEncoderTest {

    @Test
    public fun `encodes length prefix`() {
        val ch = EmbeddedChannel(MinecraftVarintLengthEncoder())
        val data = Unpooled.wrappedBuffer(byteArrayOf(0xAA.toByte(), 0xBB.toByte()))
        val packet = MinecraftPacket(0x00, data)
        ch.writeOutbound(packet)
        val out = ch.readOutbound<io.netty.buffer.ByteBuf>()
        assertThat(out).isNotNull
        val len = VarInt.readVarInt(out!!)
        val id = VarInt.readVarInt(out)
        assertThat(id).isEqualTo(0x00)
        assertThat(len).isEqualTo(1 + 2)
        assertThat(out.readableBytes()).isEqualTo(2)
        out.release()
        data.release()
        ch.finishAndReleaseAll()
    }

    @Test
    public fun `roundtrip via decoder`() {
        val encoder = EmbeddedChannel(MinecraftVarintLengthEncoder())
        val decoderFrame = EmbeddedChannel(MinecraftVarintFrameDecoder(2097152))
        val decoderPacket = EmbeddedChannel(MinecraftPacketDecoder())

        val payload = Unpooled.wrappedBuffer(byteArrayOf(0x01, 0x02, 0x03))
        val packet = MinecraftPacket(0x15, payload)
        encoder.writeOutbound(packet)
        val encoded = encoder.readOutbound<io.netty.buffer.ByteBuf>()!!
        // feed through frame decoder
        decoderFrame.writeInbound(encoded.retainedSlice())
        encoded.release()
        val frame = decoderFrame.readInbound<io.netty.buffer.ByteBuf>()!!
        decoderPacket.writeInbound(frame)
        val decoded = decoderPacket.readInbound<MinecraftPacket>()!!
        assertThat(decoded.id).isEqualTo(0x15)
        assertThat(decoded.data.readableBytes()).isEqualTo(3)
        decoded.data.release()
        payload.release()
        encoder.finishAndReleaseAll()
        decoderFrame.finishAndReleaseAll()
        decoderPacket.finishAndReleaseAll()
    }
}
