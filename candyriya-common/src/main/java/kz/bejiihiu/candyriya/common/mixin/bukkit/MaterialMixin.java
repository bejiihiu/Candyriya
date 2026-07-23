package kz.bejiihiu.candyriya.common.mixin.bukkit;

import com.google.common.collect.ImmutableMap;
import kz.bejiihiu.candyriya.common.bridge.bukkit.MaterialBridge;
import kz.bejiihiu.candyriya.common.bridge.core.world.level.block.FireBlockBridge;
import kz.bejiihiu.candyriya.common.mod.server.CandyriyaServer;
import kz.bejiihiu.candyriya.i18n.LocalizedException;
import kz.bejiihiu.candyriya.i18n.conf.MaterialPropertySpec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.block.CraftBlockState;
import org.bukkit.craftbukkit.v.block.CraftBlockStates;
import org.bukkit.craftbukkit.v.inventory.CraftMetaArmorStand;
import org.bukkit.craftbukkit.v.inventory.CraftMetaBanner;
import org.bukkit.craftbukkit.v.inventory.CraftMetaBlockState;
import org.bukkit.craftbukkit.v.inventory.CraftMetaBook;
import org.bukkit.craftbukkit.v.inventory.CraftMetaBookSigned;
import org.bukkit.craftbukkit.v.inventory.CraftMetaCharge;
import org.bukkit.craftbukkit.v.inventory.CraftMetaCrossbow;
import org.bukkit.craftbukkit.v.inventory.CraftMetaEnchantedBook;
import org.bukkit.craftbukkit.v.inventory.CraftMetaFirework;
import org.bukkit.craftbukkit.v.inventory.CraftMetaItem;
import org.bukkit.craftbukkit.v.inventory.CraftMetaKnowledgeBook;
import org.bukkit.craftbukkit.v.inventory.CraftMetaLeatherArmor;
import org.bukkit.craftbukkit.v.inventory.CraftMetaMap;
import org.bukkit.craftbukkit.v.inventory.CraftMetaPotion;
import org.bukkit.craftbukkit.v.inventory.CraftMetaSkull;
import org.bukkit.craftbukkit.v.inventory.CraftMetaSpawnEgg;
import org.bukkit.craftbukkit.v.inventory.CraftMetaSuspiciousStew;
import org.bukkit.craftbukkit.v.inventory.CraftMetaTropicalFishBucket;
import org.bukkit.craftbukkit.v.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.v.util.CraftNamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

@Mixin(value = Material.class, remap = false)
public abstract class MaterialMixin implements MaterialBridge {

    // @formatter:off
    @Shadow @Mutable @Final private NamespacedKey key;
    @Shadow @Mutable @Final private Constructor<? extends MaterialData> ctor;
    @Shadow @Mutable @Final public Class<?> data;
    @Shadow public abstract boolean isBlock();
    // @formatter:on

    private static final Map<String, BiFunction<Material, CraftMetaItem, ItemMeta>> TYPES = ImmutableMap
        .<String, BiFunction<Material, CraftMetaItem, ItemMeta>>builder()
        .put("ARMOR_STAND", (a, b) -> b instanceof CraftMetaArmorStand ? b : new CraftMetaArmorStand(b))
        .put("BANNER", (a, b) -> b instanceof CraftMetaBanner ? b : new CraftMetaBanner(b))
        .put("TILE_ENTITY", (a, b) -> new CraftMetaBlockState(b, a))
        .put("BOOK", (a, b) -> b != null && b.getClass().equals(CraftMetaBook.class) ? b : new CraftMetaBook(b))
        .put("BOOK_SIGNED", (a, b) -> b instanceof CraftMetaBookSigned ? b : new CraftMetaBookSigned(b))
        .put("SKULL", (a, b) -> b instanceof CraftMetaSkull ? b : new CraftMetaSkull(b))
        .put("LEATHER_ARMOR", (a, b) -> b instanceof CraftMetaLeatherArmor ? b : new CraftMetaLeatherArmor(b))
        .put("MAP", (a, b) -> b instanceof CraftMetaMap ? b : new CraftMetaMap(b))
        .put("POTION", (a, b) -> b instanceof CraftMetaPotion ? b : new CraftMetaPotion(b))
        .put("SPAWN_EGG", (a, b) -> b instanceof CraftMetaSpawnEgg ? b : new CraftMetaSpawnEgg(b))
        .put("ENCHANTED", (a, b) -> b instanceof CraftMetaEnchantedBook ? b : new CraftMetaEnchantedBook(b))
        .put("FIREWORK", (a, b) -> b instanceof CraftMetaFirework ? b : new CraftMetaFirework(b))
        .put("FIREWORK_EFFECT", (a, b) -> b instanceof CraftMetaCharge ? b : new CraftMetaCharge(b))
        .put("KNOWLEDGE_BOOK", (a, b) -> b instanceof CraftMetaKnowledgeBook ? b : new CraftMetaKnowledgeBook(b))
        .put("TROPICAL_FISH_BUCKET", (a, b) -> b instanceof CraftMetaTropicalFishBucket ? b : new CraftMetaTropicalFishBucket(b))
        .put("CROSSBOW", (a, b) -> b instanceof CraftMetaCrossbow ? b : new CraftMetaCrossbow(b))
        .put("SUSPICIOUS_STEW", (a, b) -> b instanceof CraftMetaSuspiciousStew ? b : new CraftMetaSuspiciousStew(b))
        .put("UNSPECIFIC", (a, b) -> new CraftMetaItem(b))
        .put("NULL", (a, b) -> null)
        .build();

    private MaterialPropertySpec.MaterialType Candyriya$type = MaterialPropertySpec.MaterialType.VANILLA;
    private MaterialPropertySpec Candyriya$spec;
    private ResourceLocation Candyriya$location;
    private boolean Candyriya$block = false, Candyriya$item = false;

    @Override
    public void bridge$setBlock() {
        this.Candyriya$block = true;
    }

    @Override
    public void bridge$setItem() {
        this.Candyriya$item = true;
    }

    @Inject(method = "isBlock", cancellable = true, at = @At("HEAD"))
    private void Candyriya$isBlock(CallbackInfoReturnable<Boolean> cir) {
        if (Candyriya$type != MaterialPropertySpec.MaterialType.VANILLA) {
            cir.setReturnValue(Candyriya$block);
        }
    }

    @Inject(method = "isItem", cancellable = true, at = @At("HEAD"))
    private void Candyriya$isItem(CallbackInfoReturnable<Boolean> cir) {
        if (Candyriya$type != MaterialPropertySpec.MaterialType.VANILLA) {
            cir.setReturnValue(Candyriya$item);
        }
    }

    @Inject(method = "isEdible", cancellable = true, at = @At("HEAD"))
    private void Candyriya$isEdible(CallbackInfoReturnable<Boolean> cir) {
        if (Candyriya$spec != null) {
            if (!Candyriya$spec.isPresent) {
                Candyriya$setupCommon();
            }
            cir.setReturnValue(Candyriya$spec.edible);
        }
    }

    @Inject(method = "isRecord", cancellable = true, at = @At("HEAD"))
    private void Candyriya$isRecord(CallbackInfoReturnable<Boolean> cir) {
        if (Candyriya$spec != null) {
            if (!Candyriya$spec.isPresent) {
                Candyriya$setupCommon();
            }
            cir.setReturnValue(Candyriya$spec.record);
        }
    }

    @Inject(method = "isSolid", cancellable = true, at = @At("HEAD"))
    private void Candyriya$isSolid(CallbackInfoReturnable<Boolean> cir) {
        if (Candyriya$spec != null) {
            if (!Candyriya$spec.isPresent) {
                Candyriya$setupCommon();
            }
            cir.setReturnValue(Candyriya$spec.solid);
        }
    }

    @Inject(method = "isAir", cancellable = true, at = @At("HEAD"))
    private void Candyriya$isAir(CallbackInfoReturnable<Boolean> cir) {
        if (Candyriya$spec != null) {
            if (!Candyriya$spec.isPresent) {
                Candyriya$setupCommon();
            }
            cir.setReturnValue(Candyriya$spec.air);
        }
    }

    @Inject(method = "isTransparent", cancellable = true, at = @At("HEAD"))
    private void Candyriya$isTransparent(CallbackInfoReturnable<Boolean> cir) {
        if (Candyriya$spec != null) {
            if (!Candyriya$spec.isPresent) {
                Candyriya$setupCommon();
            }
            cir.setReturnValue(Candyriya$spec.transparent);
        }
    }

    @Inject(method = "isFlammable", cancellable = true, at = @At("HEAD"))
    private void Candyriya$isFlammable(CallbackInfoReturnable<Boolean> cir) {
        if (Candyriya$spec != null) {
            if (!Candyriya$spec.isPresent) {
                Candyriya$setupCommon();
            }
            cir.setReturnValue(Candyriya$spec.flammable);
        }
    }

    @Inject(method = "isBurnable", cancellable = true, at = @At("HEAD"))
    private void Candyriya$isBurnable(CallbackInfoReturnable<Boolean> cir) {
        if (Candyriya$spec != null) {
            if (!Candyriya$spec.isPresent) {
                Candyriya$setupCommon();
            }
            cir.setReturnValue(Candyriya$spec.burnable);
        }
    }

    @Inject(method = "isFuel", cancellable = true, at = @At("HEAD"))
    private void Candyriya$isFuel(CallbackInfoReturnable<Boolean> cir) {
        if (Candyriya$spec != null) {
            if (!Candyriya$spec.isPresent) {
                Candyriya$setupCommon();
            }
            cir.setReturnValue(Candyriya$spec.fuel);
        }
    }

    @Inject(method = "isOccluding", cancellable = true, at = @At("HEAD"))
    private void Candyriya$isOccluding(CallbackInfoReturnable<Boolean> cir) {
        if (Candyriya$spec != null) {
            if (!Candyriya$spec.isPresent) {
                Candyriya$setupCommon();
            }
            cir.setReturnValue(Candyriya$spec.occluding);
        }
    }

    @Inject(method = "hasGravity", cancellable = true, at = @At("HEAD"))
    private void Candyriya$hasGravity(CallbackInfoReturnable<Boolean> cir) {
        if (Candyriya$spec != null) {
            if (!Candyriya$spec.isPresent) {
                Candyriya$setupCommon();
            }
            cir.setReturnValue(Candyriya$spec.gravity);
        }
    }

    @Inject(method = "isInteractable", cancellable = true, at = @At("HEAD"))
    private void Candyriya$isInteractable(CallbackInfoReturnable<Boolean> cir) {
        if (Candyriya$spec != null) {
            if (!Candyriya$spec.isPresent) {
                Candyriya$setupCommon();
            }
            cir.setReturnValue(Candyriya$spec.interactable);
        }
    }

    @Inject(method = "getHardness", cancellable = true, at = @At("HEAD"))
    private void Candyriya$getHardness(CallbackInfoReturnable<Float> cir) {
        if (Candyriya$spec != null) {
            if (!Candyriya$spec.isPresent) {
                Candyriya$setupCommon();
            }
            cir.setReturnValue(Candyriya$spec.hardness);
        }
    }

    @Inject(method = "getBlastResistance", cancellable = true, at = @At("HEAD"))
    private void Candyriya$getBlastResistance(CallbackInfoReturnable<Float> cir) {
        if (Candyriya$spec != null) {
            if (!Candyriya$spec.isPresent) {
                Candyriya$setupCommon();
            }
            cir.setReturnValue(Candyriya$spec.blastResistance);
        }
    }

    @Inject(method = "getMaxStackSize", cancellable = true, at = @At("HEAD"))
    private void Candyriya$getMaxStackSize(CallbackInfoReturnable<Integer> cir) {
        if (Candyriya$spec != null) {
            if (!Candyriya$spec.isPresent) {
                Candyriya$setupCommon();
            }
            cir.setReturnValue(Candyriya$spec.maxStack);
        }
    }

    @Inject(method = "getMaxDurability", cancellable = true, at = @At("HEAD"))
    private void Candyriya$getMaxDurability(CallbackInfoReturnable<Short> cir) {
        if (Candyriya$spec != null) {
            if (!Candyriya$spec.isPresent) {
                Candyriya$setupCommon();
            }
            if (Candyriya$spec.maxDurability != null) {
                cir.setReturnValue(Candyriya$spec.maxDurability.shortValue());
            }
        }
    }

    @Inject(method = "getCraftingRemainingItem", cancellable = true, at = @At("HEAD"))
    private void Candyriya$getCraftingRemainingItem(CallbackInfoReturnable<Material> cir) {
        if (Candyriya$spec != null) {
            if (!Candyriya$spec.isPresent) {
                Candyriya$setupCommon();
            }
            if (Candyriya$spec.craftingRemainingItem != null) {
                cir.setReturnValue(CraftMagicNumbers.getMaterial(BuiltInRegistries.ITEM.get(ResourceLocation.parse(Candyriya$spec.craftingRemainingItem))));
            }
        }
    }

    @Override
    public MaterialPropertySpec bridge$getSpec() {
        return Candyriya$spec;
    }

    @Override
    public MaterialPropertySpec.MaterialType bridge$getType() {
        return Candyriya$type;
    }

    private Function<CraftMetaItem, ItemMeta> Candyriya$metaFunc;

    @Override
    public Function<CraftMetaItem, ItemMeta> bridge$itemMetaFactory() {
        if (Candyriya$metaFunc == null && !Candyriya$spec.isPresent) {
            Candyriya$setupCommon();
        }
        return Candyriya$metaFunc;
    }

    @Override
    public void bridge$setItemMetaFactory(Function<CraftMetaItem, ItemMeta> func) {
        this.Candyriya$metaFunc = func;
    }

    private Function<CraftBlock, BlockState> Candyriya$stateFunc;

    @Override
    public Function<CraftBlock, BlockState> bridge$blockStateFactory() {
        if (Candyriya$stateFunc == null && !Candyriya$spec.isPresent) {
            Candyriya$setupCommon();
        }
        return Candyriya$stateFunc;
    }

    @Override
    public void bridge$setBlockStateFactory(Function<CraftBlock, BlockState> func) {
        this.Candyriya$stateFunc = func;
    }

    @Override
    public void bridge$setupBlock(ResourceLocation key, MaterialPropertySpec spec) {
        this.Candyriya$spec = spec.clone();
        Candyriya$type = MaterialPropertySpec.MaterialType.FORGE;
        Candyriya$block = true;
        Candyriya$setupCommonLazy(key);
    }

    @Override
    public void bridge$setupVanillaBlock(MaterialPropertySpec spec) {
        if (spec.isPresent) {
            this.Candyriya$spec = spec.clone();
            this.setupBlockStateFunc();
        }
    }

    @Override
    public void bridge$setupItem(ResourceLocation key, MaterialPropertySpec spec) {
        this.Candyriya$spec = spec.clone();
        Candyriya$type = MaterialPropertySpec.MaterialType.FORGE;
        Candyriya$item = true;
        Candyriya$setupCommonLazy(key);
    }

    @Override
    public boolean bridge$shouldApplyStateFactory() {
        return this.Candyriya$type != MaterialPropertySpec.MaterialType.VANILLA ||
            (this.Candyriya$spec != null && this.Candyriya$spec.blockStateClass != null);
    }

    @SuppressWarnings("unchecked")
    private void Candyriya$setupCommonLazy(ResourceLocation key) {
        this.key = CraftNamespacedKey.fromMinecraft(key);
        if (Candyriya$spec.materialDataClass != null) {
            try {
                Class<?> data = Class.forName(Candyriya$spec.materialDataClass);
                if (MaterialData.class.isAssignableFrom(data)) {
                    this.data = data;
                    this.ctor = (Constructor<? extends MaterialData>) data.getConstructor(Material.class, byte.class);
                }
            } catch (Exception e) {
                CandyriyaServer.LOGGER.warn("Bad material data class {} for {}", Candyriya$spec.materialDataClass, this);
                CandyriyaServer.LOGGER.warn(e);
            }
        }
        Candyriya$location = key;
    }

    private void Candyriya$setupCommon() {
        Block block = BuiltInRegistries.BLOCK.get(Candyriya$location);
        Item item = BuiltInRegistries.ITEM.get(Candyriya$location);

        // Block properties
        if (Candyriya$location.equals(MaterialBridge.AIR) || block != Blocks.AIR) {
            if (Candyriya$spec.solid == null) {
                Candyriya$spec.solid = block.defaultBlockState().canOcclude();
            }
            if (Candyriya$spec.air == null) {
                Candyriya$spec.air = block.defaultBlockState().isAir();
            }
            if (Candyriya$spec.transparent == null) {
                Candyriya$spec.transparent = block.defaultBlockState().useShapeForLightOcclusion();
            }
            if (Candyriya$spec.flammable == null) {
                Candyriya$spec.flammable = ((FireBlockBridge) Blocks.FIRE).bridge$canBurn(block);
            }
            if (Candyriya$spec.burnable == null) {
                Candyriya$spec.burnable = ((FireBlockBridge) Blocks.FIRE).bridge$canBurn(block);
            }
            if (Candyriya$spec.hardness == null) {
                Candyriya$spec.hardness = block.defaultBlockState().destroySpeed;
            }
            if (Candyriya$spec.blastResistance == null) {
                Candyriya$spec.blastResistance = block.getExplosionResistance();
            }
        }

        // Item properties
        if (Candyriya$spec.maxStack == null) {
            Candyriya$spec.maxStack = bridge$forge$getMaxStackSize(item);
        }
        if (Candyriya$spec.maxDurability == null) {
            Candyriya$spec.maxDurability = bridge$forge$getDurability(item);
        }
        if (Candyriya$spec.edible == null) {
            Candyriya$spec.edible = false;
        }
        if (Candyriya$spec.record == null) {
            Candyriya$spec.record = false;
        }
        if (Candyriya$spec.fuel == null) {
            Candyriya$spec.fuel = bridge$forge$getBurnTime(item) > 0;
        }
        if (Candyriya$spec.occluding == null) {
            Candyriya$spec.occluding = Candyriya$spec.solid;
        }
        if (Candyriya$spec.gravity == null) {
            Candyriya$spec.gravity = block instanceof FallingBlock;
        }
        if (Candyriya$spec.interactable == null) {
            Candyriya$spec.interactable = true;
        }
        if (Candyriya$spec.craftingRemainingItem == null) {
            // noinspection deprecation
            final var remaining = bridge$getCraftRemainingItem(item);
            Candyriya$spec.craftingRemainingItem = remaining != null ? remaining.toString() : null;
        }
        if (Candyriya$spec.itemMetaType == null) {
            Candyriya$spec.itemMetaType = "UNSPECIFIC";
        }
        Candyriya$spec.present();

        BiFunction<Material, CraftMetaItem, ItemMeta> function = TYPES.get(Candyriya$spec.itemMetaType);
        if (function != null) {
            this.Candyriya$metaFunc = meta -> function.apply((Material) (Object) this, meta);
        } else {
            this.Candyriya$metaFunc = dynamicMetaCreator(Candyriya$spec.itemMetaType);
        }
        this.setupBlockStateFunc();
    }

    private void setupBlockStateFunc() {
        if (Candyriya$spec.blockStateClass != null && !Candyriya$spec.blockStateClass.equalsIgnoreCase("auto")) {
            try {
                Class<?> cl = Class.forName(Candyriya$spec.blockStateClass);
                if (!CraftBlockState.class.isAssignableFrom(cl)) {
                    throw LocalizedException.checked("registry.block-state.not-subclass", cl, CraftBlockState.class);
                }
                for (Constructor<?> constructor : cl.getDeclaredConstructors()) {
                    if (constructor.getParameterTypes().length == 1
                        && org.bukkit.block.Block.class.isAssignableFrom(constructor.getParameterTypes()[0])) {
                        constructor.setAccessible(true);
                        this.Candyriya$stateFunc = b -> {
                            try {
                                return (BlockState) constructor.newInstance(b);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        };
                    }
                }
            } catch (Exception e) {
                if (e instanceof LocalizedException) {
                    CandyriyaServer.LOGGER.warn(((LocalizedException) e).node(), ((LocalizedException) e).args());
                } else {
                    CandyriyaServer.LOGGER.warn("registry.block-state.error", this, Candyriya$spec.blockStateClass, e);
                }
            }
            if (this.Candyriya$stateFunc == null) {
                CandyriyaServer.LOGGER.warn("registry.block-state.no-candidate", this, Candyriya$spec.blockStateClass);
            }
        }
        if (this.Candyriya$stateFunc == null) {
            this.Candyriya$stateFunc = CraftBlockStates::getBlockState;
        }
    }

    private Function<CraftMetaItem, ItemMeta> dynamicMetaCreator(String type) {
        Function<CraftMetaItem, ItemMeta> candidate = null;
        try {
            Class<?> cl = Class.forName(type);
            if (!CraftMetaItem.class.isAssignableFrom(cl)) {
                throw LocalizedException.checked("registry.meta-type.not-subclass", cl, CraftMetaItem.class);
            }
            for (Constructor<?> constructor : cl.getDeclaredConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == 1) {
                    if (parameterTypes[0] == Material.class) {
                        constructor.setAccessible(true);
                        candidate = meta -> {
                            try {
                                return (ItemMeta) constructor.newInstance(this);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        };
                        break;
                    } else if (CraftMetaItem.class.isAssignableFrom(parameterTypes[0])) {
                        constructor.setAccessible(true);
                        candidate = meta -> {
                            try {
                                return (ItemMeta) constructor.newInstance(meta);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        };
                        break;
                    }
                } else if (parameterTypes.length == 2) {
                    if (parameterTypes[0] == Material.class && CraftMetaItem.class.isAssignableFrom(parameterTypes[1])) {
                        constructor.setAccessible(true);
                        candidate = meta -> {
                            try {
                                return (ItemMeta) constructor.newInstance(this, meta);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        };
                        break;
                    } else if (parameterTypes[1] == Material.class && CraftMetaItem.class.isAssignableFrom(parameterTypes[0])) {
                        constructor.setAccessible(true);
                        candidate = meta -> {
                            try {
                                return (ItemMeta) constructor.newInstance(meta, this);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        };
                        break;
                    }
                }
            }
        } catch (Exception e) {
            if (e instanceof LocalizedException) {
                CandyriyaServer.LOGGER.warn(((LocalizedException) e).node(), ((LocalizedException) e).args());
            } else {
                CandyriyaServer.LOGGER.warn("registry.meta-type.error", this, type, e);
            }
        }
        if (candidate == null) {
            CandyriyaServer.LOGGER.warn("registry.meta-type.no-candidate", this, type);
            candidate = CraftMetaItem::new;
        }
        return candidate;
    }
}
