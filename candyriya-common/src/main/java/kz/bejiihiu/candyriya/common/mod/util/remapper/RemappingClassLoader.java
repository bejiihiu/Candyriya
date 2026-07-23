package kz.bejiihiu.candyriya.common.mod.util.remapper;

public interface RemappingClassLoader {

    ClassLoaderRemapper getRemapper();

    CandyriyaRemapConfig getRemapConfig();
}
