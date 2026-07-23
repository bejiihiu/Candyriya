package kz.bejiihiu.candyriya.common.mixin.core.world.entity.ai.village;

import kz.bejiihiu.candyriya.common.bridge.core.server.level.ServerLevelBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.VillageSiege;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillageSiege.class)
public class VillageSiegeMixin {

    @Inject(method = "trySpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    public void Candyriya$addEntityReason(ServerLevel world, CallbackInfo ci) {
        ((ServerLevelBridge) world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.VILLAGE_INVASION);
    }
}
