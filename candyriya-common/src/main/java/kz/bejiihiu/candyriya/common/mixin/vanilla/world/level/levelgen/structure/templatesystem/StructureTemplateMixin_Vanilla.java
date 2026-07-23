package kz.bejiihiu.candyriya.common.mixin.vanilla.world.level.levelgen.structure.templatesystem;

import kz.bejiihiu.candyriya.common.bridge.core.world.level.levelgen.structure.templatesystem.StructureTemplateBridge;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = StructureTemplate.class)
public abstract class StructureTemplateMixin_Vanilla implements StructureTemplateBridge {

}
