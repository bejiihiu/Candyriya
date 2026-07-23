package kz.bejiihiu.candyriya.common.mixin.bukkit.plugin;

import kz.bejiihiu.candyriya.common.mod.util.log.CandyriyaPluginLogger;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(JavaPlugin.class)
public class JavaPluginMixin {

    @Redirect(method = "init", remap = false, at = @At(value = "NEW", target = "(Lorg/bukkit/plugin/Plugin;)Lorg/bukkit/plugin/PluginLogger;"))
    private PluginLogger Candyriya$createLogger(Plugin plugin) {
        return new CandyriyaPluginLogger(plugin);
    }

}
