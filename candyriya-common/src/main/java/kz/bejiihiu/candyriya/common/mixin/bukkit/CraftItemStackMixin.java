package kz.bejiihiu.candyriya.common.mixin.bukkit;

import kz.bejiihiu.candyriya.common.bridge.bukkit.CraftItemStackBridge;
import kz.bejiihiu.candyriya.common.bridge.core.world.item.ItemStackBridge;
import kz.bejiihiu.candyriya.common.mod.server.BukkitRegistry;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.inventory.CraftItemType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = CraftItemStack.class, remap = false)
public abstract class CraftItemStackMixin extends org.bukkit.inventory.ItemStack implements CraftItemStackBridge {

    // @formatter:off
    @Shadow ItemStack handle;
    @Shadow public abstract Material getType();
    @Shadow public abstract short getDurability();
    @Shadow public abstract boolean hasItemMeta();
    // @formatter:on

    /**
     * @author InitAuther97
     * @reason reduce CraftItemType#bukkitToMinecraft call usage.
     * Fixed: use BukkitRegistry.getItem() as fallback for mod materials.
     * Without this, CraftItemType.bukkitToMinecraft() returns null for mod items,
     * causing handle=null and getType() returning AIR — breaking NBTAPI and any
     * plugin that reads item types from CraftItemStack.
     * @see <a href="https://github.com/IzzelAliz/Arclight/issues/1467">Arclight#1467</a>
     */
    @Overwrite
    public void setType(Material type) {
        if (this.getType() != type) {
            if (type == Material.AIR) {
                this.handle = null;
            } else {
                // First try standard Spigot mapping
                var craftType = CraftItemType.bukkitToMinecraft(type);
                // Fallback to BukkitRegistry for mod materials not in CraftMagicNumbers maps
                if (craftType == null) {
                    craftType = BukkitRegistry.getItem(type);
                }
                if (craftType == null) {
                    this.handle = null;
                } else if (this.handle == null) {
                    this.handle = new net.minecraft.world.item.ItemStack(craftType, 1);
                } else {
                    ((ItemStackBridge)(Object) this.handle).Candyriya$setItem(craftType);
                    if (this.hasItemMeta()) {
                        CraftItemStack.setItemMeta(this.handle, CraftItemStack.getItemMeta(this.handle));
                    }
                }
            }

            this.setData(null);
        }
    }

    @Unique
    private ItemEntity Candyriya$itemEntity;

    @Override
    public void Candyriya$setItemEntity(ItemEntity entity) {
        Candyriya$itemEntity = entity;
    }

    @Override
    public ItemEntity Candyriya$getItemEntity() {
        if (Candyriya$itemEntity != null && this.handle != null && !this.handle.isEmpty()) {
            Candyriya$itemEntity.setItem(this.handle);
        }
        return Candyriya$itemEntity;
    }
}
