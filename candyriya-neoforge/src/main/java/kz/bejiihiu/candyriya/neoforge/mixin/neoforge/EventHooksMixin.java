package kz.bejiihiu.candyriya.neoforge.mixin.neoforge;

import kz.bejiihiu.candyriya.common.mod.util.CandyriyaCaptures;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(EventHooks.class)
public abstract class EventHooksMixin {
    @Inject(method = "onBlockPlace", remap = false, at = @At("HEAD"))
    private static void Candyriya$captureDirection(Entity entity, BlockSnapshot blockSnapshot, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        CandyriyaCaptures.capturePlaceEventDirection(direction);
    }

    @Inject(method = "onMultiBlockPlace", remap = false, at = @At("HEAD"))
    private static void Candyriya$captureDirection(Entity entity, List<BlockSnapshot> blockSnapshots, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        CandyriyaCaptures.capturePlaceEventDirection(direction);
    }
}
