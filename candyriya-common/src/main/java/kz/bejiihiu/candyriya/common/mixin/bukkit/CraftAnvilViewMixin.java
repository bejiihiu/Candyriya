package kz.bejiihiu.candyriya.common.mixin.bukkit;

import kz.bejiihiu.candyriya.common.bridge.core.world.inventory.AnvilMenuBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import org.bukkit.craftbukkit.v.inventory.view.CraftAnvilView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CraftAnvilView.class, remap = false)
public abstract class CraftAnvilViewMixin extends CraftInventoryViewMixin {

    @Decorate(method = "setRepairCost", at = @At("HEAD"), inject = true)
    private void Candyriya$handleZeroCost(int cost) throws Throwable {
        if (cost == 0) {
            ((AnvilMenuBridge) this.container).Candyriya$allowZeroCost();
        } else if (cost == -1) {
            cost = 0;
        }
        DecorationOps.blackhole().invoke(cost);
    }

    @Inject(method = "getRepairCost", at = @At("RETURN"), cancellable = true)
    private void Candyriya$translateToNegativeCost(CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() == 0) {
            final boolean allowed = ((AnvilMenuBridge) this.container).Candyriya$isZeroCostAllowed();
            if (!allowed) {
                cir.setReturnValue(-1);
            }
        }
    }
}
