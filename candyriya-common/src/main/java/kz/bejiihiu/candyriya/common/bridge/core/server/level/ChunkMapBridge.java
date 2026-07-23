package kz.bejiihiu.candyriya.common.bridge.core.server.level;

import kz.bejiihiu.candyriya.common.mod.util.CandyriyaCallbackExecutor;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;

import java.util.function.BooleanSupplier;

public interface ChunkMapBridge {

    void bridge$tick(BooleanSupplier hasMoreTime);

    Iterable<ChunkHolder> bridge$getLoadedChunksIterable();

    void bridge$tickEntityTracker();

    CandyriyaCallbackExecutor bridge$getCallbackExecutor();

    ChunkHolder bridge$chunkHolderAt(long chunkPos);

    void bridge$setViewDistance(int i);

    void bridge$setChunkGenerator(ChunkGenerator generator);
}
