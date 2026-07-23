package kz.bejiihiu.candyriya.common.mixin.core.world.inventory;

import kz.bejiihiu.candyriya.common.bridge.core.world.level.block.entity.AbstractFurnaceBlockEntityBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FurnaceResultSlot.class)
public class FurnaceResultSlotMixin {

    // @formatter:off
    @Shadow private int removeCount;
    // @formatter:on

    @Redirect(method = "checkTakeAchievements(Lnet/minecraft/world/item/ItemStack;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;awardUsedRecipesAndPopExperience(Lnet/minecraft/server/level/ServerPlayer;)V"))
    public void Candyriya$furnaceDropExp(AbstractFurnaceBlockEntity furnace, ServerPlayer player, ItemStack stack) {
        ((AbstractFurnaceBlockEntityBridge) furnace).bridge$dropExp(player, stack, this.removeCount);
    }
}
