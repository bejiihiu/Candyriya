package kz.bejiihiu.candyriya.common.mixin.optimization.general.activationrange.entity;

import kz.bejiihiu.candyriya.common.bridge.core.world.entity.item.ItemEntityBridge;
import kz.bejiihiu.candyriya.common.bridge.core.world.level.WorldBridge;
import kz.bejiihiu.candyriya.common.mixin.optimization.general.activationrange.EntityMixin_ActivationRange;
import kz.bejiihiu.candyriya.common.mod.CandyriyaConstants;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin_ActivationRange extends EntityMixin_ActivationRange implements ItemEntityBridge {

    // @formatter:off
    @Shadow public int pickupDelay;
    @Shadow public int age;
    @Shadow public abstract ItemStack getItem();
    // @formatter:on

    private int lastTick = CandyriyaConstants.currentTick - 1;

    @Override
    public void inactiveTick() {
        super.inactiveTick();
        int elapsedTicks = CandyriyaConstants.currentTick - this.lastTick;
        if (this.pickupDelay > 0 && this.pickupDelay != 32767 && elapsedTicks > 0) this.pickupDelay -= elapsedTicks;
        if (this.age != -32768) this.age += elapsedTicks;
        this.lastTick = CandyriyaConstants.currentTick;
        this.bridge$forge$optimization$discardItemEntity();
    }

    @Override
    public void bridge$forge$optimization$discardItemEntity() {
        if (!this.level().isClientSide && this.age >= ((WorldBridge) this.level()).bridge$spigotConfig().itemDespawnRate) {
            this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DEATH);
            this.discard();
        }
    }
}
