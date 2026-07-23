package kz.bejiihiu.candyriya.common.mixin.core.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.tree.CommandNode;
import kz.bejiihiu.candyriya.common.bridge.core.commands.CommandSourceStackBridge;
import kz.bejiihiu.candyriya.common.bridge.core.command.CommandSourceBridge;
import kz.bejiihiu.candyriya.common.bridge.core.server.level.ServerPlayerBridge;
import kz.bejiihiu.candyriya.common.mod.compat.CommandNodeHooks;
import kz.bejiihiu.candyriya.common.mod.server.command.CandyriyaDummyCommandSender;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.command.VanillaCommandWrapper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Objects;

@Mixin(CommandSourceStack.class)
public abstract class CommandSourceStackMixin implements CommandSourceStackBridge {

    // @formatter:off
    @Shadow @Final @Mutable public CommandSource source;
    @Shadow public abstract ServerLevel getLevel();
    @Shadow @Final private int permissionLevel;
    // @formatter:on

    @Override
    public void bridge$setSource(CommandSource source) {
        this.source = source;
    }

    public CommandNode currentCommand;

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "hasPermission", cancellable = true, at = @At("HEAD"))
    public void Candyriya$checkPermission(int level, CallbackInfoReturnable<Boolean> cir) {
        CommandNode currentCommand = bridge$getCurrentCommand();
        if (currentCommand != null) {
            cir.setReturnValue(hasPermission(level, VanillaCommandWrapper.getPermission(currentCommand)));
        }
    }

    @Redirect(method = "broadcastToAdmins", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;isOp(Lcom/mojang/authlib/GameProfile;)Z"))
    private boolean Candyriya$feedbackPermission(PlayerList instance, GameProfile profile) {
        return ((ServerPlayerBridge) instance.getPlayer(profile.getId())).bridge$getBukkitEntity().hasPermission("minecraft.admin.command_feedback");
    }

    public boolean hasPermission(int i, String bukkitPermission) {
        // World is null when loading functions
        return ((getLevel() == null || !((CraftServer) Bukkit.getServer()).ignoreVanillaPermissions) && this.permissionLevel >= i) || getBukkitSender().hasPermission(bukkitPermission);
    }

    @Override
    public boolean bridge$hasPermission(int i, String bukkitPermission) {
        return hasPermission(i, bukkitPermission);
    }

    @Override
    public CommandNode<?> bridge$getCurrentCommand() {
        if (currentCommand == null) {
            return CommandNodeHooks.getCurrent();
        } else {
            return currentCommand;
        }
    }

    @Override
    public void bridge$setCurrentCommand(CommandNode<?> node) {
        this.currentCommand = node;
    }

    public CommandSender getBukkitSender() {
        var thus = (CommandSourceStack) (Object) this;
        var sender = ((CommandSourceBridge) this.source).bridge$getBukkitSender(thus);
        // It means that this is a custom CommandSource
        return Objects.requireNonNullElseGet(sender, () -> new CandyriyaDummyCommandSender(thus));
    }

    @Override
    public CommandSender bridge$getBukkitSender() {
        return getBukkitSender();
    }
}
