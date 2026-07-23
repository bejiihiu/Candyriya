package kz.bejiihiu.candyriya.common.bridge.bukkit;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.SimplePluginManager;

import java.util.logging.Logger;

public interface PluginClassLoaderBridge {
    PluginDescriptionFile Candyriya$desc();
    Class<?> Candyriya$loadFromExternal(String name, boolean initialize, boolean checkLibraries) throws ClassNotFoundException;
    SimplePluginManager Candyriya$getPluginManager();
    Logger Candyriya$systemLogger();
}
