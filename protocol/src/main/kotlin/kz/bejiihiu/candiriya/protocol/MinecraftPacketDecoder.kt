package kz.bejiihiu.candiriya.protocol

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder

/**
 * Decodes frame ByteBuf (length already stripped) into [MinecraftPacket].
 * Frame contains: VarInt packetId + payload.
 */
public class MinecraftPacketDecoder : ByteToMessageDecoder() {

    override fun decode(ctx: ChannelHandlerContext, frame: ByteBuf, out: MutableList<Any>) {
        if (!frame.isReadable) return
        val id = VarInt.readVarInt(frame)
        // slice remaining payload, retain for packet owner
        val payload = frame.readRetainedSlice(frame.readableBytes())
        out.add(MinecraftPacket(id, payload))
    }
}
