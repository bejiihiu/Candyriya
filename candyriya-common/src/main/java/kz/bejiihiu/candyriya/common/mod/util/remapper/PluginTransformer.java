package kz.bejiihiu.candyriya.common.mod.util.remapper;

import org.objectweb.asm.tree.ClassNode;

public interface PluginTransformer {

    void handleClass(ClassNode node, ClassLoaderRemapper remapper, CandyriyaRemapConfig config);

    default int priority() {
        return 0;
    }
}
