package kz.bejiihiu.candyriya.protocol

import io.netty.buffer.ByteBuf
import io.netty.handler.codec.CorruptedFrameException
import java.io.IOException

/**
 * VarInt codec for Minecraft protocol (LEB128, max 5 bytes).
 * Vectors: 0->[00], 128->[80 01], 25565->[DD C7 01], -1->[FF FF FF FF 0F]
 */
public object VarInt {

    /**
     * Reads VarInt from [buf] advancing readerIndex. Throws if >5 bytes or not enough data.
     */
    @Throws(IOException::class)
    public fun readVarInt(buf: ByteBuf): Int {
        var value = 0
        var position = 0
        var currentByte: Byte
        while (true) {
            if (!buf.isReadable) {
                throw IOException("VarInt not enough bytes")
            }
            currentByte = buf.readByte()
            value = value or ((currentByte.toInt() and 0x7F) shl position)
            if ((currentByte.toInt() and 0x80) == 0) break
            position += 7
            if (position >= 32) {
                throw CorruptedFrameException("VarInt too big")
            }
        }
        return value
    }

    /**
     * Reads VarInt peeking: returns null if not enough data, throws if too big.
     * Does NOT advance readerIndex on null.
     */
    public fun peekVarInt(buf: ByteBuf): Int? {
        val readerIndex = buf.readerIndex()
        var value = 0
        var position = 0
        var bytesRead = 0
        while (bytesRead < 5) {
            if (buf.readerIndex() + bytesRead >= buf.writerIndex()) {
                return null
            }
            val currentByte = buf.getByte(readerIndex + bytesRead).toInt()
            bytesRead++
            value = value or ((currentByte and 0x7F) shl position)
            if ((currentByte and 0x80) == 0) {
                return value
            }
            position += 7
        }
        throw CorruptedFrameException("VarInt too big")
    }

    public fun varIntSize(value: Int): Int {
        var v = value
        var size = 0
        do {
            size++
            v = v ushr 7
        } while (v != 0)
        return size
    }

    public fun writeVarInt(buf: ByteBuf, value: Int) {
        var v = value
        // yep classic LEB128 xd
        while (true) {
            if ((v and 0x7F.inv()) == 0) {
                buf.writeByte(v)
                return
            }
            buf.writeByte((v and 0x7F) or 0x80)
            v = v ushr 7
        }
    }
}
