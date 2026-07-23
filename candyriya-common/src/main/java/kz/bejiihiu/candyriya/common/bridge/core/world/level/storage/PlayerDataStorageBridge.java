package kz.bejiihiu.candyriya.common.bridge.core.world.level.storage;

import java.io.File;
import net.minecraft.nbt.CompoundTag;

public interface PlayerDataStorageBridge {

    File bridge$getPlayerDir();

    CompoundTag bridge$getPlayerData(String uuid);
}
