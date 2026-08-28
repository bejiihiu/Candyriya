package kz.bejiihiu.candiriya.protocol.codec

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageEncoder
import javax.crypto.Cipher

public class MinecraftCipherEncoder(
    private val cipher: Cipher
) : MessageToMessageEncoder<ByteBuf>() {
    override fun encode(ctx: ChannelHandlerContext, msg: ByteBuf, out: MutableList<Any>) {
        val readable = msg.readableBytes()
        val heap = ByteArray(readable)
        msg.getBytes(msg.readerIndex(), heap)
        val encrypted = cipher.update(heap)
            ?: throw IllegalStateException("cipher update returned null")
        val buf = ctx.alloc().buffer(encrypted.size)
        buf.writeBytes(encrypted)
        out.add(buf)
    }
}
