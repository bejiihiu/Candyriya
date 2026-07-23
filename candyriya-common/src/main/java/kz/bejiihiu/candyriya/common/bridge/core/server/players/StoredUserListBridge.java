package kz.bejiihiu.candyriya.common.bridge.core.server.players;

import java.util.Collection;

public interface StoredUserListBridge<V> {

    Collection<V> bridge$getValues();
}
