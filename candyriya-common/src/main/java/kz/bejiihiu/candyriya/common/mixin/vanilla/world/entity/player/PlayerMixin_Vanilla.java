package kz.bejiihiu.candyriya.common.mixin.vanilla.world.entity.player;

import kz.bejiihiu.candyriya.common.mixin.vanilla.world.entity.LivingEntityMixin_Vanilla;
import kz.bejiihiu.candyriya.common.mod.util.CandyriyaCaptures;
import kz.bejiihiu.candyriya.common.mod.util.CandyriyaDamageContainer;
import kz.bejiihiu.candyriya.mixin.Decorate;
import kz.bejiihiu.candyriya.mixin.DecorationOps;
import kz.bejiihiu.candyriya.mixin.Local;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin_Vanilla extends LivingEntityMixin_Vanilla {

    @Decorate(method = "actuallyHurt", inject = true, at = @At("HEAD"))
    private void Candyriya$vanilla$getEntityDamageEvent(DamageSource damageSource, float f, @Local(allocate = "CandyriyaDamageContainer") CandyriyaDamageContainer container) throws Throwable {
        container = CandyriyaCaptures.getDamageContainer();
        DecorationOps.blackhole().invoke(container);
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
