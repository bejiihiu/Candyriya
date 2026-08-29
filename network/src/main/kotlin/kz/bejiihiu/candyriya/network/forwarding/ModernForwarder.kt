package kz.bejiihiu.candyriya.network.forwarding

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kz.bejiihiu.candyriya.protocol.StringUtil
import kz.bejiihiu.candyriya.protocol.VarInt

/**
 * Modern forwarding — straight yoink from PaperMC/Velocity, respect ++
 *
 * Source: https://github.com/PaperMC/Velocity/blob/dev/3.0.0/proxy/src/main/java/com/velocitypowered/proxy/connection/PlayerDataForwarding.java
 * License: GPLv3 (we credit, we don't hide — go star Velocity, they did the hard work)
 *
 * How it works:
 * 1) backend sends LoginPluginMessage with channel "velocity:player_info" and 1 byte requested version (1..4)
 * 2) proxy replies with LoginPluginResponse( id, true, data ) where data = HMAC-SHA256(secret, payload) + payload
 * 3) payload = varint version + string address + uuid + string username + properties (+ key if version >=2)
 *
 * For Candyriya we are mostly offline-mode without IdentifiedKey, so we always use version 1 (MODERN_DEFAULT).
 * That's enough for Paper/Purpur with `velocity modern` enabled.
 *
 * btw if you haven't watched Chainsaw Man — go watch it, it's peak fiction xd
 * spoiler: yeah, Denji actually does touch Power's chest in Aki's bathroom — chapter 5 vibes, Fujimoto is wild lol
 * now back to boring proxy code...
 */
@SuppressFBWarnings(
    value = ["DB_DUPLICATE_BRANCHES", "REC_CATCH_EXCEPTION"],
    justification = "duplicate branch is intentional fallback, catch is for hmac xd"
)
public object ModernForwarder {

    public const val CHANNEL: String = "velocity:player_info"
    public const val MODERN_DEFAULT: Int = 1
    public const val MODERN_WITH_KEY: Int = 2
    public const val MODERN_WITH_KEY_V2: Int = 3
    public const val MODERN_LAZY_SESSION: Int = 4
    public const val MODERN_MAX_VERSION: Int = MODERN_LAZY_SESSION

    private const val ALGORITHM: String = "HmacSHA256"

    public fun createForwardingData(
        secret: ByteArray,
        address: String,
        uuid: java.util.UUID,
        username: String,
        requestedVersion: Int
    ): ByteBuf {
        val actualVersion = findForwardingVersion(requestedVersion)
        val forwarded: ByteBuf = Unpooled.buffer(512)
        try {
            VarInt.writeVarInt(forwarded, actualVersion)
            StringUtil.writeString(forwarded, address)
            forwarded.writeLong(uuid.mostSignificantBits)
            forwarded.writeLong(uuid.leastSignificantBits)
            StringUtil.writeString(forwarded, username)
            // properties — offline mode has none, just write empty list
            VarInt.writeVarInt(forwarded, 0)

            val mac = Mac.getInstance(ALGORITHM)
            mac.init(SecretKeySpec(secret, ALGORITHM))
            val dataLen = forwarded.readableBytes()
            val dataBytes = ByteArray(dataLen)
            forwarded.getBytes(forwarded.readerIndex(), dataBytes)
            mac.update(dataBytes)
            val sig = mac.doFinal()

            return Unpooled.wrappedBuffer(Unpooled.wrappedBuffer(sig), forwarded)
        } catch (e: Exception) {
            forwarded.release()
            throw RuntimeException(
                "failed to create modern forwarding data — check your forwarding.secret",
                e
            )
        }
    }

    public fun createForwardingData(
        secretString: String,
        address: String,
        uuid: java.util.UUID,
        username: String,
        requestedVersion: Int
    ): ByteBuf = createForwardingData(
        secretString.toByteArray(StandardCharsets.UTF_8),
        address,
        uuid,
        username,
        requestedVersion
    )

    private fun findForwardingVersion(requested: Int): Int {
<<<<<<<< HEAD:network/src/main/kotlin/kz/bejiihiu/candyriya/network/forwarding/VelocityModernForwarder.kt
        // velocity clamps to max, then downgrades based on protocol/key
        // we don't have protocol version here in Candyriya (we accept all like a proper slut xd)
        // so just clamp and return default if >1
========
>>>>>>>> feature/servers:network/src/main/kotlin/kz/bejiihiu/candyriya/network/forwarding/ModernForwarder.kt
        val clamped = requested.coerceAtMost(MODERN_MAX_VERSION)
        return if (clamped > MODERN_DEFAULT) MODERN_DEFAULT else MODERN_DEFAULT
    }

    /**
     * Legacy forwarding (handshake injection) — \0 separated: host\0ip\0uuid(undashed)\0propertiesJson
     */
    public fun createLegacyForwardingAddress(
        serverAddress: String,
        playerAddress: String,
        uuid: java.util.UUID,
        propertiesJson: String = "[]"
    ): String {
        val undashed = uuid.toString().replace("-", "")
        return "$serverAddress\u0000$playerAddress\u0000$undashed\u0000$propertiesJson"
    }

    public fun createBungeeGuardForwardingAddress(
        serverAddress: String,
        playerAddress: String,
        uuid: java.util.UUID,
        secret: String,
        propertiesJson: String = "[]"
    ): String {
        val undashed = uuid.toString().replace("-", "")
        val propsWithToken = if (propertiesJson == "[]") {
            """[{"name":"bungeeguard-token","value":"$secret","signature":""}]"""
        } else {
            propertiesJson
        }
        return "$serverAddress\u0000$playerAddress\u0000$undashed\u0000$propsWithToken"
    }
}

