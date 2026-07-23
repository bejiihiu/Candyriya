package kz.bejiihiu.candyriya.common.bridge.core.server.network;

import kz.bejiihiu.candyriya.common.bridge.core.server.network.ServerCommonPacketListenerImplBridge;
import io.izzel.tools.product.Product;
import io.izzel.tools.product.Product3;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerTeleportEvent;

public interface ServerGamePacketListenerImplBridge extends ServerCommonPacketListenerImplBridge {

    void bridge$pushTeleportCause(PlayerTeleportEvent.TeleportCause cause);

    void bridge$teleport(Location dest);

    void bridge$pushNoTeleportEvent();

    boolean bridge$teleportCancelled();

    default Product3<Boolean /* Cancelled */, ItemStack /* SwappedToMainHand */, ItemStack /* SwappedToOffHand */>
    bridge$platform$canSwapHandItems(LivingEntity entity) {
        return Product.of(false, entity.getOffhandItem(), entity.getMainHandItem());
    }

    default InteractionResult bridge$platform$onInteractEntityAt(ServerPlayer player, Entity entity, Vec3 vec,
                                                                 InteractionHand interactionHand) {
        return null;
    }

    void Candyriya$platform$setLastPosX(double d);
    void Candyriya$platform$setLastPosY(double d);
    void Candyriya$platform$setLastPosZ(double d);
    void Candyriya$platform$setLastPitch(float f);
    void Candyriya$platform$setLastYaw(float f);
}
