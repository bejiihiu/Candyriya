package io.izzel.arclight.common.mixin.bukkit;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.v.util.CraftNamespacedKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

// Candyriya start - fix NBTAPI mod item/block returning air [Arclight#1467]
@Mixin(value = CraftMagicNumbers.class, remap = false)
public class CraftMagicNumbersMixin {

    // @formatter:off
    @Shadow @Final private static Map<Material, Item> MATERIAL_ITEM;
    @Shadow @Final private static Map<Material, Block> MATERIAL_BLOCK;
    // @formatter:on

    /**
     * Fallback registry lookup when MATERIAL_ITEM map doesn't have the entry for a modded Material.
     * CraftMagicNumbers static init only populates vanilla entries. BukkitRegistry adds modded entries later,
     * but if getItem() is called before registerAll() completes (or map entry is null), plugins like NBTAPI
     * get null → ItemStack.EMPTY → "air".
     *
     * @see <a href="https://github.com/IzzelAliz/Arclight/issues/1467">Arclight#1467</a>
     */
    @Inject(method = "getItem(Lorg/bukkit/Material;)Lnet/minecraft/world/item/Item;", at = @At("HEAD"), cancellable = true, require = 0)
    private static void arclight$getItemFallback(Material material, CallbackInfoReturnable<Item> cir) {
        if (material == null || material.isLegacy()) return;

        Item item = MATERIAL_ITEM.get(material);
        if (item != null) return;

        NamespacedKey key = material.getKey();
        if (key == null) return;

        ResourceLocation rl = CraftNamespacedKey.toMinecraft(key);
        item = BuiltInRegistries.ITEM.get(rl);
        if (item != null && item != Items.AIR) {
            MATERIAL_ITEM.put(material, item);
            cir.setReturnValue(item);
        }
    }

    /**
     * Same fallback for getBlock — modded blocks may also be missing from MATERIAL_BLOCK
     * when CraftMagicNumbers static init runs before BukkitRegistry.
     *
     * @see <a href="https://github.com/IzzelAliz/Arclight/issues/1467">Arclight#1467</a>
     */
    @Inject(method = "getBlock(Lorg/bukkit/Material;)Lnet/minecraft/world/level/block/Block;", at = @At("HEAD"), cancellable = true, require = 0)
    private static void arclight$getBlockFallback(Material material, CallbackInfoReturnable<Block> cir) {
        if (material == null || material.isLegacy()) return;

        Block block = MATERIAL_BLOCK.get(material);
        if (block != null) return;

        NamespacedKey key = material.getKey();
        if (key == null) return;

        ResourceLocation rl = CraftNamespacedKey.toMinecraft(key);
        block = BuiltInRegistries.BLOCK.get(rl);
        if (block != null && block != Blocks.AIR) {
            MATERIAL_BLOCK.put(material, block);
            cir.setReturnValue(block);
        }
    }
}
// Candyriya end
