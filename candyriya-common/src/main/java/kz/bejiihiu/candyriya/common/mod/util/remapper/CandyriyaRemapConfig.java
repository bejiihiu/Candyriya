package kz.bejiihiu.candyriya.common.mod.util.remapper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/*
 * Used to record transformation detail for specific ClassLoaders.
 */
public record CandyriyaRemapConfig(boolean remap) {
    public static final CandyriyaRemapConfig PLUGIN = new CandyriyaRemapConfig(true);

    public CandyriyaRemapConfig copy() {
        return new CandyriyaRemapConfig(remap);
    }

    public int write(DataOutput output) throws IOException {
        output.writeBoolean(remap);
        return 1;
    }

    public static CandyriyaRemapConfig read(DataInput input) throws IOException {
        return new CandyriyaRemapConfig(input.readBoolean());
    }
}
