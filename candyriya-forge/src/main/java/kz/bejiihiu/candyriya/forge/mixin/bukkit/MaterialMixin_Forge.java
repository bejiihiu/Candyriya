package kz.bejiihiu.candyriya.forge.mixin.bukkit;

import kz.bejiihiu.candyriya.common.bridge.bukkit.MaterialBridge;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Material;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = Material.class, remap = false)
public abstract class MaterialMixin_Forge implements MaterialBridge {

    @Override
    public int bridge$forge$getBurnTime(Item item) {
        return new ItemStack(item).getBurnTime(null);
    }
}
