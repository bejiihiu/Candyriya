package io.izzel.arclight.common.mod.server.entity;

import net.minecraft.world.entity.LivingEntity;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v.inventory.CraftEntityEquipment;
import org.bukkit.inventory.EntityEquipment;

// Candyriya start - fix NPE in CMI /killall when LivingEntity.getEquipment() returns null [Arclight#2029]
// CraftLivingEntity only creates equipment for Mob and ArmorStand entities.
// Modded living entities (e.g. Born in Chaos, Creeper Overhaul) get null from getEquipment(),
// causing plugins like CMI to crash with NullPointerException.
public class ArclightModLivingEntity extends CraftLivingEntity {

    private CraftEntityEquipment equipment;

    public ArclightModLivingEntity(CraftServer server, LivingEntity entity) {
        super(server, entity);
    }

    @Override
    public EntityEquipment getEquipment() {
        if (super.getEquipment() != null) {
            return super.getEquipment();
        }
        if (this.equipment == null) {
            this.equipment = new CraftEntityEquipment(this);
        }
        return this.equipment;
    }
}
// Candyriya end
