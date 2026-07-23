package kz.bejiihiu.candyriya.common.mixin.core.world.level.block;

import kz.bejiihiu.candyriya.common.mixin.core.world.SimpleContainerMixin;
import kz.bejiihiu.candyriya.common.mod.mixins.annotation.CreateConstructor;
import kz.bejiihiu.candyriya.common.mod.mixins.annotation.ShadowConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ComposterBlock;
import org.bukkit.craftbukkit.v.inventory.CraftBlockInventoryHolder;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ComposterBlock.EmptyContainer.class)
public abstract class ComposterBlock_EmptyContainerMixin extends SimpleContainerMixin {

    @ShadowConstructor
    public void Candyriya$constructor() {
        throw new RuntimeException();
    }

    @CreateConstructor
    public void Candyriya$constructor(LevelAccessor world, BlockPos blockPos) {
        Candyriya$constructor();
        this.setOwner(new CraftBlockInventoryHolder(world, blockPos, this));
    }
}
