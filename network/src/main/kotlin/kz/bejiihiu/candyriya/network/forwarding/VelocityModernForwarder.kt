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
 * Velocity modern forwarding — straight yoink from PaperMC/Velocity, respect ++
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
public object VelocityModernForwarder {

    public const val CHANNEL: String = "velocity:player_info"
    public const val MODERN_DEFAULT: Int = 1
    public const val MODERN_WITH_KEY: Int = 2
    public const val MODERN_WITH_KEY_V2: Int = 3
    public const val MODERN_LAZY_SESSION: Int = 4
    public const val MODERN_MAX_VERSION: Int = MODERN_LAZY_SESSION

    private const val ALGORITHM: String = "HmacSHA256"

    /**
     * Create forwarding data exactly like Velocity's PlayerDataForwarding.createForwardingData(...)
     * Simplified: no IdentifiedKey support, always version 1, no lazy session.
     *
     * This is intentionally 1:1 with Velocity to stay compatible — if Velocity updates, we should too.
     */
    public fun createForwardingData(
        secret: ByteArray,
        address: String,
        uuid: java.util.UUID,
        username: String,
        requestedVersion: Int
    ): ByteBuf {
        // yoinked logic: clamp requested version, pick actual version
        val actualVersion = findForwardingVersion(requestedVersion)
        val forwarded: ByteBuf = Unpooled.buffer(512)
        try {
            VarInt.writeVarInt(forwarded, actualVersion)
            StringUtil.writeString(forwarded, address)
            forwarded.writeLong(uuid.mostSignificantBits)
            forwarded.writeLong(uuid.leastSignificantBits)
            StringUtil.writeString(forwarded, username)
            // properties — offline mode has none, just write empty list
            // velocity does: ProtocolUtils.writeProperties(buf, profile.getProperties())
            // which is varint size + for each property: string name, string value, string signature(optional)
            VarInt.writeVarInt(forwarded, 0)

            // no key handling for now — if we ever support online-mode with keys, add here
            // see Velocity's if (actualVersion >= MODERN_WITH_KEY ...) block

            // compute HMAC like velocity: mac.update(forwarded.array(), arrayOffset, readableBytes)
            // but Unpooled.buffer may not be array-backed after writes? Velocity uses array() directly.
            // safer: copy readable bytes
            val mac = Mac.getInstance(ALGORITHM)
            mac.init(SecretKeySpec(secret, ALGORITHM))
            val dataLen = forwarded.readableBytes()
            val dataBytes = ByteArray(dataLen)
            forwarded.getBytes(forwarded.readerIndex(), dataBytes)
            mac.update(dataBytes)
            val sig = mac.doFinal()

            // velocity does: wrappedBuffer(wrappedBuffer(sig), forwarded)
            // that's sig bytes + payload
            return Unpooled.wrappedBuffer(Unpooled.wrappedBuffer(sig), forwarded)
        } catch (e: Exception) {
            forwarded.release()
            throw RuntimeException(
                "failed to create modern forwarding data — check your forwarding.secret",
                e
            )
        }
    }

    /**
     * Overload with String secret (utf8 like BungeeGuard does)
     */
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
        // velocity clamps to max, then downgrades based on protocol/key
        // we don't have protocol version here in Candyriya (we accept all like a proper slut xd)
        // so just clamp and return default if >1
        val clamped = requested.coerceAtMost(MODERN_MAX_VERSION)
        // we don't support key/lazy session yet, so always 1 for now
        // TODO: when we add IdentifiedKey + 1.19.3+ protocol tracking, mirror Velocity's full switch
        return if (clamped > MODERN_DEFAULT) MODERN_DEFAULT else MODERN_DEFAULT
    }

    /**
     * Legacy forwarding (BungeeCord style) — not modern but included for completeness.
     * Velocity's createLegacyForwardingAddress — we keep it here for reference.
     * Used if forwardingMode == LEGACY / BUNGEEGUARD (handshake injection).
     */
    public fun createLegacyForwardingAddress(
        serverAddress: String,
        playerAddress: String,
        uuid: java.util.UUID,
        propertiesJson: String = "[]"
    ): String {
        // \0 separated: host\0ip\0uuid(undashed)\0propertiesJson
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
        // BungeeGuard = legacy + token property — velocity adds it as GameProfile property
        // for handshake we just inject via same legacy string (server checks token property)
        // simplified: append secret as extra \0 check would happen on server, but for now same as legacy
        // real Velocity does it via properties list — we don't have that in handshake path, so use same
        val undashed = uuid.toString().replace("-", "")
        val propsWithToken = if (propertiesJson == "[]") {
            """[{"name":"bungeeguard-token","value":"$secret","signature":""}]"""
        } else {
            propertiesJson
        }
        return "$serverAddress\u0000$playerAddress\u0000$undashed\u0000$propsWithToken"
    }
}
