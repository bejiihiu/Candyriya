package kz.bejiihiu.candyriya.protocol.packet

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import kz.bejiihiu.candyriya.protocol.VarInt

public object PacketCodec {
    public fun encode(packet: Packet): ByteBuf {
        val buf = Unpooled.buffer()
        VarInt.writeVarInt(buf, packet.getId())
        packet.encode(buf)
        return buf
    }

    public fun encodeWithLength(packet: Packet): ByteBuf {
        val content = encode(packet)
        try {
            val out = Unpooled.buffer()
            VarInt.writeVarInt(out, content.readableBytes())
            out.writeBytes(content)
            return out
        } finally {
            content.release()
        }
    }
}
