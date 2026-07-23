package kz.bejiihiu.candyriya.forge.mixin.core.world.level.storage.loot;

import kz.bejiihiu.candyriya.common.bridge.core.world.level.storage.loot.LootContextBridge;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LootContext.class)
public abstract class LootContextMixin_Forge implements LootContextBridge {

}
