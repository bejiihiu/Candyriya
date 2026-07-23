package kz.bejiihiu.candyriya.common.mod.server.entity;

import net.minecraft.world.entity.raid.Raider;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftRaider;

public class CandyriyaModRaider extends CraftRaider {

    public CandyriyaModRaider(CraftServer server, Raider entity) {
        super(server, entity);
    }
}
