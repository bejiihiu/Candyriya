package kz.bejiihiu.candiriya.network

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.netty.channel.Channel
import org.apache.logging.log4j.LogManager

/**
 * Small helpers for mutating netty pipelines.
 * Compression/encryption swap is tricky — keep it idempotent or we double-wrap and explode xd
 */
@SuppressFBWarnings(value = ["DE_MIGHT_IGNORE"], justification = "pipeline remove may throw, we ignore xd")
public object PipelineUtil {

    private val logger = LogManager.getLogger(PipelineUtil::class.java)

    private const val FRAME_DECODER = "frameDecoder"
    private const val PACKET_DECODER = "packetDecoder"
    private const val PACKET_ENCODER = "packetEncoder"
    private const val COMPRESSION_DECODER = "compressionDecoder"
    private const val COMPRESSION_ENCODER = "compressionEncoder"

    /**
     * Enable compression on a channel. Idempotent — if already there, do nothing.
     * Removes the plain length encoder and adds compression encoder instead,
     * because compression encoder already handles framing (length + dataLength).
     */
    public fun enableCompression(channel: Channel, threshold: Int) {
        if (threshold < 0) return
        val pipeline = channel.pipeline()

        // decoder: add after frameDecoder if not present
        if (pipeline.get(COMPRESSION_DECODER) == null) {
            try {
                if (pipeline.get(FRAME_DECODER) != null) {
                    pipeline.addAfter(
                        FRAME_DECODER,
                        COMPRESSION_DECODER,
                        kz.bejiihiu.candiriya.protocol.codec.MinecraftCompressionDecoder(threshold)
                    )
                } else {
                    pipeline.addFirst(
                        COMPRESSION_DECODER,
                        kz.bejiihiu.candiriya.protocol.codec.MinecraftCompressionDecoder(threshold)
                    )
                }
                logger.debug("added compressionDecoder threshold={} to {}", threshold, channel)
            } catch (e: Exception) {
                logger.warn("failed to add compressionDecoder to {}", channel, e)
            }
        }

        // encoder: swap packetEncoder -> compressionEncoder
        if (pipeline.get(COMPRESSION_ENCODER) == null) {
            if (pipeline.get(PACKET_ENCODER) != null) {
                try {
                    pipeline.remove(PACKET_ENCODER)
                } catch (_: Exception) {
                    // already gone, nvm
                }
            }
            try {
                pipeline.addLast(
                    COMPRESSION_ENCODER,
                    kz.bejiihiu.candiriya.protocol.codec.MinecraftCompressionEncoder(threshold)
                )
                logger.debug("added compressionEncoder threshold={} to {}", threshold, channel)
            } catch (e: Exception) {
                logger.warn("failed to add compressionEncoder to {}", channel, e)
            }
        }
    }

    public fun isCompressionEnabled(channel: Channel): Boolean = channel.pipeline().get(COMPRESSION_DECODER) != null
}
