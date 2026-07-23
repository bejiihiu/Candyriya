package kz.bejiihiu.candyriya.fabric.mixin.core.world.entity;

import kz.bejiihiu.candyriya.common.bridge.core.world.entity.LivingEntityBridge;
import kz.bejiihiu.candyriya.common.bridge.vanilla.world.entity.LivingEntityBridge_Vanilla;
import kz.bejiihiu.candyriya.fabric.mod.event.LivingDropsEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin_Fabric extends EntityMixin_Fabric implements LivingEntityBridge, LivingEntityBridge_Vanilla {

    @Override
    public void Candyriya$vanilla$callLivingDropsEvent(DamageSource source, List<ItemEntity> capturedDrops) {
        boolean cancelled = LivingDropsEvent.EVENT.invoker().onLivingDrops((LivingEntity) (Object)this, source, capturedDrops, false);
        if (cancelled) {
            capturedDrops.clear();
        }
    }
}
