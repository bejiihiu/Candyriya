package kz.bejiihiu.candyriya.protocol.packet

import io.netty.buffer.ByteBuf

/**
 * Typed Minecraft packet abstraction.
 * Each packet knows its id (for current state) and how to encode/decode itself.
 */
public interface Packet {
    /** packet id for its state+direction */
    public fun getId(): Int
    public fun encode(buf: ByteBuf)
}

public interface PacketDecoder<T : Packet> {
    public fun decode(buf: ByteBuf): T
}
