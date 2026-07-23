package kz.bejiihiu.candyriya.common.mixin.core.world.entity.animal.camel;

import kz.bejiihiu.candyriya.common.mixin.core.world.entity.animal.horse.AbstractHorseMixin;
import net.minecraft.world.entity.animal.camel.Camel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Camel.class)
public abstract class CamelMixin extends AbstractHorseMixin {

    // @formatter:off
    @Shadow public abstract void standUpInstantly();
    // @formatter:on
}
