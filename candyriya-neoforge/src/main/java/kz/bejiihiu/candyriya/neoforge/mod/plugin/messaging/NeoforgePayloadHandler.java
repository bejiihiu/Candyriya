package kz.bejiihiu.candyriya.neoforge.mod.plugin.messaging;

import kz.bejiihiu.candyriya.common.mod.plugin.messaging.CandyriyaRawPayload;
import kz.bejiihiu.candyriya.common.mod.plugin.messaging.PluginChannelHandler;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public interface NeoforgePayloadHandler extends PluginChannelHandler, IPayloadHandler<CandyriyaRawPayload> {}
