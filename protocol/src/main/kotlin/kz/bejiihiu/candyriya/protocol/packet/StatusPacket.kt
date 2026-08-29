package kz.bejiihiu.candyriya.protocol.packet

import io.netty.buffer.ByteBuf
import kz.bejiihiu.candyriya.protocol.StringUtil

// serverbound
public object StatusRequestPacket : Packet {
    override fun getId(): Int = 0x00
    override fun encode(buf: ByteBuf) { /* empty */ }
    public fun decode(buf: ByteBuf): StatusRequestPacket = StatusRequestPacket
}

public data class PingRequestPacket(val payload: Long) : Packet {
    override fun getId(): Int = 0x01
    override fun encode(buf: ByteBuf) {
        buf.writeLong(payload)
    }
    public companion object : PacketDecoder<PingRequestPacket> {
        override fun decode(buf: ByteBuf): PingRequestPacket {
            require(buf.readableBytes() >= 8) { "ping too short" }
            return PingRequestPacket(buf.readLong())
        }
    }
}

// clientbound
public data class StatusResponsePacket(val json: String) : Packet {
    override fun getId(): Int = 0x00
    override fun encode(buf: ByteBuf) {
        StringUtil.writeString(buf, json)
    }
    public companion object : PacketDecoder<StatusResponsePacket> {
        override fun decode(buf: ByteBuf): StatusResponsePacket = StatusResponsePacket(StringUtil.readString(buf, 262144))
    }
}

public data class PongResponsePacket(val payload: Long) : Packet {
    override fun getId(): Int = 0x01
    override fun encode(buf: ByteBuf) {
        buf.writeLong(payload)
    }
    public companion object : PacketDecoder<PongResponsePacket> {
        override fun decode(buf: ByteBuf): PongResponsePacket {
            require(buf.readableBytes() >= 8) { "pong too short" }
            return PongResponsePacket(buf.readLong())
        }
    }
}
