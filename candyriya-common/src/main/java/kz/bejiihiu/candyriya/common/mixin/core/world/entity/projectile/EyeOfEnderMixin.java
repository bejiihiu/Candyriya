package kz.bejiihiu.candyriya.common.mixin.core.world.entity.projectile;

import kz.bejiihiu.candyriya.common.mixin.core.world.entity.EntityMixin;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EyeOfEnder.class)
public abstract class EyeOfEnderMixin extends EntityMixin {

    @Shadow public boolean surviveAfterDeath;

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/EyeOfEnder;discard()V"))
    private void Candyriya$drop(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(this.surviveAfterDeath ? EntityRemoveEvent.Cause.DROP : EntityRemoveEvent.Cause.DESPAWN);
    }
}
