package kz.bejiihiu.candyriya.common.mixin.vanilla.world.entity.animal;

import kz.bejiihiu.candyriya.common.mixin.core.world.entity.animal.AnimalMixin;
import kz.bejiihiu.candyriya.common.mod.util.CandyriyaCaptures;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(Fox.class)
public abstract class FoxMixin_Vanilla extends AnimalMixin {

    @Decorate(method = "dropAllDeathLoot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Fox;spawnAtLocation(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity Candyriya$captureFoxDrop(Fox instance, ItemStack itemStack) throws Throwable {
        try {
            Candyriya$spawnNoAdd = true;
            final var result = (ItemEntity) DecorationOps.callsite().invoke(instance, itemStack);
            CandyriyaCaptures.captureExtraDrops(List.of(result));
            return result;
        } finally {
            Candyriya$spawnNoAdd = false;
        }
    }
}
