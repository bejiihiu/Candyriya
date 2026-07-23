package kz.bejiihiu.candyriya.common.mod.server.world;

import kz.bejiihiu.candyriya.common.mixin.core.world.level.LevelMixin;
import org.spigotmc.SpigotWorldConfig;

public class CandyriyaWorldConfig {

    /**
     * Use as a marker world name. We don't want to put trash output in terminal
     * only for reading a default world config.
     * @see kz.bejiihiu.candyriya.common.mixin.bukkit.SpigotWorldConfigMixin#Candyriya$skipLog(String)
     */
    @SuppressWarnings({"StringOperationCanBeSimplified", "JavadocReference"})
    public static final String DEFAULT_MARKER = new String("default");

    /**
     * Default world config. Used for logic world.
     * @see LevelMixin#bridge$spigotConfig()
     */
    public static final SpigotWorldConfig DEFAULT = new SpigotWorldConfig(DEFAULT_MARKER);

    private CandyriyaWorldConfig() {}
}
