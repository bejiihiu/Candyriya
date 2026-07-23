package kz.bejiihiu.candyriya.common.mixin.core.world.level;

import kz.bejiihiu.candyriya.common.bridge.core.world.level.GameRules_TypeBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.BiConsumer;

@Mixin(GameRules.Type.class)
public class GameRules_TypeMixin<T extends GameRules.Value<T>> implements GameRules_TypeBridge<T> {

    @Unique
    private BiConsumer<ServerLevel, T> Candyriya$perWorldCallback;

    @Override
    public void Candyriya$setPerWorldCallback(BiConsumer<ServerLevel, T> callback) {
        Candyriya$perWorldCallback = callback;
    }

    @Override
    public void Candyriya$runCallback(ServerLevel level, T value) {
        if (Candyriya$perWorldCallback != null) {
            Candyriya$perWorldCallback.accept(level, value);
        }
    }
}
