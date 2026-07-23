package kz.bejiihiu.candyriya.common.mixin.core.world.effect;

import kz.bejiihiu.candyriya.common.bridge.core.server.level.ServerPlayerBridge;
import kz.bejiihiu.candyriya.common.mod.util.CandyriyaCaptures;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(MobEffectUtil.class)
public class MobEffectUtilMixin {

    @Decorate(method = "addEffectToPlayersAround", inject = true, at = @At(value = "INVOKE", remap = false, target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V"))
    private static void Candyriya$pushCause(@Local(ordinal = -1) List<ServerPlayer> players) {
        var cause = CandyriyaCaptures.getEffectCause();
        if (cause != null) {
            for (ServerPlayer player : players) {
                ((ServerPlayerBridge) player).bridge$pushEffectCause(cause);
            }
        }
    }
}
