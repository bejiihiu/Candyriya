package kz.bejiihiu.candyriya.common.mod.server.world.inventory;

import com.google.common.base.Preconditions;
import kz.bejiihiu.candyriya.common.mod.server.ArclightServer;
import net.minecraft.world.Container;
import org.bukkit.craftbukkit.v.inventory.CraftInventory;
import org.bukkit.inventory.ItemStack;

public class CandyriyaModdedMenuInventory extends CraftInventory {
    public CandyriyaModdedMenuInventory(Container inventory) {
        super(inventory);
    }

    @Override
    public void setContents(ItemStack[] items) {
        ArclightServer.LOGGER.debug("Overriding content for a modded container menu inventory");
        super.setContents(items);
    }

    @Override
    public void clear() {
        ArclightServer.LOGGER.debug("Clearing everything for a modded container menu inventory");
        super.clear();
    }
}
