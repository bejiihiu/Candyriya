package kz.bejiihiu.candyriya.forge.mixin.core.world.entity.monster.piglin;

import kz.bejiihiu.candyriya.common.bridge.core.world.entity.monster.piglin.PiglinBridge;
import kz.bejiihiu.candyriya.forge.mixin.core.world.entity.LivingEntityMixin_Forge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Piglin.class)
public abstract class PiglinMixin_Forge extends LivingEntityMixin_Forge implements PiglinBridge {

    @Decorate(method = "holdInOffHand", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/world/item/ItemStack;isPiglinCurrency()Z"))
    private boolean Candyriya$customBarter(ItemStack itemStack) throws Throwable {
        return (boolean) DecorationOps.callsite().invoke(itemStack) || bridge$getAllowedBarterItems().contains(itemStack.getItem());
    }
}
