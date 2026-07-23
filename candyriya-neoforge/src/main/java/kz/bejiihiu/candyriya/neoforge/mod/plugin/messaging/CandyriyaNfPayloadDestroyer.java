package kz.bejiihiu.candyriya.neoforge.mod.plugin.messaging;

import kz.bejiihiu.candyriya.common.mod.plugin.messaging.CandyriyaPluginChannel;
import kz.bejiihiu.candyriya.common.mod.plugin.messaging.CandyriyaRawPayload;
import kz.bejiihiu.candyriya.common.mod.plugin.messaging.PayloadDestroyer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CandyriyaNfPayloadDestroyer(CandyriyaPluginChannel<CandyriyaNfPayloadDestroyer> channel) implements NeoforgePayloadHandler, PayloadDestroyer {
    @Override
    public void handle(CandyriyaRawPayload arg, IPayloadContext iPayloadContext) {}
}
