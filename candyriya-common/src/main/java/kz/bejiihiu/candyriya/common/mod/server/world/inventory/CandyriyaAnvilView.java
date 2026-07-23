package kz.bejiihiu.candyriya.common.mod.server.world.inventory;

import net.minecraft.world.inventory.AnvilMenu;
import org.bukkit.craftbukkit.v.inventory.view.CraftAnvilView;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.AnvilInventory;

public class CandyriyaAnvilView extends CraftAnvilView {
    public CandyriyaAnvilView(HumanEntity player, AnvilInventory viewing, AnvilMenu container) {
        super(player, viewing, container);
    }
}
