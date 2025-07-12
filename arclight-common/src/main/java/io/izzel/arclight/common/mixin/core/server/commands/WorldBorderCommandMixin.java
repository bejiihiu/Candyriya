package io.izzel.arclight.common.mixin.core.server.commands;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.WorldBorderCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WorldBorderCommand.class)
public class WorldBorderCommandMixin {
    @Decorate(method = {"setCenter", "setDamageAmount", "setSize", "setDamageBuffer", "setWarningDistance", "setWarningTime"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getWorldBorder()Lnet/minecraft/world/level/border/WorldBorder;"))
    private static WorldBorder arclight$useRespectiveWorldBorder(ServerLevel instance, CommandSourceStack stack) throws Throwable {
        return (WorldBorder) DecorationOps.callsite().invoke(stack.getLevel());
    }
}
