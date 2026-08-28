package kz.bejiihiu.candiriya.protocol

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.netty.buffer.ByteBuf
import java.nio.charset.StandardCharsets
import java.util.UUID

public object UuidUtil {

    public fun readUuid(buf: ByteBuf): UUID {
        val most = buf.readLong()
        val least = buf.readLong()
        return UUID(most, least)
    }

    public fun writeUuid(buf: ByteBuf, uuid: UUID) {
        buf.writeLong(uuid.mostSignificantBits)
        buf.writeLong(uuid.leastSignificantBits)
    }

    public fun offlineUuid(username: String): UUID =
        UUID.nameUUIDFromBytes("OfflinePlayer:$username".toByteArray(StandardCharsets.UTF_8))

    @SuppressFBWarnings(
        value = ["SA_LOCAL_SELF_ASSIGNMENT"],
        justification = "kotlin compiler generates self assignment"
    )
    public fun parseUndashedOrDashed(str: String): UUID = try {
        UUID.fromString(str)
    } catch (_: IllegalArgumentException) {
        // undashed 32 hex
        require(str.length == 32) { "invalid uuid $str" }
        UUID.fromString(
            "${str.substring(0, 8)}-${str.substring(8, 12)}-" +
                "${str.substring(12, 16)}-${str.substring(16, 20)}-${str.substring(20)}"
        )
    }
}
