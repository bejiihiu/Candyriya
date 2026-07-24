package io.izzel.arclight.common.mixin.bukkit.event;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

// Candyriya start - add Paper API PlayerDeathEvent#getPlayer() [Arclight#1772]
@Mixin(value = PlayerDeathEvent.class, remap = false)
public abstract class PlayerDeathEventMixin {

    @Shadow
    public abstract LivingEntity getEntity();

    /**
     * @author Just-ElC (reported), Candyriya
     * @reason Add Paper API PlayerDeathEvent#getPlayer() for plugin compatibility
     * @see <a href="https://github.com/IzzelAliz/Arclight/issues/1772">Arclight#1772</a>
     */
    public Player getPlayer() {
        return (Player) getEntity();
    }
}
// Candyriya end
