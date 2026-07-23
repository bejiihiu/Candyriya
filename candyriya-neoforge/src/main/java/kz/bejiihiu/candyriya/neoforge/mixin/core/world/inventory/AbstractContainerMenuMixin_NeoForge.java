package kz.bejiihiu.candyriya.neoforge.mixin.core.world.inventory;

import kz.bejiihiu.candyriya.common.bridge.core.world.inventory.AbstractContainerMenuBridge;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin_NeoForge implements AbstractContainerMenuBridge {

}
