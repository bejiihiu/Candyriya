package kz.bejiihiu.candyriya.common.mixin.core.world.level.block;

import kz.bejiihiu.candyriya.common.bridge.core.world.level.block.MultifaceSpreaderSpreadPosBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.MultifaceSpreader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {
        "net.minecraft.world.level.block.MultifaceSpreader$SpreadType$1",
        "net.minecraft.world.level.block.MultifaceSpreader$SpreadType$2",
        "net.minecraft.world.level.block.MultifaceSpreader$SpreadType$3"
})
public class MultifaceSpreader_SpreadTypeMixin {

    @Inject(method = "getSpreadPos", at = @At("RETURN"))
    private void Candyriya$attachSource(BlockPos pos, Direction face, Direction spreadDirection, CallbackInfoReturnable<MultifaceSpreader.SpreadPos> cir) {
        ((MultifaceSpreaderSpreadPosBridge)(Object) cir.getReturnValue()).Candyriya$setSource(pos);
    }
}
