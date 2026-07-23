package kz.bejiihiu.candyriya.common.mixin.core.world.item;

import kz.bejiihiu.candyriya.mixin.Decorate;
import kz.bejiihiu.candyriya.mixin.DecorationOps;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnderEyeItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnderEyeItem.class)
public class EnderEyeItemMixin {

    @Decorate(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean Candyriya$returnIfFail(Level world, Entity entityIn, Level worldIn, Player playerIn, InteractionHand handIn) throws Throwable {
        if (!(boolean) DecorationOps.callsite().invoke(world, entityIn)) {
            return (boolean) DecorationOps.cancel().invoke(new InteractionResultHolder<>(InteractionResult.FAIL, playerIn.getItemInHand(handIn)));
        } else {
            return true;
        }
    }
}
