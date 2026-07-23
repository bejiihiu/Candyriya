package kz.bejiihiu.candyriya.fabric.mod.event;

import kz.bejiihiu.candyriya.common.mod.server.event.EntityEventHandler;

public class EventHandlerRegistry {
    public static void register() {
        LivingDropsEvent.EVENT.register(EntityEventHandler::monitorLivingDrops);
        S2CPlayNConfigChannelHandler.register();
    }
}
