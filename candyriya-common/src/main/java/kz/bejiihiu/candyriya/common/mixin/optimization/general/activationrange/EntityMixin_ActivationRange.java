package kz.bejiihiu.candyriya.common.mixin.optimization.general.activationrange;

import kz.bejiihiu.candyriya.common.bridge.core.entity.EntityBridge;
import kz.bejiihiu.candyriya.common.bridge.core.world.level.WorldBridge;
import kz.bejiihiu.candyriya.common.bridge.optimization.EntityBridge_ActivationRange;
import kz.bejiihiu.candyriya.common.mod.CandyriyaConstants;
import kz.bejiihiu.candyriya.common.mod.util.DistValidate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spigotmc.ActivationRange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin_ActivationRange implements EntityBridge_ActivationRange, EntityBridge {

    // @formatter:off
    @Shadow public abstract void refreshDimensions();
    @Shadow public int tickCount;
    @Shadow public abstract Level level();
    @Shadow public abstract AABB getBoundingBox();
    @Shadow public abstract void discard();
    // @formatter:on

    public ActivationRange.ActivationType activationType;
    public boolean defaultActivationState;
    public long activatedTick = Integer.MIN_VALUE;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void Candyriya$init(EntityType<?> entityTypeIn, Level worldIn, CallbackInfo ci) {
        activationType = ActivationRange.initializeEntityActivationType((Entity) (Object) this);
        if (DistValidate.isValid(worldIn)) {
            var config = ((WorldBridge) worldIn).bridge$spigotConfig();
            if (config != null) {
                this.defaultActivationState = ActivationRange.initializeEntityActivationState((Entity) (Object) this, config);
            } else {
                this.defaultActivationState = false;
            }
        } else {
            this.defaultActivationState = false;
        }
    }

    public void inactiveTick() {
    }

    @Override
    public void bridge$inactiveTick() {
        this.inactiveTick();
    }

    @Override
    public void bridge$updateActivation() {
        if (CandyriyaConstants.currentTick > this.activatedTick) {
            if (this.defaultActivationState) {
                this.activatedTick = CandyriyaConstants.currentTick;
            } else if (this.activationType.boundingBox.intersects(this.getBoundingBox())) {
                this.activatedTick = CandyriyaConstants.currentTick;
            }
        }
    }
}
