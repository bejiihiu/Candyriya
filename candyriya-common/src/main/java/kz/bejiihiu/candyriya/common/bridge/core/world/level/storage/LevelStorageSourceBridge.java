package kz.bejiihiu.candyriya.common.bridge.core.world.level.storage;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.validation.ContentValidationException;

import java.io.IOException;

public interface LevelStorageSourceBridge {

    LevelStorageSource.LevelStorageAccess Candyriya$validateAndCreateAccess(String saveName, ResourceKey<LevelStem> world) throws IOException, ContentValidationException;

    interface LevelStorageAccessBridge {

        void bridge$setDimType(ResourceKey<LevelStem> typeKey);

        ResourceKey<LevelStem> bridge$getTypeKey();
    }
}
