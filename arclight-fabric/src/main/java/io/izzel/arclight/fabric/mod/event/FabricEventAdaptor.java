package io.izzel.arclight.fabric.mod.event;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.world.InteractionResult;

import java.util.function.Function;

public class FabricEventAdaptor {

    public static <T> Function<T[], T> monitored(Class<? super T> type, Function<T[], T> factory) {
        if (AttackBlockCallback.class == type) {
            return (listeners) -> (T) monitorAttackBlock((AttackBlockCallback) factory.apply(listeners));
        } else {
            return factory;
        }
    }

    private static AttackBlockCallback monitorAttackBlock(AttackBlockCallback callback) {
        return (player, world, hand, pos, direction) -> {
            final var result = callback.interact(player, world, hand, pos, direction);
            if (result != InteractionResult.PASS) {
                ArclightFabricEventFactory.callCancelledPlayerInteract(player, pos, direction, hand);
            }
            return result;
        };
    }
}
