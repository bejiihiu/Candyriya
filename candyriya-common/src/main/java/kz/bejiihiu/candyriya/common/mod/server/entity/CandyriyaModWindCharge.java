package kz.bejiihiu.candyriya.common.mod.server.entity;

import net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftAbstractWindCharge;

public class CandyriyaModWindCharge extends CraftAbstractWindCharge {

    public CandyriyaModWindCharge(CraftServer server, AbstractWindCharge entity) {
        super(server, entity);
    }
}
