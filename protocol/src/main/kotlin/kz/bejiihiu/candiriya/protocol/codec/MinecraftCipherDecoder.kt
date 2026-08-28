package kz.bejiihiu.candiriya.protocol.codec

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageDecoder
import javax.crypto.Cipher

public class MinecraftCipherDecoder(
    private val cipher: Cipher
) : MessageToMessageDecoder<ByteBuf>() {
    override fun decode(ctx: ChannelHandlerContext, msg: ByteBuf, out: MutableList<Any>) {
        val readable = msg.readableBytes()
        val heap = ByteArray(readable)
        msg.readBytes(heap)
        val decrypted = cipher.update(heap)
            ?: throw IllegalStateException("cipher update returned null")
        // cipher.update may return less; but for CFB8 it returns same length
        val buf = ctx.alloc().buffer(decrypted.size)
        buf.writeBytes(decrypted)
        out.add(buf)
    }
}
