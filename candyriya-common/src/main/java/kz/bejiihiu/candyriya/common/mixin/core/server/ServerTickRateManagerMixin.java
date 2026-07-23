package kz.bejiihiu.candyriya.common.mixin.core.server;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerTickRateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

@Mixin(ServerTickRateManager.class)
public abstract class ServerTickRateManagerMixin {

    // @formatter:off
    @Shadow public abstract boolean stopSprinting();
    // @formatter:on

    private boolean Candyriya$sendLog = true;

    public boolean stopSprinting(boolean sendLog) {
        try {
            Candyriya$sendLog = sendLog;
            return this.stopSprinting();
        } finally {
            Candyriya$sendLog = true;
        }
    }

    @Redirect(method = "finishTickSprint", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/CommandSourceStack;sendSuccess(Ljava/util/function/Supplier;Z)V"))
    private void Candyriya$send(CommandSourceStack source, Supplier<Component> supplier, boolean flag) {
        if (Candyriya$sendLog) {
            source.sendSuccess(supplier, flag);
        }
    }
}
