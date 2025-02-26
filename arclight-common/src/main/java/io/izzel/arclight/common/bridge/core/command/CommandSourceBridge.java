package io.izzel.arclight.common.bridge.core.command;

import net.minecraft.commands.CommandSourceStack;
import org.bukkit.command.CommandSender;

public interface CommandSourceBridge {

    CommandSender bridge$getBukkitSender(CommandSourceStack wrapper);
}
