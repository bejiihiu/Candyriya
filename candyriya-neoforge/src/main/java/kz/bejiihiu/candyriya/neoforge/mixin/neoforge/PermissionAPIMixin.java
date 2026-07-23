package kz.bejiihiu.candyriya.neoforge.mixin.neoforge;

import kz.bejiihiu.candyriya.common.mod.server.ArclightServer;
import kz.bejiihiu.candyriya.i18n.CandyriyaConfig;
import kz.bejiihiu.candyriya.neoforge.mod.permission.CandyriyaPermissionHandler;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.handler.IPermissionHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PermissionAPI.class, remap = false)
public abstract class PermissionAPIMixin {

    @Shadow private static IPermissionHandler activeHandler;

    @Inject(method = "initializePermissionAPI", at = @At("RETURN"))
    private static void Candyriya$init(CallbackInfo ci) {
        if (!CandyriyaConfig.spec().getCompat().isForwardPermission()) {
            return;
        }
        var handler = new CandyriyaPermissionHandler(activeHandler);
        ArclightServer.LOGGER.info("Forwarding forge permission[{}] to bukkit", activeHandler.getIdentifier());
        activeHandler = handler;
    }
}
