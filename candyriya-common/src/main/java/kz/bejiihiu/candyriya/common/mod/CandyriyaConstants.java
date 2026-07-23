package kz.bejiihiu.candyriya.common.mod;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.DSL;
import kz.bejiihiu.candyriya.api.EnumHelper;
import kz.bejiihiu.candyriya.api.Unsafe;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import org.bukkit.TreeType;

import java.util.List;

public class CandyriyaConstants {

    public static final TreeType MOD = EnumHelper.addEnum(TreeType.class, "MOD", ImmutableList.of(), ImmutableList.of());

    public static final Level.ExplosionInteraction STANDARD = Unsafe.getStatic(Level.ExplosionInteraction.class, "STANDARD");

    private static final DSL.TypeReference PDC_TYPE = () -> "bukkit_pdc";
    public static final DataFixTypes BUKKIT_PDC = EnumHelper.makeEnum(DataFixTypes.class, "BUKKIT_PDC", 0, List.of(DSL.TypeReference.class), List.of(PDC_TYPE));

    /**
     * Candyriya marker magic value for non-used custom dimension
     */
    public static final int Candyriya_DIMENSION = 0xA2c11947;

    public static final int PACKET_RECORDER_PERIOD_SEC = 5*60;

    public static final int MAX_C2S_CUSTOM_PAYLOAD_SIZE = 32767; //TODO: Change with update

    public static final int ANVIL_DEFAULT_DENIED_COST = -1;

    public static int currentTick;

}
