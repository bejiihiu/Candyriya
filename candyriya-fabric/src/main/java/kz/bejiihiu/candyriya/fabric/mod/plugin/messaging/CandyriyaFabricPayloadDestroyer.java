package kz.bejiihiu.candyriya.fabric.mod.plugin.messaging;

import kz.bejiihiu.candyriya.common.mod.plugin.messaging.CandyriyaPluginChannel;
import kz.bejiihiu.candyriya.common.mod.plugin.messaging.CandyriyaRawPayload;
import kz.bejiihiu.candyriya.common.mod.plugin.messaging.PayloadDestroyer;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public record CandyriyaFabricPayloadDestroyer(CandyriyaPluginChannel<CandyriyaFabricPayloadDestroyer> channel) implements FabricPayloadHandler, PayloadDestroyer {

    @Override
    public void receive(CandyriyaRawPayload payload, ServerPlayNetworking.Context context) {}

    @Override
    public void receive(CandyriyaRawPayload payload, ServerConfigurationNetworking.Context context) {}
}
