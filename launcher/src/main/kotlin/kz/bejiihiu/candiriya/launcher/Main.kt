package kz.bejiihiu.candiriya.launcher

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale
import kz.bejiihiu.candiriya.Candiriya
import kz.bejiihiu.candiriya.config.ConfigLoader
import org.apache.logging.log4j.LogManager

/**
 * Entry point. Parses --config and boots [Candiriya].
 */
@SuppressFBWarnings(
    value = ["SA_LOCAL_SELF_ASSIGNMENT"],
    justification = "kotlin try-catch generates self assignment"
)
public fun main(args: Array<String>) {
    val startNs = System.nanoTime()
    val logger = LogManager.getLogger("Main")
    val configPath = parseConfigPath(args)
    logger.info("starting Candiriya with config {}", configPath)
    // sync load is ok on startup; future reloads should go via scheduler
    val config = try {
        ConfigLoader.load(configPath)
    } catch (e: Exception) {
        logger.error("failed to load config", e)
        System.exit(1)
        return
    }

    val candiriya = Candiriya(config)
    candiriya.addShutdownHook()
    try {
        candiriya.start()
        // startup timing xd
        val elapsedSec = (System.nanoTime() - startNs) / 1_000_000_000.0
        logger.info(
            "Done ({}s)! For help, type \"help\"",
            String.format(Locale.US, "%.3f", elapsedSec)
        )
    } catch (e: Exception) {
        val elapsedSec = (System.nanoTime() - startNs) / 1_000_000_000.0
        logger.error("Failed to start in {}s", String.format(Locale.US, "%.3f", elapsedSec), e)
        System.exit(1)
        return
    }

    // further background tasks should go via candiriya.getScheduler() — single facade xd
    // example: candiriya.getScheduler().launch { /* coroutine example */ }
    // yep, go through scheduler, not raw threads xd
    candiriya.getScheduler().execute {
        logger.info(
            "launcher ready tick={}",
            candiriya.getTickScheduler().getCurrentTick()
        )
    }

    // block until shutdown
    candiriya.awaitShutdown()
    logger.info("bye o/")
}

private fun parseConfigPath(args: Array<String>): Path {
    var idx = args.indexOf("--config")
    if (idx != -1 && idx + 1 < args.size) {
        return Paths.get(args[idx + 1])
    }
    // also support --config=path
    for (arg in args) {
        if (arg.startsWith("--config=")) {
            return Paths.get(arg.substringAfter("="))
        }
    }
    return Paths.get("candiriya.toml")
}
