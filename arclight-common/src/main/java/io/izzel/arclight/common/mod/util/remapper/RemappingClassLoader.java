package io.izzel.arclight.common.mod.util.remapper;

public interface RemappingClassLoader {

    ClassLoaderRemapper getRemapper();

    ArclightRemapConfig getRemapConfig();

    /*
     * Redirect is applied for every cl;
     * To preserve proper delegation rule, we don't apply predicate recursively
     */
    static ClassLoader tryRedirect(ClassLoader parent) {
        return parent == ClassLoader.getSystemClassLoader() ? RemappingClassLoader.class.getClassLoader() : parent;
    }

    static boolean needRemap(ClassLoader cl) {
        // Removing redirect for PlatformClassLoader since only classes
        // in the standard library are loaded by PlatformClassLoader.
        // They are always related only to JDK we're using.
        var now = cl;
        final var needed = RemappingClassLoader.class.getClassLoader();
        do {
            if (now == ClassLoader.getSystemClassLoader() || now == needed) {
                return true;
            } else if (now == ClassLoader.getPlatformClassLoader() || now == null) {
                return false;
            }
            now = now.getParent();
        } while (now != null);
        return false;
    }
}
