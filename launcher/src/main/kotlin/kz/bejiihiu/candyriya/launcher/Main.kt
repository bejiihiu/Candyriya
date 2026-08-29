package kz.bejiihiu.candyriya.launcher

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale
import kz.bejiihiu.candyriya.Candyriya
import kz.bejiihiu.candyriya.command.ConsoleSource
import kz.bejiihiu.candyriya.config.ConfigLoader
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.apache.logging.log4j.LogManager

/**
 * Entry point. Parses --config and boots [Candyriya].
 */
@SuppressFBWarnings(
    value = ["SA_LOCAL_SELF_ASSIGNMENT"],
    justification = "kotlin try-catch generates self assignment"
)
public fun main(args: Array<String>) {
    val startNs = System.nanoTime()
    val logger = LogManager.getLogger("Main")
    val configPath = parseConfigPath(args)
    logger.info("starting Candyriya with config {}", configPath)
    // sync load is ok on startup; future reloads should go via scheduler
    val config = try {
        ConfigLoader.load(configPath)
    } catch (e: Exception) {
        logger.error("failed to load config", e)
        System.exit(1)
        return
    }

    val candyriya = Candyriya(config)
    candyriya.addShutdownHook()
    try {
        candyriya.start()
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

    // further background tasks should go via candyriya.getScheduler() — single facade xd
    // example: candyriya.getScheduler().launch { /* coroutine example */ }
    // yep, go through scheduler, not raw threads xd
    candyriya.getScheduler().execute {
        logger.info(
            "launcher ready tick={}",
            candyriya.getTickScheduler().getCurrentTick()
        )
    }

    // console input — distinguishable ConsoleSource with all perms
    val consoleSource = ConsoleSource { msg ->
        // use logger so it respects level + file
        logger.info("[Console] {}", msg)
        // also print to stdout for visibility
        println(msg)
    }
    val consoleThread = Thread({
        val reader = System.`in`.bufferedReader()
        while (candyriya.getState() != kz.bejiihiu.candyriya.lifecycle.LifecycleState.STOPPED) {
            val line = try {
                reader.readLine()
            } catch (_: Exception) {
                null
            } ?: break
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            // handle stop separately
            if (trimmed.equals("stop", ignoreCase = true) || trimmed.equals("end", ignoreCase = true)) {
                consoleSource.sendMessage(Component.text("Stopping...", NamedTextColor.YELLOW))
                candyriya.stop()
                break
            }
            // dispatch via command manager — console is always allowed
            try {
                val dispatched = candyriya.getCommandManager().dispatch(consoleSource, trimmed)
                if (!dispatched) {
                    consoleSource.sendMessage(Component.text("Unknown command: $trimmed (try /candyriya help)", NamedTextColor.RED))
                }
            } catch (e: Exception) {
                logger.error("console command error", e)
                consoleSource.sendMessage(Component.text("Error: ${e.message}", NamedTextColor.RED))
            }
        }
    }, "candyriya-console")
    consoleThread.isDaemon = true
    consoleThread.start()

    // block until shutdown
    candyriya.awaitShutdown()
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
    return Paths.get("candyriya.toml")
}
