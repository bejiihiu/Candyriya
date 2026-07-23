package kz.bejiihiu.candyriya.neoforge.mixin.bukkit;

import kz.bejiihiu.candyriya.i18n.CandyriyaConfig;
import kz.bejiihiu.candyriya.neoforge.mod.permission.CandyriyaNeoForgePermissible;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.ServerOperator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CraftHumanEntity.class, remap = false)
public abstract class CraftHumanEntityMixin_NeoForge {

    @Redirect(method = "<init>", at = @At(value = "NEW", target = "(Lorg/bukkit/permissions/ServerOperator;)Lorg/bukkit/permissions/PermissibleBase;"))
    private PermissibleBase Candyriya$forge$forwardPerm(ServerOperator opable) {
        if (CandyriyaConfig.spec().getCompat().isForwardPermissionReverse()) {
            return new CandyriyaNeoForgePermissible(opable);
        } else {
            return new PermissibleBase(opable);
        }
    }
}
