package kz.bejiihiu.candyriya.common.mixin.core.network.protocol.game;

import kz.bejiihiu.candyriya.common.mod.mixins.annotation.CreateConstructor;
import kz.bejiihiu.candyriya.common.mod.mixins.annotation.ShadowConstructor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.bukkit.craftbukkit.v.util.CraftChatMessage;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientboundSystemChatPacket.class)
public class ClientboundSystemChatPacketMixin {

    @ShadowConstructor
    public void Candyriya$constructor(Component content, boolean overlay) {
        throw new RuntimeException();
    }

    @CreateConstructor
    public void Candyriya$constructor(BaseComponent[] content, boolean overlay) {
        Candyriya$constructor(CraftChatMessage.fromJSON(ComponentSerializer.toString(content)), overlay);
    }
}
