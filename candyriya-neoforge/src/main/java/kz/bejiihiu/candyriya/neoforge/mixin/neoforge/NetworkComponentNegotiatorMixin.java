package kz.bejiihiu.candyriya.neoforge.mixin.neoforge;

import kz.bejiihiu.candyriya.neoforge.mod.plugin.messaging.CandyriyaNfMessaging;
import net.neoforged.neoforge.network.negotiation.NetworkComponentNegotiator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(value = NetworkComponentNegotiator.class, remap = false)
public abstract class NetworkComponentNegotiatorMixin {
    @SuppressWarnings("StringEquality")
    @Redirect(
            method = "validateComponent",
            slice = @Slice(
                    from = @At(
                            value = "INVOKE",
                            ordinal = 1,
                            target = "Lnet/neoforged/neoforge/network/negotiation/NegotiableNetworkComponent;version()Ljava/lang/String;"
                    ),
                    to = @At(
                            value = "INVOKE",
                            ordinal = 2,
                            target = "Lnet/neoforged/neoforge/network/negotiation/NegotiableNetworkComponent;version()Ljava/lang/String;"
                    )
            ),
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z"
            )
    )
    private static boolean Candyriya$bypassValidation(String instance, Object o) {
        if (instance == CandyriyaNfMessaging.Candyriya_CUSTOM_CHANNEL_VERSION || o == CandyriyaNfMessaging.Candyriya_CUSTOM_CHANNEL_VERSION) {
            return true;
        }
        return instance.equals(o);
    }
}
