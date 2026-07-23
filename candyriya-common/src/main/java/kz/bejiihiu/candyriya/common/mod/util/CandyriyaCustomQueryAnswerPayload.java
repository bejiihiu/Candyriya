package kz.bejiihiu.candyriya.common.mod.util;

import kz.bejiihiu.candyriya.common.mod.server.CandyriyaServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;
import net.minecraft.network.protocol.login.custom.DiscardedQueryAnswerPayload;

public record CandyriyaCustomQueryAnswerPayload(ByteBuf buf) implements CustomQueryAnswerPayload {

    @Override
    public void write(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeBytes(buf.slice());
    }

    public static CustomQueryAnswerPayload tryRead(FriendlyByteBuf buf, int max) {
        boolean hasPayload = buf.readBoolean();
        if (!hasPayload) {
            return DiscardedQueryAnswerPayload.INSTANCE;
        }
        int i = buf.readableBytes();
        if (i >= 0 && i < max) {
            var payload = Unpooled.buffer(i);
            buf.readBytes(payload);
            return new CandyriyaCustomQueryAnswerPayload(Unpooled.wrappedBuffer(Unpooled.copyBoolean(true), payload));
        } else {
            throw new IllegalArgumentException("Payload may not be larger than 1048576 bytes");
        }
    }

    public static FriendlyByteBuf tryUnwrap(CustomQueryAnswerPayload payload) {
        if (payload instanceof CandyriyaCustomQueryAnswerPayload(ByteBuf buf)) {
            // Fast path: we take control of it.
            return new FriendlyByteBuf(buf);
        } else if (payload == DiscardedQueryAnswerPayload.INSTANCE) {
            // Fast path: there is no data at all!
            return null;
        }
        CandyriyaServer.LOGGER.warn("Recreating the CustomQAPayload from an unknown payload, this usually means unexpected changes.");
        // Slow path: recreate data from the payload
        final var buf = new FriendlyByteBuf(Unpooled.buffer());
        payload.write(buf);
        return buf;
    }
}
