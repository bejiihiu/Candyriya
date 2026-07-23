package kz.bejiihiu.candyriya.common.bridge.core.world.inventory;

import kz.bejiihiu.candyriya.common.bridge.core.world.IInventoryBridge;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;

public interface TransientCraftingContainerBridge extends IInventoryBridge {

    void bridge$setOwner(Player owner);

    void bridge$setResultInventory(Container resultInventory);
}
