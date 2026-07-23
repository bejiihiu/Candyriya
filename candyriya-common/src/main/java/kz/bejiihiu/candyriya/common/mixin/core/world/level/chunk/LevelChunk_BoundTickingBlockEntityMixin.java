package kz.bejiihiu.candyriya.common.mixin.core.world.level.chunk;

import kz.bejiihiu.candyriya.common.mod.util.CandyriyaCaptures;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net/minecraft/world/level/chunk/LevelChunk$BoundTickingBlockEntity")
public class LevelChunk_BoundTickingBlockEntityMixin<T extends BlockEntity> {

    @Shadow @Final private T blockEntity;

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntityTicker;tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)V"))
    private void Candyriya$captureBlockEntity(CallbackInfo ci) {
        CandyriyaCaptures.captureTickingBlockEntity(this.blockEntity);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/level/block/entity/BlockEntityTicker;tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)V"))
    private void Candyriya$resetBlockEntity(CallbackInfo ci) {
        CandyriyaCaptures.resetTickingBlockEntity();
    }
}
