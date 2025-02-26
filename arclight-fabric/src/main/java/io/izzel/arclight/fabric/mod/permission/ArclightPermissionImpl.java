package io.izzel.arclight.fabric.mod.permission;

import io.izzel.arclight.common.bridge.core.command.CommandSourceStackBridge;
import io.izzel.arclight.i18n.ArclightConfig;
import me.lucko.fabric.api.permissions.v0.PermissionCheckEvent;
import net.fabricmc.fabric.api.util.TriState;

public class ArclightPermissionImpl {
    public static void init() {
        if (!ArclightConfig.spec().getCompat().isForwardPermission()) {
            return;
        }

        PermissionCheckEvent.EVENT.register((provider, permission) -> {
            var sender = ((CommandSourceStackBridge) provider).bridge$getBukkitSender();
            return TriState.of(sender.hasPermission(permission));
        });

        // Fixme: Bukkit didn't support offline player's permission.
//        OfflinePermissionCheckEvent.EVENT.register((uuid, permission) -> {
//            return CompletableFuture.completedFuture(TriState.FALSE);
//        });
    }
}
