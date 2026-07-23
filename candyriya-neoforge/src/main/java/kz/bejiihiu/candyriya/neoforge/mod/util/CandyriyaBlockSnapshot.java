package kz.bejiihiu.candyriya.neoforge.mod.util;

import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import org.bukkit.craftbukkit.v.block.CraftBlock;

public class CandyriyaBlockSnapshot extends CraftBlock {

    private final BlockState blockState;

    public CandyriyaBlockSnapshot(BlockSnapshot blockSnapshot, boolean current) {
        super(blockSnapshot.getLevel(), blockSnapshot.getPos());
        this.blockState = current ? blockSnapshot.getCurrentState() : blockSnapshot.getState();
    }

    @Override
    public BlockState getNMS() {
        return blockState;
    }

    public static CandyriyaBlockSnapshot fromBlockSnapshot(BlockSnapshot blockSnapshot, boolean current) {
        return new CandyriyaBlockSnapshot(blockSnapshot, current);
    }
}
