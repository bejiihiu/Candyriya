package kz.bejiihiu.candyriya.neoforge.mod;

import kz.bejiihiu.candyriya.api.CandyriyaPlatform;
import kz.bejiihiu.candyriya.api.CandyriyaServer;
import kz.bejiihiu.candyriya.api.TickingTracker;
import kz.bejiihiu.candyriya.common.mod.server.api.DefaultTickingTracker;
import net.neoforged.bus.api.IEventBus;
import org.bukkit.plugin.Plugin;

public class NeoForgeCandyriyaServer implements CandyriyaServer {

    private final TickingTracker tickingTracker = new DefaultTickingTracker();

    @Override
    public void registerForgeEvent(Plugin plugin, net.minecraftforge.eventbus.api.IEventBus eventBus, Object target) {
        registerModEvent(plugin, eventBus, target);
    }

    @Override
    public void registerModEvent(Plugin plugin, Object bus, Object target) {
        try {
            if (bus instanceof IEventBus eventBus) {
                eventBus.register(target);
            } else {
                throw new IllegalArgumentException("Unknown bus type " + bus + " on platform " + CandyriyaPlatform.current());
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public TickingTracker getTickingTracker() {
        return this.tickingTracker;
    }
}
