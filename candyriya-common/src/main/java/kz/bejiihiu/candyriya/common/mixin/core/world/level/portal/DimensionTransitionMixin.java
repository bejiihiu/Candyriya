package kz.bejiihiu.candyriya.common.mixin.core.world.level.portal;

import kz.bejiihiu.candyriya.common.bridge.core.world.level.portal.DimensionTransitionBridge;
import kz.bejiihiu.candyriya.common.mod.mixins.annotation.CreateConstructor;
import kz.bejiihiu.candyriya.common.mod.mixins.annotation.ShadowConstructor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(DimensionTransition.class)
public class DimensionTransitionMixin implements DimensionTransitionBridge {

    @ShadowConstructor
    public void Candyriya$constructor(ServerLevel newLevel, Vec3 pos, Vec3 speed, float yRot, float xRot, boolean missingRespawnBlock, DimensionTransition.PostDimensionTransition postDimensionTransition) {
        throw new RuntimeException();
    }

    @CreateConstructor
    public void Candyriya$constructor(ServerLevel newLevel, Vec3 pos, Vec3 speed, float yRot, float xRot, boolean missingRespawnBlock, DimensionTransition.PostDimensionTransition postDimensionTransition, PlayerTeleportEvent.TeleportCause cause) {
        Candyriya$constructor(newLevel, pos, speed, yRot, xRot, missingRespawnBlock, postDimensionTransition);
        this.Candyriya$cause = cause;
    }

    @ShadowConstructor
    public void Candyriya$constructor(ServerLevel serverLevel, Vec3 vec3, Vec3 vec32, float f, float g, DimensionTransition.PostDimensionTransition postDimensionTransition) {
        throw new RuntimeException();
    }

    @CreateConstructor
    public void Candyriya$constructor(ServerLevel serverLevel, Vec3 vec3, Vec3 vec32, float f, float g, DimensionTransition.PostDimensionTransition postDimensionTransition, PlayerTeleportEvent.TeleportCause cause) {
        Candyriya$constructor(serverLevel, vec3, vec32, f, g, postDimensionTransition);
        this.Candyriya$cause = cause;
    }

    @Unique private PlayerTeleportEvent.TeleportCause Candyriya$cause;

    @Override
    public void bridge$setTeleportCause(PlayerTeleportEvent.TeleportCause cause) {
        Candyriya$cause = cause;
    }

    @Override
    public PlayerTeleportEvent.TeleportCause bridge$getTeleportCause() {
        return Candyriya$cause == null ? PlayerTeleportEvent.TeleportCause.UNKNOWN : Candyriya$cause;
    }
}
