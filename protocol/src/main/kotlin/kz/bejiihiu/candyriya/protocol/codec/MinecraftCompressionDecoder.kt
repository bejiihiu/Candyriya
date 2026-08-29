package kz.bejiihiu.candyriya.protocol.codec

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.CorruptedFrameException
import java.util.zip.Inflater
import kz.bejiihiu.candyriya.protocol.VarInt

/**
 * Decompresses Minecraft compressed frames.
 * Frame: VarInt(dataLength) + data
 * - dataLength == 0 -> uncompressed (data is packet id+payload)
 * - dataLength != 0 -> zlib-compressed (data decompresses to dataLength bytes)
 */
public class MinecraftCompressionDecoder(
    private val threshold: Int
) : ByteToMessageDecoder() {

    override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
        input.markReaderIndex()
        val dataLength = VarInt.peekVarInt(input)
        if (dataLength == null) return
        val varIntSize = VarInt.varIntSize(dataLength)
        if (input.readableBytes() < varIntSize) return
        VarInt.readVarInt(input) // consume dataLength
        if (dataLength == 0) {
            // uncompressed — remaining bytes is packet
            if (input.readableBytes() == 0) {
                // empty? just pass empty
                out.add(Unpooled.EMPTY_BUFFER)
                return
            }
            out.add(input.readRetainedSlice(input.readableBytes()))
            return
        }
        if (dataLength < threshold) {
            throw CorruptedFrameException("compressed size $dataLength < threshold $threshold")
        }
        if (dataLength > 8388608) {
            throw CorruptedFrameException("compressed size $dataLength too large")
        }
        val compressed = input.readRetainedSlice(input.readableBytes())
        try {
            val inflater = Inflater()
            try {
                val bytes = ByteArray(compressed.readableBytes())
                compressed.readBytes(bytes)
                inflater.setInput(bytes)
                val outBytes = ByteArray(dataLength)
                val inflated = inflater.inflate(outBytes)
                if (inflated != dataLength) {
                    throw CorruptedFrameException("inflate mismatch $inflated != $dataLength")
                }
                if (!inflater.finished()) {
                    throw CorruptedFrameException("inflater not finished")
                }
                out.add(Unpooled.wrappedBuffer(outBytes))
            } finally {
                inflater.end()
            }
        } finally {
            compressed.release()
        }
    }
}
