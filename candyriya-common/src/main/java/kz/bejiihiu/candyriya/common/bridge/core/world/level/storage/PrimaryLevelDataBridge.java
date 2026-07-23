package kz.bejiihiu.candyriya.common.bridge.core.world.level.storage;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.dimension.LevelStem;

public interface PrimaryLevelDataBridge {

    void bridge$setWorld(ServerLevel world);

    ServerLevel bridge$getWorld();

    LevelSettings bridge$getWorldSettings();

    Lifecycle bridge$getLifecycle();

    void Candyriya$checkName(String name);

    void Candyriya$offerCustomDimensions(Registry<LevelStem> registry);
}
