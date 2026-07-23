package kz.bejiihiu.candyriya.common.mod.server.api;

import io.izzel.arclight.api.TickingTracker;
import kz.bejiihiu.candyriya.common.bridge.core.entity.EntityBridge;
import kz.bejiihiu.candyriya.common.mod.util.CandyriyaCaptures;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class DefaultTickingTracker implements TickingTracker {

    @Nullable
    @Override
    public Object getTickingSource() {
        Entity entity = getTickingEntity();
        if (entity != null) {
            return entity;
        }
        TileState tileState = getTickingBlockEntity();
        if (tileState != null) {
            return tileState;
        }
        Block block = getTickingBlock();
        if (block != null) {
            return block;
        }
        return null;
    }

    @Nullable
    @Override
    public Entity getTickingEntity() {
        var tickingEntity = CandyriyaCaptures.getTickingEntity();
        if (tickingEntity != null) {
            return ((EntityBridge) tickingEntity).bridge$getBukkitEntity();
        }
        return null;
    }

    @Nullable
    @Override
    public Block getTickingBlock() {
        var level = CandyriyaCaptures.getTickingLevel();
        var pos = CandyriyaCaptures.getTickingPosition();
        if (level != null && pos != null) {
            return CraftBlock.at(level, pos);
        }
        return null;
    }

    @Nullable
    @Override
    public TileState getTickingBlockEntity() {
        var blockEntity = CandyriyaCaptures.getTickingBlockEntity();
        if (blockEntity != null) {
            var level = blockEntity.getLevel();
            if (level != null) {
                CraftBlock block = CraftBlock.at(level, blockEntity.getBlockPos());
                BlockState state = block.getState();
                if (state instanceof TileState) {
                    return (TileState) state;
                }
            }
        }
        return null;
    }
}
