package kz.bejiihiu.candyriya.fabric.mod.plugin.messaging;

import kz.bejiihiu.candyriya.common.mod.plugin.messaging.CandyriyaRawPayload;
import kz.bejiihiu.candyriya.common.mod.plugin.messaging.PluginChannelHandler;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public interface FabricPayloadHandler extends PluginChannelHandler, ServerPlayNetworking.PlayPayloadHandler<CandyriyaRawPayload>, ServerConfigurationNetworking.ConfigurationPacketHandler<CandyriyaRawPayload> {}
