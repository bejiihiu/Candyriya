package kz.bejiihiu.candyriya.fabric.mod;

import kz.bejiihiu.candyriya.api.CandyriyaServer;
import kz.bejiihiu.candyriya.api.TickingTracker;
import kz.bejiihiu.candyriya.common.mod.server.api.DefaultTickingTracker;
import org.bukkit.plugin.Plugin;

public class FabricCandyriyaServer implements CandyriyaServer {

    private final TickingTracker tickingTracker = new DefaultTickingTracker();

    @Override
    public void registerForgeEvent(Plugin plugin, net.minecraftforge.eventbus.api.IEventBus eventBus, Object target) {
        registerModEvent(plugin, eventBus, target);
    }

    @Override
    public void registerModEvent(Plugin plugin, Object bus, Object target) {
        throw new UnsupportedOperationException("Not supported on Fabric");
    }

    @Override
    public TickingTracker getTickingTracker() {
        return this.tickingTracker;
    }
}
