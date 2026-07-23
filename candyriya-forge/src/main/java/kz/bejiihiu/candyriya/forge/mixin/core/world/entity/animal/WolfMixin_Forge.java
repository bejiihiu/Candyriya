package kz.bejiihiu.candyriya.forge.mixin.core.world.entity.animal;

import kz.bejiihiu.candyriya.common.mixin.core.world.entity.TamableAnimalMixin;
import net.minecraft.world.entity.animal.Wolf;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Wolf.class)
public abstract class WolfMixin_Forge extends TamableAnimalMixin {
    // Dummy: logic moved to ShearsItemMixin
}
