package kz.bejiihiu.candyriya.common.mixin.bukkit;

import kz.bejiihiu.candyriya.common.mod.server.world.inventory.CandyriyaInventoryView;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CraftInventoryView.class, remap = false)
public abstract class CraftInventoryViewMixin implements InventoryView {

    @Shadow @Final @Mutable private Inventory viewing;
    @Shadow @Final protected AbstractContainerMenu container;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void Candyriya$validate(HumanEntity player, Inventory viewing, AbstractContainerMenu container, CallbackInfo ci) {
        if (container.slots.size() > this.countSlots()) {
            this.viewing = CandyriyaInventoryView.createInv(((CraftHumanEntity) player).getHandle(), container);
        }
    }
}
