package kz.bejiihiu.candyriya.common.bridge.core.world.level.levelgen.structure;

import org.bukkit.craftbukkit.v.persistence.CraftPersistentDataContainer;
import org.bukkit.event.world.AsyncStructureGenerateEvent;

public interface StructureStartBridge {

    void bridge$setGenerateCause(AsyncStructureGenerateEvent.Cause cause);

    CraftPersistentDataContainer bridge$getPersistentDataContainer();
}
