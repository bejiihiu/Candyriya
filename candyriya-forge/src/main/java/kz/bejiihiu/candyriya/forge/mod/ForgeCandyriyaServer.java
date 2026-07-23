package kz.bejiihiu.candyriya.forge.mod;

import kz.bejiihiu.candyriya.api.CandyriyaPlatform;
import kz.bejiihiu.candyriya.api.CandyriyaServer;
import kz.bejiihiu.candyriya.api.TickingTracker;
import kz.bejiihiu.candyriya.common.mod.server.api.DefaultTickingTracker;
import kz.bejiihiu.candyriya.forge.mod.util.PluginEventHandler;
import net.minecraftforge.eventbus.EventBus;
import net.minecraftforge.eventbus.api.IEventBus;
import org.bukkit.plugin.Plugin;

public class ForgeCandyriyaServer implements CandyriyaServer {

    private final TickingTracker tickingTracker = new DefaultTickingTracker();

    @Override
    public void registerForgeEvent(Plugin plugin, IEventBus bus, Object target) {
        registerModEvent(plugin, bus, target);
    }

    @Override
    public void registerModEvent(Plugin plugin, Object bus, Object target) {
        try {
            if (bus instanceof EventBus eventBus) {
                PluginEventHandler.register(plugin, eventBus, target);
            } else if (bus instanceof IEventBus eventBus) {
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
