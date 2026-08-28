package kz.bejiihiu.candiriya.protocol.codec

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import java.util.zip.Deflater
import kz.bejiihiu.candiriya.protocol.VarInt

/**
 * Compresses if packet length >= threshold, else sends with dataLength 0.
 * Input is raw packet bytes (VarInt id + payload) without length prefix.
 * Output: VarInt(dataLength) + data (dataLength==0 means uncompressed)
 * Note: outer VarInt length prefix is added by MinecraftVarintLengthEncoder outside,
 * so this encoder should sit BEFORE length encoder. But for simplicity we handle
 * compression as framing that includes its own length.
 *
 * Actually Minecraft with compression: frame = VarInt(packetLength) + VarInt(dataLength) + data
 * where packetLength = VarInt.size(dataLength) + data.size
 * We implement MessageToByteEncoder<ByteBuf> that transforms raw -> compressed frame without outer length.
 * Then length encoder should NOT be used when compression enabled. To avoid confusion,
 * this encoder outputs VarInt(length)+VarInt(dataLength)+data directly if threshold >=0.
 * If threshold <0, compression disabled.
 */
public class MinecraftCompressionEncoder(
    private val threshold: Int
) : MessageToByteEncoder<kz.bejiihiu.candiriya.protocol.MinecraftPacket>() {

    private val deflater = Deflater()

    override fun encode(
        ctx: ChannelHandlerContext,
        msg: kz.bejiihiu.candiriya.protocol.MinecraftPacket,
        out: ByteBuf
    ) {
        // build raw = VarInt(id) + data
        val dataSize = msg.data.readableBytes()
        val idSize = VarInt.varIntSize(msg.id)
        val rawSize = idSize + dataSize
        if (threshold < 0 || rawSize < threshold) {
            // uncompressed: VarInt(packetLength) + VarInt(0) + raw
            val packetLen = rawSize + VarInt.varIntSize(0)
            VarInt.writeVarInt(out, packetLen)
            VarInt.writeVarInt(out, 0)
            VarInt.writeVarInt(out, msg.id)
            out.writeBytes(msg.data, msg.data.readerIndex(), dataSize)
            return
        }
        // compress raw
        val raw = ByteArray(rawSize)
        val tmp = io.netty.buffer.Unpooled.buffer(rawSize)
        try {
            VarInt.writeVarInt(tmp, msg.id)
            tmp.writeBytes(msg.data, msg.data.readerIndex(), dataSize)
            tmp.getBytes(0, raw)
        } finally {
            tmp.release()
        }
        deflater.setInput(raw)
        deflater.finish()
        val compressed = ByteArray(rawSize)
        val compSize = deflater.deflate(compressed)
        deflater.reset()
        VarInt.writeVarInt(out, VarInt.varIntSize(rawSize) + compSize)
        VarInt.writeVarInt(out, rawSize)
        out.writeBytes(compressed, 0, compSize)
    }

    override fun handlerRemoved(ctx: ChannelHandlerContext) {
        deflater.end()
        super.handlerRemoved(ctx)
    }
}
