package kz.bejiihiu.candyriya.common.mixin.core.world.level.chunk;

import kz.bejiihiu.candyriya.common.bridge.core.world.level.chunk.LevelChunkSectionBridge;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LevelChunkSection.class)
public class LevelChunkSectionMixin implements LevelChunkSectionBridge {

    @Shadow private PalettedContainerRO<Holder<Biome>> biomes;

    public void setBiome(int i, int j, int k, Holder<net.minecraft.world.level.biome.Biome> biome) {
        ((PalettedContainer<Holder<Biome>>) this.biomes).set(i, j, k, biome);
    }

    @Override
    public void bridge$setBiome(int x, int y, int z, Holder<Biome> biome) {
        this.setBiome(x, y, z, biome);
    }
}
