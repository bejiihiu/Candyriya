package kz.bejiihiu.candyriya.common.mixin.core.world.level;

import kz.bejiihiu.candyriya.api.EnumHelper;
import kz.bejiihiu.candyriya.common.mod.mixins.annotation.TransformAccess;
import net.minecraft.world.level.Level;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(Level.ExplosionInteraction.class)
public class Level_ExplosionInteractionMixin {

    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)
    private static final Level.ExplosionInteraction STANDARD = EnumHelper.makeEnum(Level.ExplosionInteraction.class, "STANDARD", Level.ExplosionInteraction.values().length, List.of(String.class), List.of("standard"));
}
