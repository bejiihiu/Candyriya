package kz.bejiihiu.candyriya.common.bridge.core.world.entity.animal;

import kz.bejiihiu.candyriya.common.bridge.core.world.entity.MobBridge;
import net.minecraft.world.item.ItemStack;

public interface AnimalBridge extends MobBridge {

    ItemStack bridge$getBreedItem();
}
