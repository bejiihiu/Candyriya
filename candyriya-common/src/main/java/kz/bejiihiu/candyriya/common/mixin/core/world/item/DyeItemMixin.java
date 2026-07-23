package kz.bejiihiu.candyriya.common.mixin.core.world.item;

import kz.bejiihiu.candyriya.common.bridge.core.world.entity.LivingEntityBridge;
import kz.bejiihiu.candyriya.common.bridge.core.server.level.ServerPlayerBridge;
import kz.bejiihiu.candyriya.mixin.Decorate;
import kz.bejiihiu.candyriya.mixin.DecorationOps;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.entity.Sheep;
import org.bukkit.event.entity.SheepDyeWoolEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DyeItem.class)
public class DyeItemMixin {

    // @formatter:off
    @Shadow @Final private DyeColor dyeColor;
    // @formatter:on

    @Decorate(method = "interactLivingEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Sheep;setColor(Lnet/minecraft/world/item/DyeColor;)V"))
    private void Candyriya$sheepDyeWool(net.minecraft.world.entity.animal.Sheep sheepEntity, DyeColor color, ItemStack stack, Player playerIn, LivingEntity target, InteractionHand hand) throws Throwable {
        byte bColor = (byte) this.dyeColor.getId();
        SheepDyeWoolEvent event = new SheepDyeWoolEvent((Sheep) ((LivingEntityBridge) target).bridge$getBukkitEntity(), org.bukkit.DyeColor.getByWoolData(bColor), ((ServerPlayerBridge) playerIn).bridge$getBukkitEntity());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            DecorationOps.cancel().invoke(InteractionResult.PASS);
            return;
        } else {
            DecorationOps.callsite().invoke(sheepEntity, DyeColor.byId(event.getColor().getWoolData()));
        }
        DecorationOps.blackhole().invoke();
    }
}
