package kz.bejiihiu.candyriya.common.bridge.bukkit;

import kz.bejiihiu.candyriya.common.mod.plugin.messaging.CandyriyaPluginChannel;
import kz.bejiihiu.candyriya.common.mod.plugin.messaging.PacketRecorder;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;

import java.util.Set;

public interface MessengerBridge {
    Object2BooleanOpenHashMap<String> valid = new Object2BooleanOpenHashMap<>();

    CandyriyaPluginChannel<?> Candyriya$setupChannel(ResourceLocation channel, Set<PluginMessageListenerRegistration> incoming, Set<Plugin> outgoing);

    void Candyriya$sendCustomPayload(Plugin src, CraftPlayer dst, ResourceLocation location, byte[] data);
    void Candyriya$registerAnonymousOutgoing(ResourceLocation location);
    CandyriyaPluginChannel<?> Candyriya$getAndCheckCrossSend(Plugin src, ResourceLocation channel);
    void Candyriya$checkUnsafeSend(Plugin src, ResourceLocation channel);

    PacketRecorder Candyriya$getPacketRecorder();
}
