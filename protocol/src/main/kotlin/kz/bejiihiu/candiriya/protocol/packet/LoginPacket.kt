package kz.bejiihiu.candiriya.protocol.packet

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.netty.buffer.ByteBuf
import java.util.UUID
import kz.bejiihiu.candiriya.protocol.StringUtil
import kz.bejiihiu.candiriya.protocol.UuidUtil
import kz.bejiihiu.candiriya.protocol.VarInt

// serverbound
public data class LoginStartPacket(
    val username: String,
    val uuid: UUID? = null
) : Packet {
    override fun getId(): Int = 0x00
    override fun encode(buf: ByteBuf) {
        StringUtil.writeString(buf, username, 16)
        if (uuid != null) {
            UuidUtil.writeUuid(buf, uuid)
        }
        // Note: 1.21.5+ may have extra fields but we keep minimal;
        // if uuid present we just write it, server may ignore.
    }

    public companion object : PacketDecoder<LoginStartPacket> {
        override fun decode(buf: ByteBuf): LoginStartPacket {
            val name = StringUtil.readString(buf, 16)
            val uuid = if (buf.isReadable) {
                // 1.21.5 LoginStart = String name + UUID (16 bytes) if online-mode style,
                // but offline may be just name. Check remaining 16 bytes.
                if (buf.readableBytes() >= 16) {
                    // peek if we have exactly 16 or more — but LoginStart has no extra after uuid
                    // So if readable >=16, read uuid.
                    UuidUtil.readUuid(buf)
                } else {
                    null
                }
            } else {
                null
            }
            return LoginStartPacket(name, uuid)
        }
    }
}

@SuppressFBWarnings(
    value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2"],
    justification = "byte arrays intentionally exposed for packet codec"
)
public data class EncryptionResponsePacket(
    val sharedSecret: ByteArray,
    val verifyToken: ByteArray
) : Packet {
    override fun getId(): Int = 0x01
    override fun encode(buf: ByteBuf) {
        VarInt.writeVarInt(buf, sharedSecret.size)
        buf.writeBytes(sharedSecret)
        VarInt.writeVarInt(buf, verifyToken.size)
        buf.writeBytes(verifyToken)
    }
    public companion object : PacketDecoder<EncryptionResponsePacket> {
        override fun decode(buf: ByteBuf): EncryptionResponsePacket {
            val ssLen = VarInt.readVarInt(buf)
            require(ssLen in 0..256) { "sharedSecret len $ssLen" }
            val ss = ByteArray(ssLen)
            buf.readBytes(ss)
            val vtLen = VarInt.readVarInt(buf)
            require(vtLen in 0..256) { "verifyToken len $vtLen" }
            val vt = ByteArray(vtLen)
            buf.readBytes(vt)
            return EncryptionResponsePacket(ss, vt)
        }
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptionResponsePacket) return false
        return sharedSecret.contentEquals(other.sharedSecret) &&
            verifyToken.contentEquals(other.verifyToken)
    }

    override fun hashCode(): Int =
        31 * sharedSecret.contentHashCode() + verifyToken.contentHashCode()
}

public object LoginAcknowledgedPacket : Packet {
    override fun getId(): Int = 0x03
    override fun encode(buf: ByteBuf) {}
    public fun decode(buf: ByteBuf): LoginAcknowledgedPacket = LoginAcknowledgedPacket
}

// clientbound
public data class DisconnectLoginPacket(val reasonJson: String) : Packet {
    override fun getId(): Int = 0x00
    override fun encode(buf: ByteBuf) {
        StringUtil.writeString(buf, reasonJson)
    }
    public companion object : PacketDecoder<DisconnectLoginPacket> {
        override fun decode(buf: ByteBuf): DisconnectLoginPacket =
            DisconnectLoginPacket(StringUtil.readString(buf, 262144))
    }
}

@SuppressFBWarnings(
    value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2"],
    justification = "byte arrays intentionally exposed"
)
public data class EncryptionRequestPacket(
    val serverId: String,
    val publicKey: ByteArray,
    val verifyToken: ByteArray,
    val shouldAuthenticate: Boolean = true
) : Packet {
    override fun getId(): Int = 0x01
    override fun encode(buf: ByteBuf) {
        StringUtil.writeString(buf, serverId, 20)
        VarInt.writeVarInt(buf, publicKey.size)
        buf.writeBytes(publicKey)
        VarInt.writeVarInt(buf, verifyToken.size)
        buf.writeBytes(verifyToken)
        buf.writeBoolean(shouldAuthenticate)
    }
    public companion object : PacketDecoder<EncryptionRequestPacket> {
        override fun decode(buf: ByteBuf): EncryptionRequestPacket {
            val sid = StringUtil.readString(buf, 20)
            val pkLen = VarInt.readVarInt(buf)
            val pk = ByteArray(pkLen)
            buf.readBytes(pk)
            val vtLen = VarInt.readVarInt(buf)
            val vt = ByteArray(vtLen)
            buf.readBytes(vt)
            val auth = if (buf.isReadable) buf.readBoolean() else true
            return EncryptionRequestPacket(sid, pk, vt, auth)
        }
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptionRequestPacket) return false
        return serverId == other.serverId &&
            publicKey.contentEquals(other.publicKey) &&
            verifyToken.contentEquals(other.verifyToken)
    }

    override fun hashCode(): Int = 31 * (31 * serverId.hashCode() + publicKey.contentHashCode()) +
        verifyToken.contentHashCode()
}

public data class SetCompressionPacket(val threshold: Int) : Packet {
    override fun getId(): Int = 0x03
    override fun encode(buf: ByteBuf) {
        VarInt.writeVarInt(buf, threshold)
    }
    public companion object : PacketDecoder<SetCompressionPacket> {
        override fun decode(buf: ByteBuf): SetCompressionPacket =
            SetCompressionPacket(VarInt.readVarInt(buf))
    }
}

@SuppressFBWarnings(
    value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2"],
    justification = "properties list exposed intentionally"
)
public data class LoginSuccessPacket(
    val uuid: UUID,
    val username: String,
    val properties: List<Property> = emptyList()
) : Packet {
    override fun getId(): Int = 0x02
    override fun encode(buf: ByteBuf) {
        UuidUtil.writeUuid(buf, uuid)
        StringUtil.writeString(buf, username, 16)
        VarInt.writeVarInt(buf, properties.size)
        for (p in properties) {
            StringUtil.writeString(buf, p.name)
            StringUtil.writeString(buf, p.value)
            val hasSig = p.signature != null
            buf.writeBoolean(hasSig)
            if (hasSig) {
                @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
                StringUtil.writeString(buf, p.signature!!)
            }
        }
        buf.writeBoolean(false) // strictErrorHandling — 1.21.5 requires boolean
    }
    public companion object : PacketDecoder<LoginSuccessPacket> {
        override fun decode(buf: ByteBuf): LoginSuccessPacket {
            val uuid = UuidUtil.readUuid(buf)
            val name = StringUtil.readString(buf, 16)
            val propCount = VarInt.readVarInt(buf)
            val props = mutableListOf<Property>()
            repeat(propCount) {
                val n = StringUtil.readString(buf)
                val v = StringUtil.readString(buf)
                val hasSig = buf.readBoolean()
                val sig = if (hasSig) StringUtil.readString(buf) else null
                props.add(Property(n, v, sig))
            }
            if (buf.isReadable) buf.readBoolean() // strictErrorHandling
            return LoginSuccessPacket(uuid, name, props)
        }
    }
}

public data class Property(val name: String, val value: String, val signature: String? = null)

public data class KeepAlivePacket(val id: Long) : Packet {
    // In CONFIGURATION+PLAY, keepAlive is shared; for minimal proxy we treat as play 0x26/0x24 etc.
    // For typed handling we just encode/decode as long for play.
    override fun getId(): Int = 0x24 // 1.21.5 play clientbound KeepAlive id (approx)
    override fun encode(buf: ByteBuf) {
        buf.writeLong(id)
    }
    public companion object : PacketDecoder<KeepAlivePacket> {
        override fun decode(buf: ByteBuf): KeepAlivePacket = KeepAlivePacket(buf.readLong())
    }
}

// Configuration minimal
public object FinishConfigurationPacket : Packet {
    override fun getId(): Int = 0x03 // clientbound finish config
    override fun encode(buf: ByteBuf) {}
    public fun decode(buf: ByteBuf): FinishConfigurationPacket = FinishConfigurationPacket
}

public object AcknowledgeFinishConfigurationPacket : Packet {
    override fun getId(): Int = 0x03 // serverbound ack
    override fun encode(buf: ByteBuf) {}
    public fun decode(buf: ByteBuf): AcknowledgeFinishConfigurationPacket =
        AcknowledgeFinishConfigurationPacket
}
