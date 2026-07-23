package kz.bejiihiu.candyriya.common.mod.server.entity;

import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftMinecartContainer;

public class CandyriyaModMinecartContainer extends CraftMinecartContainer {

    public CandyriyaModMinecartContainer(CraftServer server, AbstractMinecartContainer entity) {
        super(server, entity);
    }
}
