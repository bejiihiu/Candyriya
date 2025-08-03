package io.izzel.arclight.common.bridge.bukkit;

import org.bukkit.plugin.PluginDescriptionFile;

public interface PluginClassLoaderBridge {
    PluginDescriptionFile arclight$desc();
    Class<?> arclight$loadFromExternal(String name, boolean initialize, boolean checkLibraries) throws ClassNotFoundException;
}
