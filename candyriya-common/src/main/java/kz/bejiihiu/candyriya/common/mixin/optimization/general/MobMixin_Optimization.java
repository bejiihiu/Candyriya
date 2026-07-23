package kz.bejiihiu.candyriya.common.mixin.optimization.general;

import kz.bejiihiu.candyriya.i18n.CandyriyaConfig;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Mob.class)
public class MobMixin_Optimization {

    @ModifyConstant(method = "serverAiStep", constant = @Constant(intValue = 2))
    private int Candyriya$goalUpdateInterval(int orig) {
        return CandyriyaConfig.spec().getOptimization().getGoalSelectorInterval();
    }
}
