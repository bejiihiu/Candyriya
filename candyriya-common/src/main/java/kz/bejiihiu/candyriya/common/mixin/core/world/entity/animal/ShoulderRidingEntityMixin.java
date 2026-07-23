package kz.bejiihiu.candyriya.common.mixin.core.world.entity.animal;

import kz.bejiihiu.candyriya.common.mixin.core.world.entity.TamableAnimalMixin;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.ShoulderRidingEntity;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShoulderRidingEntity.class)
public abstract class ShoulderRidingEntityMixin extends TamableAnimalMixin {

    @Inject(method = "setEntityOnShoulder", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/ShoulderRidingEntity;discard()V"))
    private void Candyriya$pickCause(ServerPlayer serverPlayer, CallbackInfoReturnable<Boolean> cir) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.PICKUP);
    }
}
