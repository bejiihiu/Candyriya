package kz.bejiihiu.candyriya.common.mixin.vanilla.world.entity.monster;

import kz.bejiihiu.candyriya.common.bridge.core.network.syncher.SynchedEntityDataBridge;
import kz.bejiihiu.candyriya.common.mixin.core.world.entity.monster.AbstractSkeletonMixin;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Bogged;
import net.minecraft.world.entity.player.Player;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Bogged.class)
public abstract class BoggedMixin_Vanilla extends AbstractSkeletonMixin {

    @Shadow @Final private static EntityDataAccessor<Boolean> DATA_SHEARED;

    @Inject(method = "mobInteract", cancellable = true, require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Bogged;shear(Lnet/minecraft/sounds/SoundSource;)V"))
    private void Candyriya$shearEvent(Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!CraftEventFactory.handlePlayerShearEntityEvent(player, (Entity) (Object) this, player.getItemInHand(interactionHand), interactionHand)) {
            ((SynchedEntityDataBridge) this.getEntityData()).bridge$markDirty(DATA_SHEARED);
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
