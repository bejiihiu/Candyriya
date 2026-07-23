package kz.bejiihiu.candyriya.forge.mod.util;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.BlockSnapshot;
import org.bukkit.craftbukkit.v.block.CraftBlock;

public class CandyriyaBlockSnapshot extends CraftBlock {

    private final BlockState blockState;

    public CandyriyaBlockSnapshot(BlockSnapshot blockSnapshot, boolean current) {
        super(blockSnapshot.getLevel(), blockSnapshot.getPos());
        this.blockState = current ? blockSnapshot.getCurrentBlock() : blockSnapshot.getReplacedBlock();
    }

    @Override
    public BlockState getNMS() {
        return blockState;
    }

    public static CandyriyaBlockSnapshot fromBlockSnapshot(BlockSnapshot blockSnapshot, boolean current) {
        return new CandyriyaBlockSnapshot(blockSnapshot, current);
    }
}
