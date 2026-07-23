package kz.bejiihiu.candyriya.common.bridge.bukkit;


import org.bukkit.Server;
import org.bukkit.plugin.PluginDescriptionFile;

import java.net.URLClassLoader;
import java.util.List;

public interface JavaPluginLoaderBridge {

    Server Candyriya$server();

    <T extends URLClassLoader & PluginClassLoaderBridge> List<T> Candyriya$getLoaders();

    void bridge$setClass(final String name, final Class<?> clazz);

    Class<?> Candyriya$getClassByName(String name, boolean resolve, PluginDescriptionFile description);
}
