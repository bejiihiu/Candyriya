package kz.bejiihiu.candyriya.fabric.mod.permission;

import kz.bejiihiu.candyriya.common.bridge.core.commands.CommandSourceStackBridge;
import kz.bejiihiu.candyriya.i18n.CandyriyaConfig;
import me.lucko.fabric.api.permissions.v0.PermissionCheckEvent;
import net.fabricmc.fabric.api.util.TriState;

public class CandyriyaPermissionImpl {
    public static void init() {
        if (!CandyriyaConfig.spec().getCompat().isForwardPermission()) {
            return;
        }

        PermissionCheckEvent.EVENT.register((provider, permission) -> {
            if (provider instanceof CommandSourceStackBridge stack) {
                var sender = stack.bridge$getBukkitSender();
                if (sender != null) {
                    return TriState.of(sender.hasPermission(permission));
                }
            }
            return TriState.DEFAULT;
        });

        // Fixme: Bukkit didn't support offline player's permission.
//        OfflinePermissionCheckEvent.EVENT.register((uuid, permission) -> {
//            return CompletableFuture.completedFuture(TriState.FALSE);
//        });
    }
}
