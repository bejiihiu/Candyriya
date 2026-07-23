package kz.bejiihiu.candyriya.common.mod.plugin.messaging;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CandyriyaRawPayload implements RawPayload {

    public static final Map<ResourceLocation, CustomPacketPayload.Type<CandyriyaRawPayload>> REGISTRY = new HashMap<>();

    public static CustomPacketPayload.Type<CandyriyaRawPayload> getType(ResourceLocation channel) {
        return REGISTRY.computeIfAbsent(channel, CustomPacketPayload.Type::new);
    }

    private final Type<CandyriyaRawPayload> type;
    private ByteBuf data;

    public CandyriyaRawPayload(CustomPacketPayload.Type<CandyriyaRawPayload> type, ByteBuf raw) {
        Objects.requireNonNull(type, "type cannot be null");
        this.type = type;
        this.data = raw;
    }

    public CandyriyaRawPayload(CustomPacketPayload.Type<CandyriyaRawPayload> type, byte[] raw) {
        this(type, Unpooled.wrappedBuffer(raw));
    }

    @Override
    public Type<CandyriyaRawPayload> type() {
        return type;
    }

    @Override
    public ByteBuf Candyriya$getRawData() {
        return data;
    }

    @Override
    public void Candyriya$setData(ByteBuf data) {
        this.data = data;
    }
}
