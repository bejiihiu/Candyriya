package kz.bejiihiu.candyriya.common.mixin.core.world.level.block;

import kz.bejiihiu.candyriya.common.bridge.core.world.entity.MobBridge;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.BeehiveBlock;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BeehiveBlock.class)
public class BeehiveBlockMixin {

    @Redirect(method = "angerNearbyBees", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Bee;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void Candyriya$targetReason(Bee beeEntity, LivingEntity livingEntity) {
        ((MobBridge) beeEntity).bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.CLOSEST_PLAYER, true);
        beeEntity.setTarget(livingEntity);
    }
}
