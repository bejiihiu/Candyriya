package kz.bejiihiu.candyriya.common.mixin.core.world.level;

import kz.bejiihiu.candyriya.common.bridge.core.world.entity.MobBridge;
import kz.bejiihiu.candyriya.common.bridge.core.world.level.WorldBridge;
import kz.bejiihiu.candyriya.common.bridge.core.world.level.BaseSpawnerBridge;
import kz.bejiihiu.candyriya.mixin.Decorate;
import kz.bejiihiu.candyriya.mixin.DecorationOps;
import kz.bejiihiu.candyriya.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.SpawnData;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BaseSpawner.class)
public abstract class BaseSpawnerMixin implements BaseSpawnerBridge {

    // @formatter:off
    @Shadow public SimpleWeightedRandomList<SpawnData> spawnPotentials;
    // @formatter:on

    @Inject(method = "setEntityId", at = @At("RETURN"))
    public void Candyriya$clearMobs(CallbackInfo ci) {
        this.spawnPotentials = SimpleWeightedRandomList.empty();
    }

    @Decorate(method = "serverTick", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/SpawnData;getEquipment()Ljava/util/Optional;"))
    private void Candyriya$nerf(@Local(ordinal = -1) Mob mob) {
        if (((WorldBridge) mob.level()).bridge$spigotConfig().nerfSpawnerMobs) {
            ((MobBridge) mob).bridge$setAware(false);
        }
    }

    @Decorate(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tryAddFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean Candyriya$spawnerSpawn(ServerLevel instance, Entity entity, ServerLevel level, BlockPos pos) throws Throwable {
        if (CraftEventFactory.callSpawnerSpawnEvent(entity, pos).isCancelled()) {
            throw DecorationOps.jumpToLoopStart();
        }
        ((WorldBridge) instance).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.SPAWNER);
        return (boolean) DecorationOps.callsite().invoke(instance, entity);
    }
}
