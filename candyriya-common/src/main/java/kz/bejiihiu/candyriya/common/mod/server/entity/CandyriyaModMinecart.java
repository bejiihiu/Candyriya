package kz.bejiihiu.candyriya.common.mod.server.entity;

import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftMinecart;

public class CandyriyaModMinecart extends CraftMinecart {

    public CandyriyaModMinecart(CraftServer server, AbstractMinecart entity) {
        super(server, entity);
    }
}
