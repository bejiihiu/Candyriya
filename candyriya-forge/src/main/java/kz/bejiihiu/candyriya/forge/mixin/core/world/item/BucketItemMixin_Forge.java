package kz.bejiihiu.candyriya.forge.mixin.core.world.item;

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
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(BucketItem.class)
public abstract class BucketItemMixin_Forge implements BucketItemBridge {

    @Inject(method = "use", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/world/item/BucketItem;emptyContents(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/item/ItemStack;)Z"))
    private void Candyriya$capture(Level worldIn, Player playerIn, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir, ItemStack stack, BlockHitResult result) {
        Candyriya$setDirection(result.getDirection());
        Candyriya$setClick(result.getBlockPos());
        Candyriya$setHand(hand);
        Candyriya$setStack(stack);
    }

    @Inject(method = "emptyContents(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/item/ItemStack;)Z", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/dimension/DimensionType;ultraWarm()Z"))
    private void Candyriya$bucketEmpty(Player player, Level worldIn, BlockPos posIn, BlockHitResult rayTrace, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (LevelAccessorBridge.from(worldIn) instanceof LevelAccessorBridge bridge && player != null && stack != null) {
            PlayerBucketEmptyEvent event = CraftEventFactory.callPlayerBucketEmptyEvent(bridge.bridge$getMinecraftWorld(), player, posIn, Candyriya$getClick(), Candyriya$getDirection(), stack, Candyriya$getHand() == null ? InteractionHand.MAIN_HAND : Candyriya$getHand());
            if (event.isCancelled()) {
                ((ServerPlayer) player).connection.send(new ClientboundBlockUpdatePacket(worldIn, posIn));
                ((ServerPlayerBridge) player).bridge$getBukkitEntity().updateInventory();
                cir.setReturnValue(false);
            }
        }
    }
}
