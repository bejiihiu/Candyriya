package kz.bejiihiu.candyriya.common.mixin.core.world.inventory;

import kz.bejiihiu.candyriya.common.bridge.core.world.entity.player.PlayerBridge;
import kz.bejiihiu.candyriya.common.bridge.core.world.inventory.ContainerLevelAccessBridge;
import kz.bejiihiu.candyriya.common.mod.server.world.inventory.CandyriyaSmithingView;
import kz.bejiihiu.candyriya.mixin.Decorate;
import kz.bejiihiu.candyriya.mixin.DecorationOps;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftInventorySmithing;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin extends ItemCombinerMenuMixin {

    private CraftInventoryView<SmithingMenu, ?> bukkitEntity;

    @Decorate(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/ResultContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V"))
    private void Candyriya$prepareSmithing(ResultContainer craftResultInventory, int index, ItemStack stack) throws Throwable {
        final CraftInventoryView<SmithingMenu, ?> craft = getBukkitView();
        if (craft instanceof CandyriyaSmithingView) {
            // Call preparing event; preserve injection logic
            PrepareSmithingEvent event = new PrepareSmithingEvent(craft, CraftItemStack.asCraftMirror(stack).clone());
            Bukkit.getServer().getPluginManager().callEvent(event);
            DecorationOps.callsite().invoke(craftResultInventory, index, CraftItemStack.asNMSCopy(event.getResult()));
        } else {
            // Run plugin custom logic
            CraftEventFactory.callPrepareSmithingEvent(craft, stack);
        }
    }

    @Override
    public CraftInventoryView<SmithingMenu, ?> getBukkitView() {
        if (this.bukkitEntity != null) {
            return this.bukkitEntity;
        }
        CraftInventorySmithing inventory = new CraftInventorySmithing(((ContainerLevelAccessBridge) this.access).bridge$getLocation(), this.inputSlots, this.resultSlots);
        return this.bukkitEntity = new CandyriyaSmithingView(((PlayerBridge) this.player).bridge$getBukkitEntity(), inventory, (SmithingMenu) (Object) this);
    }
}
