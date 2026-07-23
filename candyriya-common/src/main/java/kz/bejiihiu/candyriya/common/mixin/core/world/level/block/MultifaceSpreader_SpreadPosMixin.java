package kz.bejiihiu.candyriya.common.mixin.core.world.level.block;

import kz.bejiihiu.candyriya.common.bridge.core.world.level.block.MultifaceSpreaderSpreadPosBridge;
import kz.bejiihiu.candyriya.common.mod.mixins.annotation.CreateConstructor;
import kz.bejiihiu.candyriya.common.mod.mixins.annotation.ShadowConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.MultifaceSpreader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MultifaceSpreader.SpreadPos.class)
public abstract class MultifaceSpreader_SpreadPosMixin implements MultifaceSpreaderSpreadPosBridge {
    @Unique private BlockPos source;

    @ShadowConstructor
    abstract void Candyriya$constructor$this(BlockPos pos, Direction face);

    @SuppressWarnings("unused")
    @Unique
    @CreateConstructor
    public void Candyriya$constructor$new(BlockPos pos, Direction face, BlockPos source) {
        Candyriya$constructor$this(pos, face);
        Candyriya$setSource(source);
    }

    @Override
    public void Candyriya$setSource(BlockPos source) {
        this.source = source;
    }

    @Unique
    @Override
    public BlockPos source() {
        return source;
    }
}
