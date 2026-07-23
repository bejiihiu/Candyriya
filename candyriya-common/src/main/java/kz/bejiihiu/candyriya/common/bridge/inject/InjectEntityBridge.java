package kz.bejiihiu.candyriya.common.bridge.inject;

import kz.bejiihiu.candyriya.common.bridge.core.entity.EntityBridge;
import org.bukkit.craftbukkit.v.entity.CraftEntity;

public interface InjectEntityBridge {

    default CraftEntity bridge$getBukkitEntity() {
        throw new IllegalStateException("Not implemented");
    }

    default EntityBridge bridge() {
        return (EntityBridge) this;
    }
}
