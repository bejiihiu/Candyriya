package kz.bejiihiu.candyriya.common.bridge.core.server.dedicated;

import net.minecraft.core.RegistryAccess;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;

public interface DedicatedServerBridge {
    default void bridge$platform$exitNow() {
    }

    WorldLoader.DataLoadContext Candyriya$dataLoadContext();

    void Candyriya$forceUpgradeIfNeeded(LevelStorageSource.LevelStorageAccess worldSession, RegistryAccess.Frozen dimensions);

    void Candyriya$prepareAndAddLevel(ServerLevel level, PrimaryLevelData levelData);
}
