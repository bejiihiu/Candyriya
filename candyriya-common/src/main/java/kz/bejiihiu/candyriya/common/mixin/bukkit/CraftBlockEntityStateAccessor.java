package kz.bejiihiu.candyriya.common.mixin.bukkit;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.bukkit.craftbukkit.v.block.CraftBlockEntityState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = CraftBlockEntityState.class, remap = false)
public interface CraftBlockEntityStateAccessor {
    @Accessor("tileEntity")
    BlockEntity Candyriya$getBlockEntity();
}
