package kz.bejiihiu.candyriya.common.mod.server.world.inventory;

import kz.bejiihiu.candyriya.common.bridge.core.world.entity.player.PlayerBridge;
import kz.bejiihiu.candyriya.common.mod.util.CandyriyaCaptures;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.bukkit.craftbukkit.v.inventory.CraftInventory;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

public class CandyriyaInventoryView {

    /*
     * Treat all modded containers not having a "bottom" inventory.
     */
    public static InventoryView createInvView(AbstractContainerMenu container) {
        var containerOwner = CandyriyaCaptures.getContainerOwner();
        Inventory viewing = createInv(containerOwner, container);
        return new CraftInventoryView<>(((PlayerBridge) containerOwner).bridge$getBukkitEntity(), viewing, container);
    }

    public static CraftInventory createInv(Player containerOwner, AbstractContainerMenu container) {
        return new CandyriyaModdedMenuInventory(new CandyriyaModdedMenuContainer(container, containerOwner));
    }

    public static SimpleContainer copyOf(SimpleContainer container) {
        var copy = new SimpleContainer(container.getContainerSize());
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            copy.items.set(slot, container.items.get(slot).copy());
        }
        return copy;
    }
}
