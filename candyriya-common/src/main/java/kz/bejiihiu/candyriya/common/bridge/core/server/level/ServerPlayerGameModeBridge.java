package kz.bejiihiu.candyriya.common.bridge.core.server.level;

import kz.bejiihiu.candyriya.common.mod.util.CandyriyaCaptures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public interface ServerPlayerGameModeBridge {

    boolean bridge$isFiredInteract();

    void bridge$setFiredInteract(boolean b);

    boolean bridge$getInteractResult();

    void bridge$setInteractResult(boolean b);

    void bridge$handleBlockDrop(CandyriyaCaptures.BlockBreakEventContext breakEventContext, BlockPos pos);

    BlockPos bridge$getInteractPosition();

    InteractionHand bridge$getInteractHand();

    ItemStack bridge$getInteractItemStack();
}
