package io.izzel.arclight.common.mod.plugin.messaging;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.ResourceLocation;

public interface RawPayload extends CustomPacketPayload {
    ByteBuf arclight$getData();
    void arclight$setData(ByteBuf data);

    default byte[] arclight$readBytes() {
        final var buf = arclight$getData();
        byte[] allocate = new byte[buf.readableBytes()];
        buf.readBytes(allocate);
        return allocate;
    }

    static <B extends FriendlyByteBuf> StreamCodec<B, ArclightRawPayload> channelCodec(CustomPacketPayload.Type<ArclightRawPayload> type, int max) {
        return StreamCodec.composite(
                StreamCodec.of(FriendlyByteBuf::writeBytes, buf -> {
                    var size = buf.readableBytes();
                    Preconditions.checkArgument(size <= max, "Custom payload size may not be larger than " + max);
                    return buf.readRetainedSlice(size);
                }),
                RawPayload::arclight$getData,
                it -> new ArclightRawPayload(type, it)
        );
    }

    static <B extends FriendlyByteBuf> StreamCodec<B, CustomPacketPayload> discardedCodec(ResourceLocation location, int max) {
        return new StreamCodec<>() {
            @Override
            public DiscardedPayload decode(B buf) {
                int j = buf.readableBytes();
                if (j >= 0 && j <= max) {
                    var data = buf.readRetainedSlice(j);
                    var payload = new DiscardedPayload(location);
                    ((RawPayload)(Object)payload).arclight$setData(data);
                    return payload;
                } else {
                    throw new IllegalArgumentException("Payload may not be larger than " + max + " bytes");
                }
            }

            @Override
            public void encode(B buf, CustomPacketPayload obj) {
                if (obj instanceof RawPayload raw) {
                    buf.writeBytes(raw.arclight$getData());
                }
            }
        };
    }

}
