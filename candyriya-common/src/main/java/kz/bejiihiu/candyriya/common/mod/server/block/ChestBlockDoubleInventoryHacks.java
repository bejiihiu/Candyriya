package kz.bejiihiu.candyriya.common.mod.server.block;

import kz.bejiihiu.candyriya.api.Unsafe;
import kz.bejiihiu.candyriya.common.mod.util.remapper.CandyriyaRemapper;
import net.minecraft.world.CompoundContainer;

import java.lang.reflect.Field;

public class ChestBlockDoubleInventoryHacks {

    private static final Class<?> cl;
    private static final long offset;

    static {
        try {
            var className = CandyriyaRemapper.getNmsMapper().mapType("net/minecraft/world/level/block/BlockChest$2$1").replace('/', '.');
            cl = Class.forName(className);
            Field field = cl.getDeclaredField("inventorylargechest");
            offset = Unsafe.objectFieldOffset(field);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static CompoundContainer get(Object obj) {
        return (CompoundContainer) Unsafe.getObject(obj, offset);
    }

    public static boolean isInstance(Object obj) {
        return cl.isInstance(obj);
    }
}
