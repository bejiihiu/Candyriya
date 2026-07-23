package kz.bejiihiu.candyriya.forge;

import kz.bejiihiu.candyriya.api.Candyriya;
import kz.bejiihiu.candyriya.common.mod.server.CandyriyaServer;
import kz.bejiihiu.candyriya.forge.mod.ForgeCandyriyaServer;
import kz.bejiihiu.candyriya.forge.mod.event.CandyriyaEventDispatcherRegistry;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;
import java.io.PrintStream;

@Mod("Candyriya")
public class CandyriyaMod {

    public CandyriyaMod() {
        CandyriyaServer.LOGGER.info("mod-load");
        Candyriya.setServer(new ForgeCandyriyaServer());
        System.setOut(new LoggingPrintStream("STDOUT", System.out, Level.INFO));
        System.setErr(new LoggingPrintStream("STDERR", System.err, Level.ERROR));
        CandyriyaEventDispatcherRegistry.registerAllEventDispatchers();
    }

    private static class LoggingPrintStream extends PrintStream {

        private final Logger logger;
        private final Level level;

        public LoggingPrintStream(String name, @NotNull OutputStream out, Level level) {
            super(out);
            this.logger = LogManager.getLogger(name);
            this.level = level;
        }

        @Override
        public void println(@Nullable String x) {
            logger.log(level, x);
        }

        @Override
        public void println(@Nullable Object x) {
            logger.log(level, String.valueOf(x));
        }
    }
}
