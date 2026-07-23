package kz.bejiihiu.candyriya.common.mixin.core.world.level.storage.loot;

import kz.bejiihiu.candyriya.common.bridge.core.world.level.storage.loot.LootTableBridge;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import org.bukkit.craftbukkit.v.CraftLootTable;
import org.bukkit.craftbukkit.v.util.CraftNamespacedKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LootDataType.class)
public class LootDataTypeMixin {

    @Inject(method = "createLootTableValidator", cancellable = true, at = @At("RETURN"))
    private static void Candyriya$setHandle(CallbackInfoReturnable<LootDataType.Validator<LootTable>> cir) {
        var validator = cir.getReturnValue();
        cir.setReturnValue((validationContext, resourceKey, object) -> {
            validator.run(validationContext, resourceKey, object);
            ((LootTableBridge) object).bridge$setCraftLootTable(new CraftLootTable(CraftNamespacedKey.fromMinecraft(resourceKey.location()), object));
        });
    }
}
