package kz.bejiihiu.candyriya.common.mixin.bukkit;

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import kz.bejiihiu.candyriya.common.bridge.bukkit.MessengerBridge;
import kz.bejiihiu.candyriya.common.mod.plugin.messaging.PacketRecorder;
import kz.bejiihiu.candyriya.common.mod.server.ArclightServer;
import kz.bejiihiu.candyriya.common.mod.plugin.messaging.CandyriyaPluginChannel;
import net.minecraft.resources.ResourceLocation;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.bukkit.plugin.messaging.StandardMessenger;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(value = StandardMessenger.class, remap = false)
public abstract class StandardMessengerMixin implements Messenger, MessengerBridge {

    @Shadow @Final private Map<String, Set<Plugin>> outgoingByChannel;

    @Shadow @Final private Map<String, Set<PluginMessageListenerRegistration>> incomingByChannel;

    @Unique
    private Map<ResourceLocation, CandyriyaPluginChannel<?>> Candyriya$registry;

    @Unique
    private SetMultimap<Plugin, ResourceLocation> Candyriya$crossSend;

    @Unique
    private PacketRecorder Candyriya$recorder;

    @ModifyConstant(
            method = "validateAndCorrectChannel",
            constant = @Constant(intValue = Messenger.MAX_CHANNEL_SIZE)
    )
    private static int Candyriya$modifyMaxChannelSize(int original) {
        return 256;
    }

    @Override
    public CandyriyaPluginChannel<?> Candyriya$getAndCheckCrossSend(Plugin src, ResourceLocation channel) {
        var Candyriya = this.Candyriya$registry.get(channel);
        if (src == null) {
            ArclightServer.LOGGER.warn("Sending anonymous packet on channel {}", channel);
        } else if (!Candyriya.getOutgoing().contains(src)) {
            boolean first;
            synchronized (this.Candyriya$crossSend) {
                first = this.Candyriya$crossSend.put(src, channel);
            }
            if (first) {
                ArclightServer.LOGGER.warn("A plugin is sending message on a channel that's registered as outgoing by other plugins but itself!");
                ArclightServer.LOGGER.warn("Plugin: [{}], on channel: {}", src.getDescription().getFullName(), channel);
                ArclightServer.LOGGER.warn("This warning will only be displayed once for every plugin and channel.");
            }
        }
        return Candyriya;
    }

    @Override
    public void Candyriya$checkUnsafeSend(Plugin src, ResourceLocation channel) {
        var Candyriya = Candyriya$registry.get(channel);
        if (Candyriya != null && !Candyriya.getOutgoing().isEmpty()) {
            return;
        }
        var fullName = src == null ? "Unknown" : src.getDescription().getFullName();
        if (src == null) {
            Candyriya$registerAnonymousOutgoing(channel);
        } else {
            registerOutgoingPluginChannel(src, channel.toString());
        }
        ArclightServer.LOGGER.warn("Plugin [{}] is sending message on an unregistered outgoing channel {}, registering.", fullName, channel);
    }

    @Override
    public void Candyriya$sendCustomPayload(Plugin src, CraftPlayer dst, ResourceLocation location, byte[] data) {
        Candyriya$checkUnsafeSend(src, location);
        var channel = Candyriya$getAndCheckCrossSend(src, location);
        channel.sendCustomPayload(src, dst, data);
    }

    @Override
    public void Candyriya$registerAnonymousOutgoing(ResourceLocation location) {
        Candyriya$updateChannel(location, true);
    }

    @Override
    public PacketRecorder Candyriya$getPacketRecorder() {
        return Candyriya$recorder;
    }

    @Unique
    private void Candyriya$updateChannel(ResourceLocation location, boolean create) {
        if (location != null) {
            var id = location.toString();
            var channel = Candyriya$registry.computeIfAbsent(location, it -> {
                if (!create) {
                    return null;
                }
                var inByChannel = incomingByChannel.computeIfAbsent(id, k -> new HashSet<>());
                var outByChannel = outgoingByChannel.computeIfAbsent(id, k -> new HashSet<>());
                return Candyriya$setupChannel(location, inByChannel, outByChannel);
            });
            if (channel != null) {
                channel.getChannelHandler().updateChannel();
            }
        }
    }

    @Unique
    private void Candyriya$updateChannel(String location, boolean create) {
        Candyriya$updateChannel(ResourceLocation.tryParse(location), create);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void Candyriya$init(CallbackInfo ci) {
        Candyriya$registry = new HashMap<>();
        Candyriya$crossSend = MultimapBuilder.hashKeys().hashSetValues().build();
        Candyriya$recorder = new PacketRecorder();
    }

    @Redirect(method = {"removeFromOutgoing*", "removeFromIncoming*"}, at = @At(value = "INVOKE", target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object Candyriya$skipRemove(Map<?, ?> thus, Object key) {
        return null;
    }

    @Inject(method = "addToOutgoing", at = @At("RETURN"))
    private void Candyriya$registerOut(Plugin plugin, String id, CallbackInfo ci) {
        Candyriya$updateChannel(id, true);
    }

    @Inject(method = "removeFromOutgoing(Lorg/bukkit/plugin/Plugin;Ljava/lang/String;)V", at = @At("RETURN"))
    private void Candyriya$unregisterOut(Plugin plugin, String id, CallbackInfo ci) {
        Candyriya$updateChannel(id, false);
    }

    @Inject(method = "addToIncoming", at = @At("RETURN"))
    private void Candyriya$registerIn(PluginMessageListenerRegistration registration, CallbackInfo ci) {
        Candyriya$updateChannel(registration.getChannel(), true);
    }

    @Inject(method = "removeFromIncoming(Lorg/bukkit/plugin/messaging/PluginMessageListenerRegistration;)V", at = @At("RETURN"))
    private void Candyriya$unregisterIn(PluginMessageListenerRegistration registration, CallbackInfo ci) {
        Candyriya$updateChannel(registration.getChannel(), false);
    }

    @Inject(method = "validateAndCorrectChannel", at = @At("TAIL"))
    private static void Candyriya$enhancedValidation(String channel, CallbackInfoReturnable<String> cir) {
        if (!valid.containsKey(channel)) {
            var corrected = cir.getReturnValue();
            var namespace = corrected.substring(0, corrected.indexOf(':'));
            var path = corrected.substring(corrected.indexOf(':') + 1);
            if (!ResourceLocation.isValidNamespace(namespace) || !ResourceLocation.isValidPath(path)) {
                ArclightServer.LOGGER.warn("Channel name is malformed and impossible to use: {}", corrected);
                ArclightServer.LOGGER.warn("Related functionality cannot be guaranteed!");
                ArclightServer.LOGGER.warn("This message will only be displayed once for this channel!");
                valid.put(channel, false);
            } else {
                valid.put(channel, true);
            }
        }
    }
}
