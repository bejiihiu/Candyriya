package kz.bejiihiu.candyriya.protocol

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.netty.buffer.ByteBuf

/**
 * Minecraft packet: id + payload (without length prefix).
 * [data] is retained slice with refCnt=1, owner must release.
 */
@SuppressFBWarnings(
    value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2"],
    justification = "ByteBuf is intentionally shared with retain/release semantics"
)
public data class MinecraftPacket(
    val id: Int,
    val data: ByteBuf
)
