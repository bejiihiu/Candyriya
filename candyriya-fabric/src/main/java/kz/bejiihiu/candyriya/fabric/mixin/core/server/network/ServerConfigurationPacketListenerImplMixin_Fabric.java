package kz.bejiihiu.candyriya.fabric.mixin.core.server.network;

import kz.bejiihiu.candyriya.common.mod.server.ArclightServer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerConfigurationPacketListenerImpl.class)
public abstract class ServerConfigurationPacketListenerImplMixin_Fabric {

    // @formatter:off
    @Shadow public abstract void startConfiguration();
    // @formatter:on

    @Inject(method = "startConfiguration", cancellable = true, at = @At("HEAD"))
    private void Candyriya$runOnMainThread(CallbackInfo ci) {
        if (!ArclightServer.isPrimaryThread()) {
            ArclightServer.executeOnMainThread(this::startConfiguration);
            ci.cancel();
        }
    }
}
