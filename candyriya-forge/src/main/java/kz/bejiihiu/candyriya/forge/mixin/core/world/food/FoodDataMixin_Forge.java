package kz.bejiihiu.candyriya.forge.mixin.core.world.food;

import kz.bejiihiu.candyriya.common.bridge.core.world.food.FoodDataBridge;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FoodData.class)
public abstract class FoodDataMixin_Forge implements FoodDataBridge {

    // @formatter:off
    @Shadow public int foodLevel;
    @Shadow public abstract void eat(int i, float f);
    // @formatter:on
}
