package kz.bejiihiu.candyriya.neoforge.mod.event;

import kz.bejiihiu.candyriya.common.bridge.core.server.level.ServerPlayerBridge;
import kz.bejiihiu.candyriya.common.mod.util.CandyriyaCaptures;
import kz.bejiihiu.candyriya.common.mod.util.DistValidate;
import kz.bejiihiu.candyriya.neoforge.mod.util.CandyriyaBlockSnapshot;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BlockPlaceEventDispatcher {

    @SubscribeEvent(receiveCanceled = true)
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayerBridge playerEntity) {
            Player player = playerEntity.bridge$getBukkitEntity();
            Direction direction = CandyriyaCaptures.getPlaceEventDirection();
            if (direction != null && DistValidate.isValid(event.getLevel())) {
                InteractionHand hand = CandyriyaCaptures.getPlaceEventHand(InteractionHand.MAIN_HAND);
                CraftBlock placedBlock = CandyriyaBlockSnapshot.fromBlockSnapshot(event.getBlockSnapshot(), true);
                CraftBlock againstBlock = CraftBlock.at(event.getLevel(), event.getPos().relative(direction.getOpposite()));
                ItemStack bukkitStack;
                EquipmentSlot bukkitHand;
                if (hand == InteractionHand.MAIN_HAND) {
                    bukkitStack = player.getInventory().getItemInMainHand();
                    bukkitHand = EquipmentSlot.HAND;
                } else {
                    bukkitStack = player.getInventory().getItemInOffHand();
                    bukkitHand = EquipmentSlot.OFF_HAND;
                }
                BlockPlaceEvent placeEvent = new BlockPlaceEvent(
                        placedBlock,
                        placedBlock.getState(),
                        againstBlock,
                        bukkitStack,
                        player,
                        !event.isCanceled(),
                        bukkitHand
                );
                placeEvent.setCancelled(event.isCanceled());
                Bukkit.getPluginManager().callEvent(placeEvent);
                event.setCanceled(placeEvent.isCancelled() || !placeEvent.canBuild());
            }
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onMultiPlace(BlockEvent.EntityMultiPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayerBridge playerEntity) {
            Player player = playerEntity.bridge$getBukkitEntity();
            Direction direction = CandyriyaCaptures.getPlaceEventDirection();
            if (direction != null && DistValidate.isValid(event.getLevel())) {
                InteractionHand hand = CandyriyaCaptures.getPlaceEventHand(InteractionHand.MAIN_HAND);
                List<BlockState> placedBlocks = new ArrayList<>(event.getReplacedBlockSnapshots().size());
                for (BlockSnapshot snapshot : event.getReplacedBlockSnapshots()) {
                    placedBlocks.add(CandyriyaBlockSnapshot.fromBlockSnapshot(snapshot, true).getState());
                }
                CraftBlock againstBlock = CraftBlock.at(event.getLevel(), event.getPos().relative(direction.getOpposite()));
                ItemStack bukkitStack;
                if (hand == InteractionHand.MAIN_HAND) {
                    bukkitStack = player.getInventory().getItemInMainHand();
                } else {
                    bukkitStack = player.getInventory().getItemInOffHand();
                }
                BlockPlaceEvent placeEvent = new BlockMultiPlaceEvent(
                        placedBlocks,
                        againstBlock,
                        bukkitStack,
                        player,
                        !event.isCanceled()
                );
                placeEvent.setCancelled(event.isCanceled());
                Bukkit.getPluginManager().callEvent(placeEvent);
                event.setCanceled(placeEvent.isCancelled() || !placeEvent.canBuild());
            }
        }
    }
}
