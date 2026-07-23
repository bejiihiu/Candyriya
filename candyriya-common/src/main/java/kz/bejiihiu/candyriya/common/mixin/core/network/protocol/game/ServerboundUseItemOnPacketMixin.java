package kz.bejiihiu.candyriya.common.mixin.core.network.protocol.game;

import kz.bejiihiu.candyriya.common.bridge.core.network.play.TimestampedPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerboundUseItemOnPacket.class)
public class ServerboundUseItemOnPacketMixin implements TimestampedPacket {

    public long timestamp;

    @Inject(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At("RETURN"))
    private void Candyriya$read(FriendlyByteBuf buf, CallbackInfo ci) {
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public long bridge$timestamp() {
        return timestamp;
    }
}
