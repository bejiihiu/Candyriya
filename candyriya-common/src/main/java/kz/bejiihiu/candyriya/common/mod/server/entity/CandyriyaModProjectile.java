package kz.bejiihiu.candyriya.common.mod.server.entity;

import net.minecraft.world.entity.projectile.Projectile;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftProjectile;

public class CandyriyaModProjectile extends CraftProjectile {

    public CandyriyaModProjectile(CraftServer server, Projectile entity) {
        super(server, entity);
    }
}
