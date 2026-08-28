package kz.bejiihiu.candiriya.protocol

import io.netty.buffer.Unpooled
import io.netty.handler.codec.CorruptedFrameException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

public class VarIntTest {

    @ParameterizedTest
    @CsvSource(
        "0, 00",
        "1, 01",
        "127, 7F",
        "128, 8001",
        "255, FF01",
        "25565, DDC701",
        "2097151, FFFF7F",
        "2147483647, FFFFFFFF07",
        "-1, FFFFFFFF0F",
        "-2147483648, 8080808008"
    )
    public fun `varint encode vectors`(value: Int, hex: String) {
        val buf = Unpooled.buffer()
        try {
            VarInt.writeVarInt(buf, value)
            val bytes = ByteArray(buf.readableBytes())
            buf.readBytes(bytes)
            val actualHex = bytes.joinToString("") { "%02X".format(it) }
            assertThat(actualHex).isEqualTo(hex)
        } finally {
            buf.release()
        }
    }

    @Test
    public fun `roundtrip vectors`() {
        val vectors = intArrayOf(0, 1, 127, 128, 255, 25565, 2097151, 2147483647, -1, -2147483648)
        for (v in vectors) {
            val buf = Unpooled.buffer()
            try {
                VarInt.writeVarInt(buf, v)
                val decoded = VarInt.readVarInt(buf)
                assertThat(decoded).isEqualTo(v)
            } finally {
                buf.release()
            }
        }
    }

    @Test
    public fun `varIntSize correct`() {
        assertThat(VarInt.varIntSize(0)).isEqualTo(1)
        assertThat(VarInt.varIntSize(127)).isEqualTo(1)
        assertThat(VarInt.varIntSize(128)).isEqualTo(2)
        assertThat(VarInt.varIntSize(25565)).isEqualTo(3)
        assertThat(VarInt.varIntSize(2097151)).isEqualTo(3)
        assertThat(VarInt.varIntSize(2147483647)).isEqualTo(5)
        assertThat(VarInt.varIntSize(-1)).isEqualTo(5)
    }

    @Test
    public fun `too big throws`() {
        // 6 bytes varint -> too big
        val buf = Unpooled.wrappedBuffer(
            byteArrayOf(
                0x80.toByte(),
                0x80.toByte(),
                0x80.toByte(),
                0x80.toByte(),
                0x80.toByte(),
                0x01
            )
        )
        try {
            assertThatThrownBy { VarInt.readVarInt(buf) }
                .isInstanceOf(CorruptedFrameException::class.java)
        } finally {
            buf.release()
        }
    }

    @Test
    public fun `peekVarInt returns null if not enough bytes`() {
        val buf = Unpooled.wrappedBuffer(byteArrayOf(0x80.toByte()))
        try {
            assertThat(VarInt.peekVarInt(buf)).isNull()
        } finally {
            buf.release()
        }
    }

    @Test
    public fun `peekVarInt too big`() {
        val buf = Unpooled.wrappedBuffer(
            byteArrayOf(
                0x80.toByte(),
                0x80.toByte(),
                0x80.toByte(),
                0x80.toByte(),
                0x80.toByte(),
                0x01
            )
        )
        try {
            assertThatThrownBy { VarInt.peekVarInt(buf) }
                .isInstanceOf(CorruptedFrameException::class.java)
        } finally {
            buf.release()
        }
    }
}
