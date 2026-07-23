package kz.bejiihiu.candyriya.neoforge.mod.event;

import kz.bejiihiu.candyriya.common.mod.server.CandyriyaServer;
import net.neoforged.neoforge.common.NeoForge;

public abstract class CandyriyaEventDispatcherRegistry {

    public static void registerAllEventDispatchers() {
        NeoForge.EVENT_BUS.register(new BlockBreakEventDispatcher());
        NeoForge.EVENT_BUS.register(new BlockPlaceEventDispatcher());
        NeoForge.EVENT_BUS.register(new EntityEventDispatcher());
        NeoForge.EVENT_BUS.register(new EntityTeleportEventDispatcher());
        NeoForge.EVENT_BUS.register(new ItemEntityEventDispatcher());
        CandyriyaServer.LOGGER.info("registry.forge-event");
    }
}
