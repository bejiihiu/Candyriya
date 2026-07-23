package kz.bejiihiu.candyriya.common.mod.server.entity;

import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftThrowableProjectile;

public class CandyriyaModThrowableProjectile extends CraftThrowableProjectile {

    public CandyriyaModThrowableProjectile(CraftServer server, ThrowableItemProjectile entity) {
        super(server, entity);
    }
}
