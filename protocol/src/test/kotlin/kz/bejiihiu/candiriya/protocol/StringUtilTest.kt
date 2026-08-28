package kz.bejiihiu.candiriya.protocol

import io.netty.buffer.Unpooled
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

public class StringUtilTest {

    @Test
    public fun `encode decode roundtrip`() {
        val buf = Unpooled.buffer()
        try {
            StringUtil.writeString(buf, "hello")
            assertThat(StringUtil.readString(buf)).isEqualTo("hello")
        } finally {
            buf.release()
        }
    }

    @Test
    public fun `empty string`() {
        val buf = Unpooled.buffer()
        try {
            StringUtil.writeString(buf, "")
            assertThat(StringUtil.readString(buf)).isEqualTo("")
        } finally {
            buf.release()
        }
    }

    @Test
    public fun `utf8 string`() {
        val buf = Unpooled.buffer()
        try {
            val value = "Привет 🌍"
            StringUtil.writeString(buf, value)
            assertThat(StringUtil.readString(buf)).isEqualTo(value)
        } finally {
            buf.release()
        }
    }

    @Test
    public fun `maxLen exceeded on write`() {
        val buf = Unpooled.buffer()
        try {
            assertThatThrownBy { StringUtil.writeString(buf, "toolong", maxLen = 3) }
                .isInstanceOf(IllegalArgumentException::class.java)
        } finally {
            buf.release()
        }
    }

    @Test
    public fun `maxLen exceeded on read`() {
        val buf = Unpooled.buffer()
        try {
            StringUtil.writeString(buf, "hello", maxLen = 32767)
            // try to read with smaller maxLen
            assertThatThrownBy { StringUtil.readString(buf, maxLen = 2) }
                .isInstanceOf(IllegalArgumentException::class.java)
        } finally {
            buf.release()
        }
    }
}
