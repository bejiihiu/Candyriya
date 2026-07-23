package kz.bejiihiu.candyriya.common.mixin.core.world.inventory;

import kz.bejiihiu.candyriya.common.bridge.core.world.entity.player.PlayerBridge;
import kz.bejiihiu.candyriya.common.bridge.core.world.inventory.TransientCraftingContainerBridge;
import kz.bejiihiu.candyriya.common.mod.util.CandyriyaCaptures;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultContainer;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryCrafting;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractContainerMenuMixin {

    // @formatter:off
    @Shadow @Final private CraftingContainer craftSlots;
    @Shadow @Final private ResultContainer resultSlots;
    // @formatter:on

    private CraftInventoryView<InventoryMenu, ?> bukkitEntity;
    private Inventory playerInventory;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void Candyriya$init(Inventory playerInventory, boolean localWorld, Player playerIn, CallbackInfo ci) {
        this.playerInventory = playerInventory;
        ((TransientCraftingContainerBridge) this.craftSlots).bridge$setOwner(playerInventory.player);
        ((TransientCraftingContainerBridge) this.craftSlots).bridge$setResultInventory(this.resultSlots);
        this.setTitle(Component.translatable("container.crafting"));
    }

    @Inject(method = "slotsChanged", at = @At("HEAD"))
    public void Candyriya$captureContainer(Container inventoryIn, CallbackInfo ci) {
        CandyriyaCaptures.captureWorkbenchContainer((AbstractContainerMenu) (Object) this);
    }

    @Override
    public CraftInventoryView<InventoryMenu, ?> getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }

        CraftInventoryCrafting inventory = new CraftInventoryCrafting(this.craftSlots, this.resultSlots);
        bukkitEntity = new CraftInventoryView<>(((PlayerBridge) this.playerInventory.player).bridge$getBukkitEntity(), inventory, (InventoryMenu) (Object) this);
        return bukkitEntity;
    }
}
