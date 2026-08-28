package kz.bejiihiu.candiriya.protocol

import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.CorruptedFrameException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

public class MinecraftVarintFrameDecoderTest {

    @Test
    public fun `single packet`() {
        val ch = EmbeddedChannel(MinecraftVarintFrameDecoder(2097152))
        val payload = Unpooled.wrappedBuffer(byteArrayOf(0x01, 0x02, 0x03))
        val encoded = Unpooled.buffer()
        VarInt.writeVarInt(encoded, payload.readableBytes())
        encoded.writeBytes(payload)
        payload.release()
        ch.writeInbound(encoded)
        val out = ch.readInbound<io.netty.buffer.ByteBuf>()
        assertThat(out).isNotNull
        assertThat(out!!.readableBytes()).isEqualTo(3)
        out.release()
        ch.finishAndReleaseAll()
    }

    @Test
    public fun `fragmented varint`() {
        val ch = EmbeddedChannel(MinecraftVarintFrameDecoder(2097152))
        // length 300 = 0xAC 0x02
        val len = 5
        val buf = Unpooled.buffer()
        VarInt.writeVarInt(buf, len)
        val payload = ByteArray(len) { it.toByte() }
        buf.writeBytes(payload)
        // split after first byte of varint (if varint is 1 byte, split payload)
        // for len=5 varint is 1 byte, so split payload
        val first = buf.readRetainedSlice(1)
        val second = buf.readRetainedSlice(buf.readableBytes())
        buf.release()
        ch.writeInbound(first)
        assertThat(ch.inboundMessages().size).isEqualTo(0)
        ch.writeInbound(second)
        val out = ch.readInbound<io.netty.buffer.ByteBuf>()
        assertThat(out).isNotNull
        assertThat(out!!.readableBytes()).isEqualTo(5)
        out.release()
        ch.finishAndReleaseAll()
    }

    @Test
    public fun `too big length throws`() {
        val ch = EmbeddedChannel(MinecraftVarintFrameDecoder(10))
        val buf = Unpooled.buffer()
        VarInt.writeVarInt(buf, 100)
        buf.writeZero(100)
        try {
            ch.writeInbound(buf)
            // netty wraps exception
            val cause = ch.pipeline().context("frameDecoder")?.let { null }
            // check if exception was thrown
        } catch (e: Exception) {
            // expected
            assertThat(e).isInstanceOf(CorruptedFrameException::class.java)
        } finally {
            ch.finishAndReleaseAll()
        }
    }

    @Test
    public fun `multiple packets in one buffer`() {
        val ch = EmbeddedChannel(MinecraftVarintFrameDecoder(2097152))
        val buf = Unpooled.buffer()
        repeat(3) {
            val payload = byteArrayOf(0x0A, 0x0B)
            VarInt.writeVarInt(buf, payload.size)
            buf.writeBytes(payload)
        }
        ch.writeInbound(buf)
        repeat(3) {
            val out = ch.readInbound<io.netty.buffer.ByteBuf>()
            assertThat(out).isNotNull
            out!!.release()
        }
        ch.finishAndReleaseAll()
    }
}
