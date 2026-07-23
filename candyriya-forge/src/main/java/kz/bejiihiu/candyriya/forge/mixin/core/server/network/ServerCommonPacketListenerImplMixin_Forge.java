package kz.bejiihiu.candyriya.forge.mixin.core.server.network;

import kz.bejiihiu.candyriya.common.bridge.core.server.network.ServerCommonPacketListenerImplBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin_Forge implements ServerCommonPacketListenerImplBridge {

}
