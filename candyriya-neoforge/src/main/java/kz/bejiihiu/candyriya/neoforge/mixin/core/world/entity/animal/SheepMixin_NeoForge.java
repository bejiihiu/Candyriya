package kz.bejiihiu.candyriya.neoforge.mixin.core.world.entity.animal;

import kz.bejiihiu.candyriya.common.mixin.core.world.entity.animal.AnimalMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.world.entity.animal.Sheep.class)
public abstract class SheepMixin_NeoForge extends AnimalMixin {
    @Inject(method = "shear", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Sheep;spawnAtLocation(Lnet/minecraft/world/level/ItemLike;I)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private void Candyriya$forceDrop(CallbackInfo ci) { forceDrops = true; }

    @Inject(method = "shear", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/animal/Sheep;spawnAtLocation(Lnet/minecraft/world/level/ItemLike;I)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private void Candyriya$forceDropReset(CallbackInfo ci) { forceDrops = false; }
}
