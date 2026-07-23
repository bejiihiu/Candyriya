package kz.bejiihiu.candyriya.boot.neoforge.mod;

import net.neoforged.fml.loading.targets.NeoForgeServerLaunchHandler;

public class CandyriyaLaunchHandler extends NeoForgeServerLaunchHandler {

    @Override
    public String name() {
        return "Candyriyaserver";
    }

    @Override
    protected String[] preLaunch(String[] arguments, ModuleLayer layer) {
        // skip the log4j configuration reloading
        return arguments;
    }
}
