package kz.bejiihiu.candyriya.neoforge.mixin.core.world.entity.player;

import kz.bejiihiu.candyriya.common.bridge.core.world.entity.player.PlayerBridge;
import kz.bejiihiu.candyriya.common.mod.util.CandyriyaCaptures;
import kz.bejiihiu.candyriya.common.mod.util.CandyriyaDamageContainer;
import kz.bejiihiu.candyriya.mixin.Decorate;
import kz.bejiihiu.candyriya.mixin.DecorationOps;
import kz.bejiihiu.candyriya.mixin.Local;
import kz.bejiihiu.candyriya.neoforge.mixin.core.world.entity.LivingEntityMixin_NeoForge;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.extensions.IPlayerExtension;
import org.bukkit.event.entity.EntityDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin_NeoForge extends LivingEntityMixin_NeoForge implements PlayerBridge, IPlayerExtension {

    @Inject(method = "hurt", cancellable = true, at = @At("HEAD"))
    private void Candyriya$onPlayerAttack(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (CommonHooks.onEntityIncomingDamage((Player) (Object) this, new DamageContainer(source, amount))) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "attack", cancellable = true, at = @At("HEAD"))
    private void Candyriya$onPlayerAttackTarget(Entity entity, CallbackInfo ci) {
        if (!CommonHooks.onPlayerAttackTarget((Player) (Object) this, entity)) {
            ci.cancel();
        }
    }

    @Decorate(method = "actuallyHurt", inject = true, at = @At("HEAD"))
    private void Candyriya$neoforge$getDamageContainer(DamageSource damageSource, float f, @Local(allocate = "CandyriyaDamageContainer") CandyriyaDamageContainer container) throws Throwable {
        container = CandyriyaCaptures.getDamageContainer();
        DecorationOps.blackhole().invoke(container);
    }

    @Decorate(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/common/CommonHooks;onLivingDamagePre(Lnet/minecraft/world/entity/LivingEntity;Lnet/neoforged/neoforge/common/damagesource/DamageContainer;)F"))
    private float Candyriya$neoforge$applyFromLivingDamagePre(LivingEntity entity, DamageContainer container, @Local(allocate = "CandyriyaDamageContainer") CandyriyaDamageContainer Candyriya) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(entity, container);
        Candyriya.setCurrentDamage(result);
        return result;
    }

    @Decorate(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float Candyriya$neoforge$postApplyArmor(Player player, DamageSource source, float original, @Local(allocate = "CandyriyaDamageContainer") CandyriyaDamageContainer container) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(player, source, original);
        result = container.calculateStage(EntityDamageEvent.DamageModifier.ARMOR, result);
        return result;
    }

    @Decorate(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float Candyriya$neoforge$postApplyMagic(Player player, DamageSource source, float original, @Local(allocate = "CandyriyaDamageContainer") CandyriyaDamageContainer Candyriya) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(player, source, original);
        float newResult = Candyriya.calculateStage(EntityDamageEvent.DamageModifier.MAGIC, result);
        if (Math.abs(result - newResult) > Mth.EPSILON) {
            DamageContainer container = damageContainers.peek();
            container.setNewDamage(original);
            container.setReduction(DamageContainer.Reduction.ENCHANTMENTS, original - newResult);
        }
        return newResult;
    }

    @Decorate(method = "actuallyHurt", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/neoforged/neoforge/common/damagesource/DamageContainer;setReduction(Lnet/neoforged/neoforge/common/damagesource/DamageContainer$Reduction;F)V"))
    private void Candyriya$neoforge$postApplyAbsorption(DamageContainer container, DamageContainer.Reduction reduction, float amount, @Local(allocate = "CandyriyaDamageContainer") CandyriyaDamageContainer Candyriya) throws Throwable {
        float currentDamage = damageContainers.peek().getNewDamage();
        float exactDamage = currentDamage - amount;
        float afterDamage = Candyriya.calculateStage(EntityDamageEvent.DamageModifier.ABSORPTION, exactDamage);
        if (Math.abs(afterDamage - exactDamage) > Mth.EPSILON) {
            amount = currentDamage - afterDamage;
        }
        DecorationOps.callsite().invoke(container, reduction, amount);
    }

    @Inject(method = "actuallyHurt", at = @At("RETURN"))
    private void Candyriya$neoforge$popEntityDamageEvent(DamageSource arg, float g, CallbackInfo ci) {
        CandyriyaCaptures.popDamageContainer();
    }

    @Override
    public boolean bridge$platform$mayfly() {
        return this.mayFly();
    }
}
