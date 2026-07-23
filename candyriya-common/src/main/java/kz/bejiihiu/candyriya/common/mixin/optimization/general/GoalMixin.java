package kz.bejiihiu.candyriya.common.mixin.optimization.general;

import kz.bejiihiu.candyriya.i18n.CandyriyaConfig;
import net.minecraft.world.entity.ai.goal.Goal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Goal.class)
public class GoalMixin {

    @ModifyConstant(method = "reducedTickDelay", constant = @Constant(intValue = 2))
    private static int Candyriya$goalUpdateInterval(int orig) {
        return CandyriyaConfig.spec().getOptimization().getGoalSelectorInterval();
    }
}
