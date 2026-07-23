package kz.bejiihiu.candyriya.forge.mixin.bukkit;

import kz.bejiihiu.candyriya.common.bridge.bukkit.MessengerBridge;
import kz.bejiihiu.candyriya.common.mod.plugin.messaging.CandyriyaPluginChannel;
import kz.bejiihiu.candyriya.forge.mod.plugin.messaging.CandyriyaForgeMessaging;
import net.minecraft.resources.ResourceLocation;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;
import org.bukkit.plugin.messaging.StandardMessenger;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Set;

@Mixin(value = StandardMessenger.class, remap = false)
public abstract class StandardMessengerMixin_Forge implements Messenger, MessengerBridge {

    @Override
    public CandyriyaPluginChannel<?> Candyriya$setupChannel(ResourceLocation channel, Set<PluginMessageListenerRegistration> incoming, Set<Plugin> outgoing) {
        return CandyriyaForgeMessaging.setupChannel(this, channel, incoming, outgoing);
    }
}
