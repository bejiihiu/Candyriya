package kz.bejiihiu.candyriya.common.bridge.core.world.entity.vehicle;

public interface AbstractMinecartBridge {

    default boolean bridge$forge$canUseRail() {
        return true;
    }
}
