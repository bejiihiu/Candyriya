package kz.bejiihiu.candyriya.neoforge.mixin.core.world.entity.ai.behavior;

import kz.bejiihiu.candyriya.common.mod.util.CandyriyaCaptures;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.HarvestFarmland;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HarvestFarmland.class)
public abstract class HarvestFarmlandMixin_NeoForge {

    @Inject(method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/Villager;J)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private void onSetBlock(ServerLevel worldIn, Villager owner, long gameTime, CallbackInfo ci) {
        CandyriyaCaptures.captureEntityChangeBlock(owner);
    }
}
