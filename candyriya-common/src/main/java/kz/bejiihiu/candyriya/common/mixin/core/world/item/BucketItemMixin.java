package kz.bejiihiu.candyriya.common.mixin.core.world.item;

import kz.bejiihiu.candyriya.common.bridge.core.server.level.ServerPlayerBridge;
import kz.bejiihiu.candyriya.common.bridge.core.world.level.LevelAccessorBridge;
import kz.bejiihiu.candyriya.common.bridge.core.world.item.BucketItemBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.util.DummyGeneratorAccess;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public abstract class BucketItemMixin implements BucketItemBridge {

    // @formatter:off
    @Shadow public abstract boolean emptyContents(@Nullable Player player, Level worldIn, BlockPos posIn, @javax.annotation.Nullable BlockHitResult rayTrace);
    // @formatter:on

    // Using @Local doesn't work for Forge since they don't have MixinExtras :(
    // Use Decorate to capture
    @Decorate(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/BucketPickup;pickupBlock(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack Candyriya$bucketFill(BucketPickup pickup, Player playerIn, LevelAccessor worldIn, BlockPos pos, BlockState state,
                                          @Local(ordinal = 0) InteractionHand handIn, @Local(ordinal = 0) ItemStack stack, @Local(ordinal = 0) BlockHitResult result) throws Throwable {
        if (LevelAccessorBridge.from(worldIn) instanceof LevelAccessorBridge bridge) {
            ItemStack dummyFluid = pickup.pickupBlock(playerIn, DummyGeneratorAccess.INSTANCE, pos, state);
            PlayerBucketFillEvent event = CraftEventFactory.callPlayerBucketFillEvent(bridge.bridge$getMinecraftWorld(), playerIn, pos, pos, result.getDirection(), stack, dummyFluid.getItem(), handIn);
            if (event.isCancelled()) {
                ((ServerPlayer) playerIn).connection.send(new ClientboundBlockUpdatePacket(worldIn, pos));
                ((ServerPlayerBridge) playerIn).bridge$getBukkitEntity().updateInventory();
                return (ItemStack) DecorationOps.cancel().invoke(new InteractionResultHolder<>(InteractionResult.FAIL, stack));
            } else {
                Candyriya$setCaptureItem(event.getItemStack());
            }
        }
        return (ItemStack) DecorationOps.callsite().invoke(pickup, playerIn, worldIn, pos, state);
    }

    @Inject(method = "use", at = @At("RETURN"))
    private void Candyriya$clean(Level worldIn, Player playerIn, InteractionHand handIn, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        Candyriya$setDirection(null);
        Candyriya$setClick(null);
        Candyriya$setHand(null);
        Candyriya$setStack(null);
    }

    @ModifyArg(method = "use", index = 2, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemUtils;createFilledResult(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack Candyriya$useEventItem(ItemStack itemStack) {
        return Candyriya$getCaptureItem() == null ? itemStack : CraftItemStack.asNMSCopy(Candyriya$getCaptureItem());
    }

    public boolean emptyContents(Player entity, Level world, BlockPos pos, @Nullable BlockHitResult result, Direction direction, BlockPos clicked, ItemStack itemstack, InteractionHand hand) {
        Candyriya$setDirection(direction);
        Candyriya$setClick(clicked);
        Candyriya$setHand(hand);
        Candyriya$setStack(itemstack);
        try {
            return this.emptyContents(entity, world, pos, result);
        } finally {
            Candyriya$setDirection(null);
            Candyriya$setClick(null);
            Candyriya$setHand(null);
            Candyriya$setStack(null);
        }
    }

    @Unique
    @Nullable
    private transient Direction Candyriya$direction;

    @Unique
    @Nullable
    private transient BlockPos Candyriya$click;

    @Unique
    @Nullable
    private transient InteractionHand Candyriya$hand;

    @Unique
    @Nullable
    private transient ItemStack Candyriya$stack;

    @Unique
    @Nullable
    private transient org.bukkit.inventory.ItemStack Candyriya$captureItem;

    @Nullable
    @Override
    public Direction Candyriya$getDirection() {
        return this.Candyriya$direction;
    }

    @Override
    public void Candyriya$setDirection(@Nullable Direction value) {
        this.Candyriya$direction = value;
    }

    @Nullable
    @Override
    public BlockPos Candyriya$getClick() {
        return this.Candyriya$click;
    }

    @Override
    public void Candyriya$setClick(@Nullable BlockPos value) {
        this.Candyriya$click = value;
    }

    @Nullable
    @Override
    public InteractionHand Candyriya$getHand() {
        return this.Candyriya$hand;
    }

    @Override
    public void Candyriya$setHand(@Nullable InteractionHand value) {
        this.Candyriya$hand = value;
    }

    @Nullable
    @Override
    public ItemStack Candyriya$getStack() {
        return this.Candyriya$stack;
    }

    @Override
    public void Candyriya$setStack(@Nullable ItemStack value) {
        this.Candyriya$stack = value;
    }

    @Nullable
    @Override
    public org.bukkit.inventory.ItemStack Candyriya$getCaptureItem() {
        return this.Candyriya$captureItem;
    }

    @Override
    public void Candyriya$setCaptureItem(@Nullable org.bukkit.inventory.ItemStack value) {
        this.Candyriya$captureItem = value;
    }
}
