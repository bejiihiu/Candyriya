package kz.bejiihiu.candyriya.common.mod.server.world.inventory;

import net.minecraft.world.inventory.GrindstoneMenu;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryGrindstone;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.bukkit.entity.HumanEntity;

public class CandyriyaGrindstoneView extends CraftInventoryView<GrindstoneMenu, CraftInventoryGrindstone> {
    public CandyriyaGrindstoneView(HumanEntity player, CraftInventoryGrindstone viewing, GrindstoneMenu container) {
        super(player, viewing, container);
    }
}
