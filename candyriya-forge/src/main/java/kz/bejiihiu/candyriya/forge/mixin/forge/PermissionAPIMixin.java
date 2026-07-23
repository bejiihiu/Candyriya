package kz.bejiihiu.candyriya.forge.mixin.forge;

import kz.bejiihiu.candyriya.forge.mod.permission.CandyriyaPermissionHandler;
import kz.bejiihiu.candyriya.common.mod.server.CandyriyaServer;
import kz.bejiihiu.candyriya.i18n.CandyriyaConfig;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.handler.IPermissionHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PermissionAPI.class, remap = false)
public class PermissionAPIMixin {

    @Shadow private static IPermissionHandler activeHandler;

    @Inject(method = "initializePermissionAPI", at = @At("RETURN"))
    private static void Candyriya$init(CallbackInfo ci) {
        if (!CandyriyaConfig.spec().getCompat().isForwardPermission()) {
            return;
        }
        var handler = new CandyriyaPermissionHandler(activeHandler);
        CandyriyaServer.LOGGER.info("Forwarding forge permission[{}] to bukkit", activeHandler.getIdentifier());
        activeHandler = handler;
    }
}
