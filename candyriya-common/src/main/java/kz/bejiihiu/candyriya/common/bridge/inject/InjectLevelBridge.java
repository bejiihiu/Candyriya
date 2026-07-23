package kz.bejiihiu.candyriya.common.bridge.inject;

import org.bukkit.craftbukkit.v.CraftWorld;

public interface InjectLevelBridge {

    default CraftWorld bridge$getWorld() {
        throw new IllegalStateException("Not implemented");
    }
}
