package kz.bejiihiu.candyriya.common.mixin.core.world.level;

import kz.bejiihiu.candyriya.common.bridge.core.world.level.LevelAccessorBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LevelAccessor.class)
public interface LevelAccessorMixin extends LevelAccessorBridge {

    default ServerLevel getMinecraftWorld() {
        return this.bridge$getMinecraftWorld();
    }
}
