package kz.bejiihiu.candyriya.common.mixin.core.world.level.portal;

import kz.bejiihiu.candyriya.common.bridge.core.world.level.portal.PortalForcerBridge;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalForcer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.util.BlockStateListPopulator;
import org.bukkit.event.world.PortalCreateEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(value = PortalForcer.class, priority = 1500)
public abstract class PortalForcerMixin implements PortalForcerBridge {

    // @formatter:off
    @Shadow public abstract Optional<BlockUtil.FoundRectangle> createPortal(BlockPos pos, Direction.Axis axis);
    @Shadow @Final protected ServerLevel level;
    // @formatter:on

    private transient int Candyriya$searchRadius = -1;

    @Override
    public void bridge$pushSearchRadius(int searchRadius) {
        this.Candyriya$searchRadius = searchRadius;
    }

    @ModifyVariable(method = "findClosestPortalPosition", ordinal = 0, at = @At(value = "STORE", ordinal = 0))
    private int Candyriya$useSearchRadius(int i) {
        return this.Candyriya$searchRadius == -1 ? i : this.Candyriya$searchRadius;
    }

    @ModifyArg(method = "createPortal", index = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;spiralAround(Lnet/minecraft/core/BlockPos;ILnet/minecraft/core/Direction;Lnet/minecraft/core/Direction;)Ljava/lang/Iterable;"))
    private int Candyriya$changeRadius(int i) {
        return this.Candyriya$createRadius == -1 ? i : this.Candyriya$createRadius;
    }

    @Redirect(method = "createPortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean Candyriya$captureBlocks1(ServerLevel serverWorld, BlockPos pos, BlockState state) {
        if (this.Candyriya$populator == null) {
            this.Candyriya$populator = new BlockStateListPopulator(serverWorld);
        }
        return this.Candyriya$populator.setBlock(pos, state, 3);
    }

    @Redirect(method = "createPortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean Candyriya$captureBlocks2(ServerLevel serverWorld, BlockPos pos, BlockState state, int flags) {
        if (this.Candyriya$populator == null) {
            this.Candyriya$populator = new BlockStateListPopulator(serverWorld);
        }
        return this.Candyriya$populator.setBlock(pos, state, flags);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "createPortal", cancellable = true, at = @At("RETURN"))
    private void Candyriya$portalCreate(BlockPos pos, Direction.Axis axis, CallbackInfoReturnable<Optional<BlockUtil.FoundRectangle>> cir) {
        CraftWorld craftWorld = this.level.bridge$getWorld();
        List<org.bukkit.block.BlockState> blockStates;
        if (this.Candyriya$populator == null) {
            blockStates = new ArrayList<>();
        } else {
            blockStates = (List) this.Candyriya$populator.getList();
        }
        PortalCreateEvent event = new PortalCreateEvent(blockStates, craftWorld, (this.Candyriya$entity == null) ? null : this.Candyriya$entity.bridge$getBukkitEntity(), PortalCreateEvent.CreateReason.NETHER_PAIR);

        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            cir.setReturnValue(Optional.empty());
            return;
        }
        if (this.Candyriya$populator != null) {
            this.Candyriya$populator.updateList();
        }
    }

    private transient BlockStateListPopulator Candyriya$populator;
    private transient Entity Candyriya$entity;
    private transient int Candyriya$createRadius = -1;

    @Override
    public void bridge$pushPortalCreate(Entity entity, int createRadius) {
        this.Candyriya$entity = entity;
        this.Candyriya$createRadius = createRadius;
    }
}
