package kz.bejiihiu.candyriya.common.mixin.core.world.effect;

import kz.bejiihiu.candyriya.common.bridge.core.server.level.ServerPlayerBridge;
import kz.bejiihiu.candyriya.mixin.Decorate;
import kz.bejiihiu.candyriya.mixin.DecorationOps;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.effect.SaturationMobEffect")
public class SaturationMobEffectMixin {

    @Decorate(method = "applyEffectTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(IF)V"))
    private void Candyriya$foodLevelChange(FoodData foodStats, int foodLevelIn, float foodSaturationModifier, LivingEntity livingEntity, int amplifier) throws Throwable {
        Player playerEntity = ((Player) livingEntity);
        int oldFoodLevel = playerEntity.getFoodData().getFoodLevel();
        FoodLevelChangeEvent event = CraftEventFactory.callFoodLevelChangeEvent(playerEntity, foodLevelIn + oldFoodLevel);
        if (!event.isCancelled()) {
            DecorationOps.callsite().invoke(foodStats, event.getFoodLevel() - oldFoodLevel, foodSaturationModifier);
        }
        ((ServerPlayer) playerEntity).connection.send(new ClientboundSetHealthPacket(((ServerPlayerBridge) playerEntity).bridge$getBukkitEntity().getScaledHealth(),
            playerEntity.getFoodData().getFoodLevel(), playerEntity.getFoodData().getSaturationLevel()));
    }
}
