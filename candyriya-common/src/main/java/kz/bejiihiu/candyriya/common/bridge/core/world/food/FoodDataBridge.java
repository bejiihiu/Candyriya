package kz.bejiihiu.candyriya.common.bridge.core.world.food;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface FoodDataBridge {

    void bridge$setEntityHuman(Player playerEntity);

    Player bridge$getEntityHuman();

    void bridge$pushEatStack(ItemStack stack);
}
