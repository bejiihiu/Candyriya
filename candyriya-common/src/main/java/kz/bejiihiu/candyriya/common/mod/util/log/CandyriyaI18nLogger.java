package kz.bejiihiu.candyriya.common.mod.util.log;

import io.izzel.arclight.api.Unsafe;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

public class CandyriyaI18nLogger {

    private static final MethodHandle MH_GET_LOGGER;

    static {
        try {
            MH_GET_LOGGER = Unsafe.lookup().findStatic(Class.forName("kz.bejiihiu.candyriya.boot.log.CandyriyaI18nLogger"), "getLogger", MethodType.methodType(Logger.class, String.class));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Logger getLogger(String name) {
        try {
            return (Logger) MH_GET_LOGGER.invokeExact(name);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
}
