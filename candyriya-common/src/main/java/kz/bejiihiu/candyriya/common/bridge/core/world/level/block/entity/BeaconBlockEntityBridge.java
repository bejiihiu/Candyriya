package kz.bejiihiu.candyriya.common.bridge.core.world.level.block.entity;

import org.bukkit.potion.PotionEffect;

public interface BeaconBlockEntityBridge {

    PotionEffect bridge$getPrimaryEffect();

    PotionEffect bridge$getSecondaryEffect();
}
