package kz.bejiihiu.candyriya.fabric.mod.plugin.messaging;

import kz.bejiihiu.candyriya.common.bridge.core.server.level.ServerPlayerBridge;
import kz.bejiihiu.candyriya.common.bridge.core.server.network.ServerCommonPacketListenerImplBridge;
import kz.bejiihiu.candyriya.common.mod.plugin.messaging.CandyriyaPluginChannel;
import kz.bejiihiu.candyriya.common.mod.plugin.messaging.CandyriyaRawPayload;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;

public record CandyriyaFabricPayloadHandler(CandyriyaPluginChannel<CandyriyaFabricPayloadHandler> channel) implements FabricPayloadHandler {

    @Override
    public void updateChannel() {
    }

    @Override
    public void receive(CandyriyaRawPayload pkt, ServerPlayNetworking.Context ctx) {
        ctx.server().executeIfPossible(() -> {
            var bukkit = ((ServerPlayerBridge)ctx.player()).bridge$getBukkitEntity();
            channel.dispatchMessage(bukkit, pkt.Candyriya$leak());
        });
    }

    @Override
    public void receive(CandyriyaRawPayload pkt, ServerConfigurationNetworking.Context ctx) {
        ctx.server().executeIfPossible(() -> {
            var bukkit = ((ServerCommonPacketListenerImplBridge)ctx.networkHandler()).bridge$getCraftPlayer();
            channel.dispatchMessage(bukkit, pkt.Candyriya$leak());
        });
    }

    @Override
    public void sendCustomPayload(Plugin src, CraftPlayer dst, byte[] data) {
        var player = dst.getHandle();
        if (player.connection != null) {
            ServerPlayNetworking.send(player, new CandyriyaRawPayload(channel.getType(), data));
        }
    }
}
