package kz.bejiihiu.candyriya.common.bridge.core.world.level;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;

import javax.annotation.Nullable;

public interface GameRules_ValueBridge<T extends GameRules.Value<T>> {
    void Candyriya$setFrom(T t, @Nullable ServerLevel level);
    void Candyriya$set(Object value, @Nullable ServerLevel level);
}
