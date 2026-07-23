package kz.bejiihiu.candyriya.common.mixin.core.world.level;

import kz.bejiihiu.candyriya.common.bridge.core.world.level.GameRulesBridge;
import kz.bejiihiu.candyriya.common.bridge.core.world.level.GameRules_TypeBridge;
import kz.bejiihiu.candyriya.common.bridge.core.world.level.GameRules_ValueBridge;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Set;

@Mixin(GameRules.class)
public abstract class GameRulesMixin implements GameRulesBridge {

    @Shadow
    @Final
    private Map<GameRules.Key<?>, GameRules.Value<?>> rules;

    @Shadow public abstract <T extends GameRules.Value<T>> T getRule(GameRules.Key<T> key);

    @Unique
    public void assignFrom(GameRules gamerules, @Nullable ServerLevel level) {
        ((GameRulesBridge) gamerules).Candyriya$getAllRules().forEach(it -> {
            assignCap(it, gamerules, level);
        });
    }

    @Unique
    private <T extends GameRules.Value<T>> void assignCap(GameRules.Key<T> key, GameRules gamerules, @Nullable ServerLevel level) {
        T t = gamerules.getRule(key);
        ((GameRules_ValueBridge<T>) this.getRule(key)).Candyriya$setFrom(t, level);
    }

    @Override
    public Set<GameRules.Key<?>> Candyriya$getAllRules() {
        return rules.keySet();
    }

    @Inject(method = "register", at = @At("HEAD"))
    private static <T extends GameRules.Value<T>> void Candyriya$initPerWorldCallback(String s, GameRules.Category category, GameRules.Type<T> type, CallbackInfoReturnable<GameRules.Key<T>> cir) {
        GameRules_TypeBridge<T> bridge = (GameRules_TypeBridge<T>) type;
        switch (s) {
            case "reducedDebugInfo" -> bridge.Candyriya$setPerWorldCallback((level, rule) -> {
                boolean value = ((GameRules.BooleanValue) rule).get();
                int i = value ? 22 : 23;
                for (ServerPlayer player: level.players()) {
                    player.connection.send(new ClientboundEntityEventPacket(player, (byte) i));
                }
            });

            case "doLimitedCrafting" -> bridge.Candyriya$setPerWorldCallback((level, rule) -> {
                boolean value = ((GameRules.BooleanValue) rule).get();
                for (ServerPlayer player: level.players()) {
                    player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.LIMITED_CRAFTING, value ? 1.0F : 0.0F));
                }
            });

            case "doImmediateRespawn" -> bridge.Candyriya$setPerWorldCallback((level, rule) -> {
                boolean value = ((GameRules.BooleanValue) rule).get();
                for (ServerPlayer player: level.players()) {
                    player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.IMMEDIATE_RESPAWN, value ? 1.0F : 0.0F));
                }
            });

            case "spawnChunkRadius" -> bridge.Candyriya$setPerWorldCallback((level, rule) -> {
                level.setDefaultSpawnPos(level.getSharedSpawnPos(), level.getSharedSpawnAngle());
            });
        }
    }
}
