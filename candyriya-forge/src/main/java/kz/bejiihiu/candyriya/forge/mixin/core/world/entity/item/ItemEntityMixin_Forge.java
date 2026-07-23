package kz.bejiihiu.candyriya.forge.mixin.core.world.entity.item;

import kz.bejiihiu.candyriya.common.bridge.core.world.entity.item.ItemEntityBridge;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemEntity.class)
public class ItemEntityMixin_Forge implements ItemEntityBridge {
}
