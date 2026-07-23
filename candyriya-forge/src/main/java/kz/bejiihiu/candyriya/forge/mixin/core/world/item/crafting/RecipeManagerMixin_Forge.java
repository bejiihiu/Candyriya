package kz.bejiihiu.candyriya.forge.mixin.core.world.item.crafting;

import kz.bejiihiu.candyriya.common.bridge.core.world.item.crafting.RecipeManagerBridge;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.common.crafting.conditions.ICondition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin_Forge implements RecipeManagerBridge {

    // @formatter:off
    @Shadow(remap = false) @Final private ICondition.IContext context;
    // @formatter:on
}
