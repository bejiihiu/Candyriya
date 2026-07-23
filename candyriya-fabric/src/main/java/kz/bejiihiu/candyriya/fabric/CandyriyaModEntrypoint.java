package kz.bejiihiu.candyriya.fabric;

import kz.bejiihiu.candyriya.api.Candyriya;
import kz.bejiihiu.candyriya.fabric.mod.FabricCandyriyaServer;
import kz.bejiihiu.candyriya.fabric.mod.event.EventHandlerRegistry;
import kz.bejiihiu.candyriya.fabric.mod.permission.CandyriyaPermissionImpl;
import net.fabricmc.api.ModInitializer;

public class CandyriyaModEntrypoint implements ModInitializer {

    @Override
    public void onInitialize() {
        Candyriya.setServer(new FabricCandyriyaServer());
        EventHandlerRegistry.register();
        CandyriyaPermissionImpl.init();
    }
}
