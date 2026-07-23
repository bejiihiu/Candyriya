package kz.bejiihiu.candyriya.common.mixin.core.world.damagesource;

import kz.bejiihiu.candyriya.common.bridge.core.world.damagesource.CombatEntryBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.CombatEntry;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CombatEntry.class)
public class CombatEntryMixin implements CombatEntryBridge {

    private Component Candyriya$deathMessage;

    @Override
    public void bridge$setDeathMessage(Component component) {
        this.Candyriya$deathMessage = component;
    }

    @Override
    public Component bridge$getDeathMessage() {
        return this.Candyriya$deathMessage;
    }
}
