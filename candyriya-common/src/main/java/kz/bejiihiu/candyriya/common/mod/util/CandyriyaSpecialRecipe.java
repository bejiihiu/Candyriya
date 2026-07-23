package kz.bejiihiu.candyriya.common.mod.util;

import kz.bejiihiu.candyriya.common.bridge.core.world.item.crafting.RecipeManagerBridge;
import kz.bejiihiu.candyriya.common.mod.server.ArclightServer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v.inventory.CraftComplexRecipe;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.util.CraftNamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class CandyriyaSpecialRecipe extends CraftComplexRecipe {

    private final Recipe<?> recipe;

    public CandyriyaSpecialRecipe(NamespacedKey id, Recipe<?> recipe) {
        super(id, new ItemStack(Material.AIR), null);
        this.recipe = recipe;
    }

    @Override
    public @NotNull ItemStack getResult() {
        return CraftItemStack.asCraftMirror(this.recipe.getResultItem(ArclightServer.getMinecraftServer().registryAccess()));
    }

    @Override
    public void addToCraftingManager() {
        ((RecipeManagerBridge) ArclightServer.getMinecraftServer().getRecipeManager()).bridge$addRecipe(new RecipeHolder<>(CraftNamespacedKey.toMinecraft(this.getKey()), this.recipe));
    }
}
