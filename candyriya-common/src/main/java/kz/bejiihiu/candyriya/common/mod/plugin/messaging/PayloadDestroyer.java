package kz.bejiihiu.candyriya.common.mod.plugin.messaging;

import kz.bejiihiu.candyriya.common.mod.server.CandyriyaServer;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;

public interface PayloadDestroyer extends PluginChannelHandler {

    default void sendCustomPayload(Plugin src, CraftPlayer dst, byte[] data) {
        CandyriyaServer.LOGGER.debug("Ignoring sendCustomPayload for channel {} due to conflict with mod channel.", channel().getChannel());
    }

    @Override
    default void updateChannel() {}
}
