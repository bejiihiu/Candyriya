package kz.bejiihiu.candyriya.common.mixin.core.world.level.block.entity;

import kz.bejiihiu.candyriya.common.bridge.core.world.entity.player.PlayerBridge;
import kz.bejiihiu.candyriya.common.bridge.core.world.damagesource.DamageSourceBridge;
import kz.bejiihiu.candyriya.common.mod.mixins.annotation.TransformAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ConduitBlockEntity.class)
public abstract class ConduitBlockEntityMixin extends BlockEntityMixin {

    // @formatter:off
    @Shadow private static void updateDestroyTarget(Level level, BlockPos blockPos, BlockState blockState, List<BlockPos> list, ConduitBlockEntity conduitBlockEntity) {}
    // @formatter:on

    @Redirect(method = "applyEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private static boolean Candyriya$addEntity(Player player, MobEffectInstance eff) {
        ((PlayerBridge) player).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.CONDUIT);
        return player.addEffect(eff);
    }

    @Redirect(method = "updateDestroyTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;magic()Lnet/minecraft/world/damagesource/DamageSource;"))
    private static DamageSource Candyriya$attackReason(DamageSources instance, Level level, BlockPos pos) {
        return ((DamageSourceBridge) instance.magic()).bridge$directBlock(CraftBlock.at(level, pos));
    }

    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static int getRange(List<BlockPos> list) {
        int i = list.size();
        return i / 7 * 16;
    }

    private static boolean Candyriya$damageTarget = true;

    @Inject(method = "updateDestroyTarget", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"))
    private static void Candyriya$returnIfNot(Level level, BlockPos blockPos, BlockState blockState, List<BlockPos> list, ConduitBlockEntity conduitBlockEntity, CallbackInfo ci) {
        if (!Candyriya$damageTarget) {
            ci.cancel();
            level.sendBlockUpdated(blockPos, blockState, blockState, 2);
        }
    }

    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static void updateDestroyTarget(Level level, BlockPos blockPos, BlockState blockState, List<BlockPos> list, ConduitBlockEntity conduitBlockEntity, boolean damageTarget) {
        Candyriya$damageTarget = damageTarget;
        updateDestroyTarget(level, blockPos, blockState, list, conduitBlockEntity);
        Candyriya$damageTarget = true;
    }
}
