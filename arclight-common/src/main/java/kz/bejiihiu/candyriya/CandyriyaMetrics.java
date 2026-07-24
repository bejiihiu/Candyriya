package kz.bejiihiu.candyriya;

import io.izzel.arclight.api.ArclightPlatform;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import kz.bejiihiu.candyriya.bstats.Metrics;
import org.bukkit.Bukkit;

/**
 * bStats metrics initialization for Candyriya.
 * Collects server implementation statistics: version, mod loader, player count, etc.
 *
 * @see <a href="https://bstats.org/plugin/server-implementation/Candyriya/32849">bStats dashboard</a>
 */
public final class CandyriyaMetrics {

    private static final int PLUGIN_ID = 32849;

    private CandyriyaMetrics() {
    }

    /**
     * Initializes bStats metrics. Called from ArclightServer.createOrLoad().
     */
    public static void init() {
        try {
            Metrics metrics = Metrics.loadFromConfig(Brand.NAME);
            if (metrics == null) {
                return;
            }

            // Candyriya start - bstats charts
            metrics.addCustomChart(new Metrics.SimplePie("candyriya_version", () ->
                System.getProperty("arclight.version", "unknown")
            ));

            metrics.addCustomChart(new Metrics.SimplePie("minecraft_version", () ->
                Bukkit.getBukkitVersion().split("-")[0]
            ));

            metrics.addCustomChart(new Metrics.SimplePie("mod_loader", () -> {
                switch (ArclightPlatform.current()) {
                    case FORGE: return "Forge";
                    case NEOFORGE: return "NeoForge";
                    case FABRIC: return "Fabric";
                    default: return "Unknown";
                }
            }));

            metrics.addCustomChart(new Metrics.SingleLineChart("players", () ->
                Bukkit.getOnlinePlayers().size()
            ));

            metrics.addCustomChart(new Metrics.SimplePie("online_mode", () ->
                Bukkit.getOnlineMode() ? "online" : "offline"
            ));

            metrics.addCustomChart(new Metrics.DrilldownPie("java_version", () -> {
                Map<String, Map<String, Integer>> map = new HashMap<>();
                String javaVersion = System.getProperty("java.version");
                Map<String, Integer> entry = new HashMap<>();
                entry.put(javaVersion, 1);

                if (javaVersion.startsWith("21")) {
                    map.put("Java 21", entry);
                } else if (javaVersion.startsWith("22")) {
                    map.put("Java 22", entry);
                } else if (javaVersion.startsWith("17")) {
                    map.put("Java 17", entry);
                } else {
                    map.put("Other", entry);
                }
                return map;
            }));

            metrics.addCustomChart(new Metrics.SingleLineChart("bukkit_plugin_count", () ->
                Bukkit.getPluginManager().getPlugins().length
            ));
            // Candyriya end

            ArclightServer.LOGGER.info("bStats metrics initialized");
        } catch (Exception e) {
            ArclightServer.LOGGER.warn("Failed to initialize bStats metrics", e);
        }
    }
}
