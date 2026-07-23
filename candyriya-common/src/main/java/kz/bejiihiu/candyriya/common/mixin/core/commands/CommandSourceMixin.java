package kz.bejiihiu.candyriya.common.mixin.core.commands;

import kz.bejiihiu.candyriya.common.bridge.core.command.CommandSourceBridge;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CommandSource.class)
public interface CommandSourceMixin extends CommandSourceBridge {

    default CommandSender getBukkitSender(CommandSourceStack wrapper) {
        return this.bridge$getBukkitSender(wrapper);
    }
}
