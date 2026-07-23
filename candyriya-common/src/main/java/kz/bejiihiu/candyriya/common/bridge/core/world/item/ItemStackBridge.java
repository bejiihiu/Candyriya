package kz.bejiihiu.candyriya.common.bridge.core.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelReader;

public interface ItemStackBridge {

    void Candyriya$restorePatch(DataComponentPatch datacomponentpatch);

    void Candyriya$setItem(Item item);

    default InteractionResult bridge$forge$onItemUseFirst(UseOnContext context) {
        return InteractionResult.PASS;
    }

    default boolean bridge$forge$doesSneakBypassUse(LevelReader level, BlockPos pos, Player player) {
        return false;
    }
}
