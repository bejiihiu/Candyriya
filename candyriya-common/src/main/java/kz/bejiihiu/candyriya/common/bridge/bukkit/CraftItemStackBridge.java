package kz.bejiihiu.candyriya.common.bridge.bukkit;

import net.minecraft.world.entity.item.ItemEntity;

public interface CraftItemStackBridge {
    void Candyriya$setItemEntity(ItemEntity entity);
    ItemEntity Candyriya$getItemEntity();
}
