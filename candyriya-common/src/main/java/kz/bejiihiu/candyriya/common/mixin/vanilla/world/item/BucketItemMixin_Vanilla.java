package kz.bejiihiu.candyriya.common.mixin.vanilla.world.item;

import com.llamalad7.mixinextras.sugar.Local;
import kz.bejiihiu.candyriya.common.bridge.core.server.level.ServerPlayerBridge;
import kz.bejiihiu.candyriya.common.bridge.core.world.level.LevelAccessorBridge;
import kz.bejiihiu.candyriya.common.bridge.core.world.item.BucketItemBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public abstract class BucketItemMixin_Vanilla implements BucketItemBridge {
    @Inject(method = "use", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BucketItem;emptyContents(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;)Z"))
    private void Candyriya$capture(Level worldIn, Player playerIn, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir, @Local BlockHitResult result, @Local ItemStack stack) {
        Candyriya$setDirection(result.getDirection());
        Candyriya$setClick(result.getBlockPos());
        Candyriya$setHand(hand);
        Candyriya$setStack(stack);
    }

    @Inject(method = "emptyContents", require = 0, cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/dimension/DimensionType;ultraWarm()Z"))
    private void Candyriya$bucketEmpty(Player player, Level worldIn, BlockPos posIn, BlockHitResult rayTrace, CallbackInfoReturnable<Boolean> cir) {
        if (LevelAccessorBridge.from(worldIn) instanceof LevelAccessorBridge bridge && player != null && Candyriya$getStack() != null) {
            PlayerBucketEmptyEvent event = CraftEventFactory.callPlayerBucketEmptyEvent(bridge.bridge$getMinecraftWorld(), player, posIn, Candyriya$getClick(), Candyriya$getDirection(), Candyriya$getStack(), Candyriya$getHand() == null ? InteractionHand.MAIN_HAND : Candyriya$getHand());
            if (event.isCancelled()) {
                ((ServerPlayer) player).connection.send(new ClientboundBlockUpdatePacket(worldIn, posIn));
                ((ServerPlayerBridge) player).bridge$getBukkitEntity().updateInventory();
                cir.setReturnValue(false);
            }
        }
    }
}
