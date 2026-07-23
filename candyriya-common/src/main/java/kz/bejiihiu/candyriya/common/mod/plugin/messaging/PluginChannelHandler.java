package kz.bejiihiu.candyriya.common.mod.plugin.messaging;

import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;

public interface PluginChannelHandler {
    CandyriyaPluginChannel<?> channel();
    void updateChannel();
    void sendCustomPayload(Plugin src, CraftPlayer dst, byte[] data);
}
