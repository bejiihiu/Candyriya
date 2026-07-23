package kz.bejiihiu.candyriya.common.mixin.bukkit;

import kz.bejiihiu.candyriya.common.mod.server.world.CandyriyaWorldConfig;
import kz.bejiihiu.candyriya.mixin.Decorate;
import kz.bejiihiu.candyriya.mixin.DecorationOps;
import org.spigotmc.SpigotWorldConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SpigotWorldConfig.class, remap = false)
public class SpigotWorldConfigMixin {

    @Shadow @Final private String worldName;

    @SuppressWarnings("StringEquality")
    @Decorate(method = "log", inject = true, at = @At("HEAD"))
    private void Candyriya$skipLog(String content) throws Throwable {
        if (worldName == CandyriyaWorldConfig.DEFAULT_MARKER) {
            DecorationOps.cancel().invoke();
            return;
        }
        DecorationOps.blackhole().invoke();
    }
}
