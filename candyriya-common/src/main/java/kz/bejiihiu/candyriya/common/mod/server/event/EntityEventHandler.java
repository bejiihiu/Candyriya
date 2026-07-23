package kz.bejiihiu.candyriya.common.mod.server.event;

import kz.bejiihiu.candyriya.common.bridge.core.entity.EntityBridge;
import kz.bejiihiu.candyriya.common.bridge.core.world.entity.LivingEntityBridge;
import kz.bejiihiu.candyriya.common.bridge.core.server.level.ServerPlayerBridge;
import kz.bejiihiu.candyriya.common.mod.server.CandyriyaServer;
import kz.bejiihiu.candyriya.common.mod.server.world.item.EntityDropContainer;
import kz.bejiihiu.candyriya.common.mod.util.CandyriyaCaptures;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class EntityEventHandler {

    /**
     * @return whether the event is cancelled. If so no drop will be spawned in dropAllDeathLoot
     */
    public static boolean monitorLivingDrops(LivingEntity living, DamageSource source, List<ItemEntity> drops, boolean isCancelled) {
        if (!(living instanceof LivingEntityBridge bridge)) {
            return false;
        }
        if (source == null) {
            source = living.damageSources().genericKill();
        }
        if (isCancelled) {
            drops.clear();
        }
        if (living instanceof ServerPlayer player) {
            String dmsgOrig = player.getCombatTracker().getDeathMessage().getString();
            Inventory beforeDeath = CandyriyaCaptures.getDeathPlayerInv();
            Inventory original;
            if (beforeDeath != null) { // not keeping inventory, from game rule
                original = new Inventory(player);
                original.replaceWith(player.getInventory());
                player.getInventory().replaceWith(beforeDeath);
            } else {
                original = null;
            }
            int expReward = bridge.bridge$getExpReward(source.getEntity());
            final PlayerDeathEvent event;
            try (final var container = new EntityDropContainer()) {
                // Candyriya: Spigot drops obtained from getCapturedDrops()
                // Already respect vanilla behaviours by using entity capture
                List<org.bukkit.inventory.ItemStack> loot = container.initDecorate(drops);
                event = CandyriyaEventFactory.callPlayerDeathEvent(player, source, loot, expReward, dmsgOrig, original == null);
                if (event.getKeepInventory()) {
                    original = null;
                }
                container.convert(loot, drops, bridge::Candyriya$spawnAtLocationNoAdd);
            }
            // beforeDeath == null : true if we used to keepInventory
            // original == null : true if now we don't keepInventory
            if (beforeDeath != null) {
                if (original == null) {
                    CandyriyaServer.LOGGER.debug("Overriding keepInventory from false to true. Preserve modified inventory before death.");
                } else {
                    // Don't clear it. Preserve original content.
                    // player.getInventory().clearContent();
                    player.getInventory().replaceWith(original);
                }
            } else if (!event.getKeepInventory()) {
                CandyriyaServer.LOGGER.warn("Overriding keepInventory from true to false. This won't take effect.");
            }
            ((ServerPlayerBridge) player).Candyriya$readDeathEvent(event);
        } else {
            final var extra = CandyriyaCaptures.consumeExtraDrops();
            if (extra != null) {
                drops.addAll(extra);
            }
            final EntityDeathEvent event;
            try (final var container = new EntityDropContainer()) {
                List<ItemStack> itemStackList = container.initDecorate(drops);
                event = CandyriyaEventFactory.callEntityDeathEvent(living, source, itemStackList);
                container.convert(itemStackList, drops, ((EntityBridge) living)::Candyriya$spawnAtLocationNoAdd);
            }
            bridge.bridge$setExpToDrop(event.getDroppedExp());
        }
        return drops.isEmpty();
    }
}
