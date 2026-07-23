package kz.bejiihiu.candyriya.common.bridge.core.server.network;

import com.mojang.authlib.GameProfile;
import kz.bejiihiu.candyriya.common.mod.util.CandyriyaCustomQueryAnswerPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;

public interface ServerLoginPacketListenerImplBridge {
    default Thread bridge$newHandleThread(String name, Runnable runnable) {
        return new Thread(runnable, name);
    }

    int bridge$getVelocityLoginId();

    void bridge$preLogin(GameProfile authenticatedProfile) throws Exception;

    void bridge$disconnect(String reason);

    default FriendlyByteBuf Candyriya$platform$customQAData(ServerboundCustomQueryAnswerPacket packet) {
        return CandyriyaCustomQueryAnswerPayload.tryUnwrap(packet.payload());
    }

    default void Candyriya$platform$onCustomQA(ServerboundCustomQueryAnswerPacket payload) {}
}
