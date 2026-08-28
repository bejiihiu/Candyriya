package kz.bejiihiu.candiriya.protocol

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

/**
 * Encodes [MinecraftPacket] to length-prefixed bytes: VarInt(length) + VarInt(id) + data
 */
public class MinecraftVarintLengthEncoder : MessageToByteEncoder<MinecraftPacket>() {

    override fun encode(ctx: ChannelHandlerContext, msg: MinecraftPacket, out: ByteBuf) {
        val idSize = VarInt.varIntSize(msg.id)
        val dataSize = msg.data.readableBytes()
        val packetLen = idSize + dataSize
        VarInt.writeVarInt(out, packetLen)
        VarInt.writeVarInt(out, msg.id)
        // copy without moving readerIndex of msg.data
        out.writeBytes(msg.data, msg.data.readerIndex(), dataSize)
        // TODO: compression/encryption handlers here
    }
}
