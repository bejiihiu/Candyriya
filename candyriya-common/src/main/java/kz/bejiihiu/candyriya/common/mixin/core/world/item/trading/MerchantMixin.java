package kz.bejiihiu.candyriya.common.mixin.core.world.item.trading;

import kz.bejiihiu.candyriya.common.bridge.core.world.item.trading.MerchantBridge;
import net.minecraft.world.item.trading.Merchant;
import org.bukkit.craftbukkit.v.inventory.CraftMerchant;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Merchant.class)
public interface MerchantMixin extends MerchantBridge {

    default CraftMerchant getCraftMerchant() {
        return bridge$getCraftMerchant();
    }
}
