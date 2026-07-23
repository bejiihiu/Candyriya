package kz.bejiihiu.candyriya.boot.neoforge.application;

import io.izzel.arclight.api.ArclightPlatform;
import io.izzel.arclight.api.EnumHelper;
import io.izzel.arclight.api.Unsafe;
import kz.bejiihiu.candyriya.boot.AbstractBootstrap;
import kz.bejiihiu.candyriya.i18n.CandyriyaConfig;
import kz.bejiihiu.candyriya.i18n.CandyriyaLocale;

import java.util.Arrays;
import java.util.ServiceLoader;
import java.util.function.Consumer;

public class ApplicationBootstrap implements Consumer<String[]>, AbstractBootstrap {

    private static final int MIN_DEPRECATED_VERSION = 60;
    private static final int MIN_DEPRECATED_JAVA_VERSION = 16;

    @Override
    @SuppressWarnings("unchecked")
    public void accept(String[] args) {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
        System.setProperty("log4j.jul.LoggerAdapter", "kz.bejiihiu.candyriya.boot.log.CandyriyaLoggerAdapter");
        System.setProperty("log4j.configurationFile", "Candyriya-log4j2.xml");
        CandyriyaLocale.info("i18n.using-language", CandyriyaConfig.spec().getLocale().getCurrent(), CandyriyaConfig.spec().getLocale().getFallback());
        try {
            int javaVersion = (int) Float.parseFloat(System.getProperty("java.class.version"));
            if (javaVersion < MIN_DEPRECATED_VERSION) {
                CandyriyaLocale.error("java.deprecated", System.getProperty("java.version"), MIN_DEPRECATED_JAVA_VERSION);
                Thread.sleep(3000);
            }
            Unsafe.ensureClassInitialized(EnumHelper.class);
        } catch (Throwable t) {
            System.err.println("Your Java is not compatible with Candyriya.");
            t.printStackTrace();
            return;
        }
        try {
            this.setupMod(ArclightPlatform.NEOFORGE);
            this.dirtyHacks();
            int targetIndex = Arrays.asList(args).indexOf("--launchTarget");
            if (targetIndex >= 0 && targetIndex < args.length - 1) {
                args[targetIndex + 1] = "ArclightServer";
            }
            ServiceLoader.load(getClass().getModule().getLayer(), Consumer.class).stream()
                    .filter(it -> !it.type().getName().contains("Candyriya"))
                    .findFirst().orElseThrow().get().accept(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fail to launch Candyriya.");
        }
    }
}
