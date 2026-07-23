package kz.bejiihiu.candyriya.boot.forge.mod;

import net.minecraftforge.fml.loading.targets.CommonLaunchHandler;

import java.nio.file.Path;
import java.util.List;

// duplicate of ForgeProdLaunchHandler, change on update
public class CandyriyaLaunchHandler extends CommonLaunchHandler {

    public CandyriyaLaunchHandler() {
        super(CommonLaunchHandler.SERVER, "Candyriya_");
    }

    @Override
    public String getNaming() {
        // Target mapping in use is SRG -> MCP, translates to MCP
        return "mcp";
    }

    @Override
    public boolean isProduction() {
        return true;
    }

    @Override
    public List<Path> getMinecraftPaths() {
        return List.of(CommonLaunchHandler.getPathFromResource("net/minecraft/server/MinecraftServer.class"));
    }

    @Override
    protected String[] preLaunch(String[] arguments, ModuleLayer layer) {
        // skip the log4j configuration reloading
        return arguments;
    }
}
