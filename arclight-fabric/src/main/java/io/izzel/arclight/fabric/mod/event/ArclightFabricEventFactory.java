package io.izzel.arclight.fabric.mod.event;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.block.Action;

public class ArclightFabricEventFactory {

    public static void callCancelledPlayerInteract(Player player, BlockPos pos, Direction face, InteractionHand hand) {
        ArclightCaptures.cancelPlayerInteract();
        CraftEventFactory.callPlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, pos, face, player.getInventory().getSelected(), hand);
    }
}
