package kz.bejiihiu.candyriya.common.mod.util.remapper.patcher;

import kz.bejiihiu.candyriya.common.mod.util.log.CandyriyaPluginLogger;
import kz.bejiihiu.candyriya.common.mod.util.remapper.CandyriyaRemapConfig;
import kz.bejiihiu.candyriya.common.mod.util.remapper.ClassLoaderRemapper;
import kz.bejiihiu.candyriya.common.mod.util.remapper.PluginTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

public class PluginLoggerTransformer implements PluginTransformer {

    @Override
    public void handleClass(ClassNode node, ClassLoaderRemapper remapper, CandyriyaRemapConfig config) {
        for (var mn : node.methods) {
            for (var insn : mn.instructions) {
                if (insn.getOpcode() == Opcodes.INVOKESTATIC && insn instanceof MethodInsnNode method
                    && method.owner.equals("java/util/logging/Logger") && method.name.equals("getLogger")) {
                    method.owner = Type.getInternalName(CandyriyaPluginLogger.class);
                }
            }
        }
    }
}
