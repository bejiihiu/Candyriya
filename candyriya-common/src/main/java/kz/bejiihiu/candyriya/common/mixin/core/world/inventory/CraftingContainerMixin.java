package kz.bejiihiu.candyriya.common.mixin.core.world.inventory;

import kz.bejiihiu.candyriya.common.bridge.core.world.IInventoryBridge;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CraftingContainer.class)
public interface CraftingContainerMixin extends IInventoryBridge {

    @Override
    default RecipeHolder<?> getCurrentRecipe() {
        return null;
    }

    @Override
    default void setCurrentRecipe(RecipeHolder<?> recipe) {
    }
}
