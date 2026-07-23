package kz.bejiihiu.candyriya.fabric;

import io.izzel.arclight.api.Arclight;
import kz.bejiihiu.candyriya.fabric.mod.FabricArclightServer;
import kz.bejiihiu.candyriya.fabric.mod.event.EventHandlerRegistry;
import kz.bejiihiu.candyriya.fabric.mod.permission.CandyriyaPermissionImpl;
import net.fabricmc.api.ModInitializer;

public class CandyriyaModEntrypoint implements ModInitializer {

    @Override
    public void onInitialize() {
        Candyriya.setServer(new FabricArclightServer());
        EventHandlerRegistry.register();
        CandyriyaPermissionImpl.init();
    }
}
