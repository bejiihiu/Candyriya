package kz.bejiihiu.candyriya.common.bridge.core.world.level;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;

import java.util.function.BiConsumer;

public interface GameRules_TypeBridge<T extends GameRules.Value<T>> {
    void Candyriya$setPerWorldCallback(BiConsumer<ServerLevel, T> callback);
    void Candyriya$runCallback(ServerLevel level, T value);
}
