package kz.bejiihiu.candyriya.common.bridge.core.network.syncher;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerPlayer;

public interface SynchedEntityDataBridge {

    <T> void bridge$markDirty(EntityDataAccessor<T> key);

    void bridge$refresh(ServerPlayer player);
}
