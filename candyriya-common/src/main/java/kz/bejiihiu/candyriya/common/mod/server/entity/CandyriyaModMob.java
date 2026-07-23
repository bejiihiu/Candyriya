package kz.bejiihiu.candyriya.common.mod.server.entity;

import net.minecraft.world.entity.Mob;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftMob;

public class CandyriyaModMob extends CraftMob {

    public CandyriyaModMob(CraftServer server, Mob entity) {
        super(server, entity);
    }
}
