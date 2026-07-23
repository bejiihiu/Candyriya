package kz.bejiihiu.candyriya.common.mod;

import io.izzel.arclight.api.ArclightPlatform;
import kz.bejiihiu.candyriya.common.mod.util.log.CandyriyaI18nLogger;
import io.izzel.arclight.mixin.MixinTools;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

public class CandyriyaConnector implements IMixinConnector {

    public static final Logger LOGGER = CandyriyaI18nLogger.getLogger("Candyriya");

    @Override
    public void connect() {
        MixinTools.setup();
        Mixins.addConfiguration("mixins.Candyriya.core.json");
        Mixins.addConfiguration("mixins.Candyriya.bukkit.json");
        switch (ArclightPlatform.current()) {
            case VANILLA -> Mixins.addConfiguration("mixins.Candyriya.vanilla.json");
            case FORGE -> Mixins.addConfiguration("mixins.Candyriya.forge.json");
            case NEOFORGE -> Mixins.addConfiguration("mixins.Candyriya.neoforge.json");
        }
        LOGGER.info("mixin-load.core");
        Mixins.addConfiguration("mixins.Candyriya.impl.optimization.json");
        LOGGER.info("mixin-load.optimization");
    }
}
