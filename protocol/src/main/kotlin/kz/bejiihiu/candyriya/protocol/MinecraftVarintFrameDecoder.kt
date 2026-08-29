package kz.bejiihiu.candyriya.protocol

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.CorruptedFrameException

/**
 * Frame decoder: reads VarInt length prefix, slices payload.
 * Compatible with Velocity's MinecraftVarintFrameDecoder.
 */
public class MinecraftVarintFrameDecoder(
    private val maxPacketSize: Int = 2097152
) : ByteToMessageDecoder() {

    override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
        input.markReaderIndex()
        val length = VarInt.peekVarInt(input)
        if (length == null) {
            input.resetReaderIndex()
            return
        }
        val varIntSize = VarInt.varIntSize(length)
        // ensure length varint bytes are actually available (peek already did)
        if (input.readableBytes() < varIntSize) {
            input.resetReaderIndex()
            return
        }
        // consume length varint
        try {
            VarInt.readVarInt(input)
        } catch (e: Exception) {
            input.resetReaderIndex()
            throw e
        }
        if (length < 0) {
            throw CorruptedFrameException("negative packet length $length")
        }
        if (length > maxPacketSize) {
            throw CorruptedFrameException("packet length $length > max $maxPacketSize")
        }
        if (input.readableBytes() < length) {
            input.resetReaderIndex()
            return
        }
        out.add(input.readRetainedSlice(length))
    }
}
