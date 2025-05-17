package io.izzel.arclight.common.mod.plugin.messaging;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ArclightRawPayload implements RawPayload {

    public static final Map<ResourceLocation, CustomPacketPayload.Type<ArclightRawPayload>> REGISTRY = new HashMap<>();

    public static CustomPacketPayload.Type<ArclightRawPayload> getType(ResourceLocation channel) {
        return REGISTRY.computeIfAbsent(channel, CustomPacketPayload.Type::new);
    }

    private final Type<ArclightRawPayload> type;
    private byte[] data;

    public ArclightRawPayload(CustomPacketPayload.Type<ArclightRawPayload> type, ByteBuf buf) {
        this(type, RawPayload.toBytes(buf));
    }

    public ArclightRawPayload(CustomPacketPayload.Type<ArclightRawPayload> type, byte[] raw) {
        Objects.requireNonNull(type, "type cannot be null");
        this.type = type;
        this.data = raw;
    }

    @Override
    public @NotNull Type<ArclightRawPayload> type() {
        return type;
    }

    @Override
    public byte[] arclight$getData() {
        return data;
    }

    @Override
    public void arclight$setData(byte[] data) {
        this.data = data;
    }
}
