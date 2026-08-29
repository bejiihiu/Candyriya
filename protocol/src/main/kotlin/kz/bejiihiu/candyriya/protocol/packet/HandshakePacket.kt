package kz.bejiihiu.candyriya.protocol.packet

import io.netty.buffer.ByteBuf
import kz.bejiihiu.candyriya.protocol.StringUtil
import kz.bejiihiu.candyriya.protocol.VarInt

public data class HandshakePacket(
    val protocolVersion: Int,
    val serverAddress: String,
    val serverPort: Int,
    val nextState: Int
) : Packet {
    override fun getId(): Int = 0x00

    override fun encode(buf: ByteBuf) {
        VarInt.writeVarInt(buf, protocolVersion)
        StringUtil.writeString(buf, serverAddress, 255)
        buf.writeShort(serverPort)
        VarInt.writeVarInt(buf, nextState)
    }

    public companion object : PacketDecoder<HandshakePacket> {
        override fun decode(buf: ByteBuf): HandshakePacket {
            val proto = VarInt.readVarInt(buf)
            val addr = StringUtil.readString(buf, 255)
            val port = buf.readUnsignedShort()
            val next = VarInt.readVarInt(buf)
            return HandshakePacket(proto, addr, port, next)
        }
    }
}
