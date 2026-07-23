package kz.bejiihiu.candyriya.common.mixin.core.world.level.block;

import kz.bejiihiu.candyriya.common.mod.util.CandyriyaCaptures;
import kz.bejiihiu.candyriya.mixin.Decorate;
import kz.bejiihiu.candyriya.mixin.DecorationOps;
import kz.bejiihiu.candyriya.mixin.Eject;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.block.SculkVeinBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SculkVeinBlock.class)
public class SculkVeinBlockMixin {

    @Decorate(method = "attemptUseCharge", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/SculkVeinBlock;attemptPlaceSculk(Lnet/minecraft/world/level/block/SculkSpreader;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)Z"))
    private boolean Candyriya$captureSpreadPos(SculkVeinBlock instance, SculkSpreader spreader, LevelAccessor level, BlockPos pos, RandomSource random) throws Throwable {
        BlockPos old = null;
        try {
            old = CandyriyaCaptures.captureSpreadSource(pos);
            return (boolean) DecorationOps.callsite().invoke(instance, spreader, level, pos, random);
        } finally {
            CandyriyaCaptures.resetSpreadSource(old, pos);
        }
    }

    @Eject(method = "attemptPlaceSculk", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean Candyriya$blockSpread(LevelAccessor level, BlockPos pos, BlockState state, int i, CallbackInfoReturnable<Boolean> cir) {
        if (!CraftEventFactory.handleBlockSpreadEvent(level, CandyriyaCaptures.getSpreadPos(), pos, state, i)) {
            cir.setReturnValue(false);
            return false;
        }
        return true;
    }
}
