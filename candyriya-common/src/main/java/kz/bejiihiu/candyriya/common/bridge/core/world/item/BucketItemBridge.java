package kz.bejiihiu.candyriya.common.bridge.core.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface BucketItemBridge {
    @Nullable Direction Candyriya$getDirection();
    void Candyriya$setDirection(@Nullable Direction value);

    @Nullable BlockPos Candyriya$getClick();
    void Candyriya$setClick(@Nullable BlockPos value);

    @Nullable InteractionHand Candyriya$getHand();
    void Candyriya$setHand(@Nullable InteractionHand value);

    @Nullable ItemStack Candyriya$getStack();
    void Candyriya$setStack(@Nullable ItemStack value);

    @Nullable org.bukkit.inventory.ItemStack Candyriya$getCaptureItem();
    void Candyriya$setCaptureItem(@Nullable org.bukkit.inventory.ItemStack value);
}
