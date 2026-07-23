package kz.bejiihiu.candyriya.forge.mod.plugin.messaging;

import kz.bejiihiu.candyriya.common.mod.plugin.messaging.PluginChannelHandler;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Consumer;

public interface ForgePayloadHandler extends PluginChannelHandler, Consumer<CustomPayloadEvent> {}
