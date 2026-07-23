package kz.bejiihiu.candyriya.common.mixin.core.world.inventory;

import kz.bejiihiu.candyriya.common.bridge.core.world.entity.player.PlayerBridge;
import kz.bejiihiu.candyriya.common.bridge.core.world.IInventoryBridge;
import kz.bejiihiu.candyriya.common.mixin.core.world.SimpleContainerMixin;
import kz.bejiihiu.candyriya.common.mod.mixins.annotation.CreateConstructor;
import kz.bejiihiu.candyriya.common.mod.mixins.annotation.ShadowConstructor;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PlayerEnderChestContainer.class)
public abstract class PlayerEnderChestContainerMixin extends SimpleContainerMixin implements IInventoryBridge, Container {

    // @formatter:off
    @Shadow private EnderChestBlockEntity activeChest;
    // @formatter:on

    private Player owner;

    @ShadowConstructor.Super
    public void Candyriya$constructor$super(int numSlots, InventoryHolder owner) {
        throw new RuntimeException();
    }

    @CreateConstructor
    public void Candyriya$constructor(Player owner) {
        Candyriya$constructor$super(27, ((PlayerBridge) owner).bridge$getBukkitEntity());
        this.owner = owner;
    }

    public InventoryHolder getBukkitOwner() {
        return ((PlayerBridge) owner).bridge$getBukkitEntity();
    }

    @Override
    public InventoryHolder getOwner() {
        return ((PlayerBridge) owner).bridge$getBukkitEntity();
    }

    @Override
    public void setOwner(InventoryHolder owner) {
        if (owner instanceof HumanEntity) {
            this.owner = ((CraftHumanEntity) owner).getHandle();
        }
    }

    @Override
    public Location getLocation() {
        return CraftBlock.at(this.activeChest.getLevel(), this.activeChest.getBlockPos()).getLocation();
    }
}
