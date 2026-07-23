package kz.bejiihiu.candyriya.common.mixin.core.world.level.block;

import kz.bejiihiu.candyriya.common.bridge.core.world.level.LevelAccessorBridge;
import kz.bejiihiu.candyriya.common.mod.server.event.CandyriyaEventFactory;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.block.CraftBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LiquidBlock.class)
public class LiquidBlockMixin {

    private transient boolean Candyriya$fizz = true;

    @Decorate(method = "shouldSpreadLiquid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    public boolean Candyriya$blockForm(Level world, BlockPos pos, BlockState newState) throws Throwable {
        if (LevelAccessorBridge.from(world) instanceof LevelAccessorBridge bridge) {
            final var event = CandyriyaEventFactory.callBlockFormEvent(bridge.bridge$getMinecraftWorld(), pos, newState, 3, null);
            if (event != null) {
                if (event.isCancelled()) {
                    return false;
                }
                newState = ((CraftBlockState) event.getNewState()).getHandle();
            }
        }
        return Candyriya$fizz = (boolean) DecorationOps.callsite().invoke(world, pos, newState);
    }

    @Inject(method = "fizz", cancellable = true, at = @At("HEAD"))
    public void Candyriya$fizz(LevelAccessor worldIn, BlockPos pos, CallbackInfo ci) {
        if (!Candyriya$fizz) {
            ci.cancel();
        }
    }
}
