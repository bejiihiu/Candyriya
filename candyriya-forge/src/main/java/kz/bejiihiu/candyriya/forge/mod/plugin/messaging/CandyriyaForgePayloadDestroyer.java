package kz.bejiihiu.candyriya.forge.mod.plugin.messaging;

import kz.bejiihiu.candyriya.common.mod.plugin.messaging.CandyriyaPluginChannel;
import kz.bejiihiu.candyriya.common.mod.plugin.messaging.CandyriyaRawPayload;
import kz.bejiihiu.candyriya.common.mod.plugin.messaging.PayloadDestroyer;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record CandyriyaForgePayloadDestroyer(CandyriyaPluginChannel<CandyriyaForgePayloadDestroyer> channel) implements ForgePayloadHandler, PayloadDestroyer {

    @Override
    public void accept(CustomPayloadEvent customPayloadEvent) {}
}
