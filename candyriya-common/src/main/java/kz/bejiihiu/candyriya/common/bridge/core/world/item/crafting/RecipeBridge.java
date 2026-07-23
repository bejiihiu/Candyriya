package kz.bejiihiu.candyriya.common.bridge.core.world.item.crafting;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;

public interface RecipeBridge {

    Recipe bridge$toBukkitRecipe(NamespacedKey id);
}
