package kz.bejiihiu.candyriya.protocol

import io.netty.buffer.ByteBuf
import java.nio.charset.StandardCharsets

/**
 * Minecraft string helpers: VarInt length + UTF8 bytes.
 */
public object StringUtil {

    public fun readString(buf: ByteBuf, maxLen: Int = 32767): String {
        val len = VarInt.readVarInt(buf)
        require(len >= 0) { "string length <0: $len" }
        require(len <= maxLen * 4) { "string length $len > maxBytes ${maxLen * 4}" }
        require(buf.readableBytes() >= len) {
            "not enough bytes for string: need $len have ${buf.readableBytes()}"
        }
        val bytes = ByteArray(len)
        buf.readBytes(bytes)
        val str = String(bytes, StandardCharsets.UTF_8)
        require(str.length <= maxLen) { "decoded string length ${str.length} > maxLen $maxLen" }
        return str
    }

    public fun writeString(buf: ByteBuf, value: String, maxLen: Int = 32767) {
        require(value.length <= maxLen) { "string length ${value.length} > maxLen $maxLen" }
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size <= maxLen * 4) { "encoded bytes ${bytes.size} > maxBytes ${maxLen * 4}" }
        VarInt.writeVarInt(buf, bytes.size)
        buf.writeBytes(bytes)
    }

    /**
     * Helper to check ByteBufUtil.writeUtf8 style without extra alloc.
     */
    public fun writeStringByteBufUtil(buf: ByteBuf, value: String, maxLen: Int = 32767) {
        writeString(buf, value, maxLen)
    }
}
