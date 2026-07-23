package kz.bejiihiu.candyriya.common.mixin.bukkit;

import kz.bejiihiu.candyriya.common.mod.util.CandyriyaCaptures;
import kz.bejiihiu.candyriya.mixin.Decorate;
import kz.bejiihiu.candyriya.mixin.DecorationOps;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftEntity;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.inventory.InventoryView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = CraftHumanEntity.class)
public abstract class CraftHumanEntityMixin extends CraftEntity {

    // @formatter:off
    @Shadow(remap = false) public abstract Player getHandle();
    @Shadow(remap = false) public abstract void setHandle(Player entity);
    // @formatter:on

    public CraftHumanEntityMixin(CraftServer server, Entity entity) {
        super(server, entity);
    }

    @Decorate(method = "getOpenInventory", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;getBukkitView()Lorg/bukkit/inventory/InventoryView;"))
    private InventoryView Candyriya$capturePlayer(AbstractContainerMenu instance) throws Throwable {
        Player handle = getHandle();
        try {
            CandyriyaCaptures.captureContainerOwner(handle);
            return (InventoryView) DecorationOps.callsite().invoke(instance);
        } finally {
            CandyriyaCaptures.popContainerOwner(handle);
        }
    }

    @Override
    public void setHandle(Entity entity) {
        setHandle((Player) entity);
    }
}
