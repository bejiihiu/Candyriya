package kz.bejiihiu.candyriya.common.mixin.core.world.item.crafting;

import kz.bejiihiu.candyriya.common.bridge.core.world.item.crafting.RecipeBridge;
import kz.bejiihiu.candyriya.common.mod.util.CandyriyaSpecialRecipe;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(net.minecraft.world.item.crafting.Recipe.class)
public interface RecipeMixin extends RecipeBridge {

    default Recipe toBukkitRecipe(NamespacedKey id) {
        return bridge$toBukkitRecipe(id);
    }

    @Override
    default Recipe bridge$toBukkitRecipe(NamespacedKey id) {
        return new CandyriyaSpecialRecipe(id, (net.minecraft.world.item.crafting.Recipe<?>) this);
    }
}
