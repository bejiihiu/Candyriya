package kz.bejiihiu.candyriya.forge.mod.plugin.messaging;

import com.google.common.base.Preconditions;
import kz.bejiihiu.candyriya.common.bridge.core.server.network.ServerCommonPacketListenerImplBridge;
import kz.bejiihiu.candyriya.common.mod.CandyriyaConstants;
import kz.bejiihiu.candyriya.common.mod.plugin.messaging.CandyriyaPluginChannel;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.EventNetworkChannel;
import net.minecraftforge.network.PacketDistributor;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;

public class CandyriyaForgePayloadHandler implements ForgePayloadHandler {

    private final CandyriyaPluginChannel<?> bukkit;
    private EventNetworkChannel forge;

    public CandyriyaForgePayloadHandler(CandyriyaPluginChannel<?> bukkit) {
        this.bukkit = bukkit;
    }

    public void initialize(EventNetworkChannel unconfigured) {
        forge = unconfigured.addListener(this);
    }

    @Override
    public void sendCustomPayload(Plugin src, CraftPlayer dst, byte[] data) {
        forge.send(new FriendlyByteBuf(Unpooled.wrappedBuffer(data)), PacketDistributor.PLAYER.with(dst.getHandle()));
    }

    @Override
    public CandyriyaPluginChannel<?> channel() {
        return bukkit;
    }

    @Override
    public void updateChannel() {

    }

    @Override
    public void accept(CustomPayloadEvent event) {
        var ctx = event.getSource();
        ctx.setPacketHandled(true);
        var buf = event.getPayload();
        final var max = CandyriyaConstants.MAX_C2S_CUSTOM_PAYLOAD_SIZE;
        Preconditions.checkArgument(buf.readableBytes() <= max, "Custom payload size may not be larger than " + max);

        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);

        ctx.enqueueWork(() -> {
            var listener = ctx.getConnection().getPacketListener();
            if (listener instanceof ServerCommonPacketListenerImplBridge bridge) {
                var craftbukkit = bridge.bridge$getCraftPlayer();
                bukkit.dispatchMessage(craftbukkit, data);
            }
        });
    }
}
