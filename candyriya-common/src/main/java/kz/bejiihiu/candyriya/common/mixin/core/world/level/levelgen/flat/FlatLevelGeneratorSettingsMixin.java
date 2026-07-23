package kz.bejiihiu.candyriya.common.mixin.core.world.level.levelgen.flat;

import kz.bejiihiu.candyriya.common.bridge.core.world.level.levelgen.flat.FlatLevelGeneratorSettingsBridge;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(FlatLevelGeneratorSettings.class)
public class FlatLevelGeneratorSettingsMixin implements FlatLevelGeneratorSettingsBridge {

    @Unique
    private BiomeSource Candyriya$biomeSource;

    @Override
    public void bridge$setBiomeSource(BiomeSource biomeSource) {
        Candyriya$biomeSource = biomeSource;
    }

    @Override
    public FlatLevelGeneratorSettings bridge$withBiomeSource(BiomeSource biomeSource) {
        Candyriya$biomeSource = biomeSource;
        return (FlatLevelGeneratorSettings) (Object) this;
    }

    @Override
    public BiomeSource bridge$getBiomeSource() {
        return Candyriya$biomeSource;
    }
}
