package kz.bejiihiu.candyriya.common.mixin.core.world.entity.animal;

import kz.bejiihiu.candyriya.common.bridge.core.world.inventory.AbstractContainerMenuBridge;
import org.bukkit.inventory.InventoryView;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "net/minecraft/world/entity/animal/Sheep$1")
public abstract class Sheep1Mixin implements AbstractContainerMenuBridge {

    @Override
    public InventoryView bridge$getBukkitView() {
        return null;
    }
}
