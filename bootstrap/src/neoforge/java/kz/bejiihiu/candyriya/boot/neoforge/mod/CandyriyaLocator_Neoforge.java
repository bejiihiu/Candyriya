package kz.bejiihiu.candyriya.boot.neoforge.mod;

import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;
import net.neoforged.neoforgespi.locating.IncompatibleFileReporting;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CandyriyaLocator_Neoforge implements IModFileCandidateLocator {

    private final Path Candyriya;

    public CandyriyaLocator_Neoforge() {
        ModBootstrap.run();
        this.Candyriya = loadJar();
    }

    protected Path loadJar() {
        var version = System.getProperty("Candyriya.version");
        return Paths.get(".Candyriya", "mod_file", version + ".jar");
    }

    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {
        pipeline.addPath(this.Candyriya, ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.WARN_ALWAYS);
    }

    @Override
    public String toString() {
        return "Candyriya";
    }
}
