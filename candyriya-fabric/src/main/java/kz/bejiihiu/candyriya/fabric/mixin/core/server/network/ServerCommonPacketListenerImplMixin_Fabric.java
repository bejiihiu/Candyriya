package kz.bejiihiu.candyriya.fabric.mixin.core.server.network;

import kz.bejiihiu.candyriya.common.bridge.bukkit.MessengerBridge;
import kz.bejiihiu.candyriya.common.bridge.core.server.network.ServerCommonPacketListenerImplBridge;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.bukkit.Bukkit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin_Fabric implements ServerCommonPacketListenerImplBridge {

    @Inject(method = "handleCustomPayload", at = @At("TAIL"))
    private void Candyriya$handleUnknownPayload(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        var recorder = ((MessengerBridge) Bukkit.getMessenger()).Candyriya$getPacketRecorder();
        recorder.recordUnknown(packet.payload().type().id());
        recorder.update();
    }
}
