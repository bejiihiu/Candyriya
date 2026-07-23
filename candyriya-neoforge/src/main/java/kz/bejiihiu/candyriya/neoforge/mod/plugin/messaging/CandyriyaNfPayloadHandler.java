package kz.bejiihiu.candyriya.neoforge.mod.plugin.messaging;

import kz.bejiihiu.candyriya.common.bridge.core.server.level.ServerPlayerBridge;
import kz.bejiihiu.candyriya.common.mod.plugin.messaging.CandyriyaPluginChannel;
import kz.bejiihiu.candyriya.common.mod.plugin.messaging.CandyriyaRawPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;

public record CandyriyaNfPayloadHandler(CandyriyaPluginChannel<CandyriyaNfPayloadHandler> channel) implements NeoforgePayloadHandler {
    @Override
    public void handle(CandyriyaRawPayload pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var bukkit = ((ServerPlayerBridge)ctx.player()).bridge$getBukkitEntity();
            channel.dispatchMessage(bukkit, pkt.Candyriya$leak());
        });
    }

    @Override
    public void updateChannel() {
        CandyriyaNfMessaging.updateChannel(channel);
    }

    @Override
    public void sendCustomPayload(Plugin src, CraftPlayer dst, byte[] data) {
        PacketDistributor.sendToPlayer(dst.getHandle(), new CandyriyaRawPayload(channel.getType(), data));
    }
}
