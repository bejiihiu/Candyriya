package kz.bejiihiu.candyriya.common.mod.server.entity;

import net.minecraft.world.entity.vehicle.VehicleEntity;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftVehicle;

public class CandyriyaModVehicle extends CraftVehicle {

    public CandyriyaModVehicle(CraftServer server, VehicleEntity entity) {
        super(server, entity);
    }
}
