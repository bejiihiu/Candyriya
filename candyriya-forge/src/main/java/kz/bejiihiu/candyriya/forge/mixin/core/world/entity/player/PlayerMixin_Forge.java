package kz.bejiihiu.candyriya.forge.mixin.core.world.entity.player;

import kz.bejiihiu.candyriya.common.bridge.core.world.entity.player.PlayerBridge;
import kz.bejiihiu.candyriya.common.mod.util.CandyriyaCaptures;
import kz.bejiihiu.candyriya.common.mod.util.CandyriyaDamageContainer;
import kz.bejiihiu.candyriya.forge.mixin.core.world.entity.LivingEntityMixin_Forge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.extensions.IForgePlayer;
import org.bukkit.event.entity.EntityDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin_Forge extends LivingEntityMixin_Forge implements PlayerBridge, IForgePlayer {
    @Decorate(method = "actuallyHurt", inject = true, at = @At("HEAD"))
    private void Candyriya$vanilla$getEntityDamageEvent(DamageSource damageSource, float f, @Local(allocate = "CandyriyaDamageContainer") CandyriyaDamageContainer container) throws Throwable {
        container = CandyriyaCaptures.getDamageContainer();
        DecorationOps.blackhole().invoke(container);
    }

    @Decorate(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/common/ForgeHooks;onLivingHurt(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float Candyriya$forge$applyFromLivingHurt(LivingEntity entity, DamageSource src, float amount, @Local(allocate = "CandyriyaDamageContainer") CandyriyaDamageContainer container) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(entity, src, amount);
        container.setCurrentDamage(result);
        return result;
    }

    @Decorate(method = "actuallyHurt", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private void Candyriya$vanilla$postApplyArmor(DamageSource source, float original, @Local(allocate = "CandyriyaDamageContainer") CandyriyaDamageContainer container) throws Throwable {
        original = container.calculateStage(EntityDamageEvent.DamageModifier.ARMOR, original);
        DecorationOps.blackhole().invoke(original);
    }

    @Decorate(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float Candyriya$vanilla$postApplyMagic(Player entity, DamageSource source, float original, @Local(allocate = "CandyriyaDamageContainer") CandyriyaDamageContainer container) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(entity, source, original);
        return container.calculateStage(EntityDamageEvent.DamageModifier.MAGIC, result);
    }

    @Decorate(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
    private float Candyriya$vanilla$postApplyAbsorption(float first, float second, @Local(allocate = "CandyriyaDamageContainer") CandyriyaDamageContainer container) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(first, second);
        result = container.calculateStage(EntityDamageEvent.DamageModifier.ABSORPTION, result);
        return Math.max(result, 0.0F);
    }

    @Inject(method = "actuallyHurt", at = @At("RETURN"))
    private void Candyriya$vanilla$popEntityDamageEvent(DamageSource arg, float g, CallbackInfo ci) {
        CandyriyaCaptures.popDamageContainer();
    }
}
