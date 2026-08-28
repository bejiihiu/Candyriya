package kz.bejiihiu.candiriya.launcher

import java.nio.file.Path
import java.nio.file.Paths
import kz.bejiihiu.candiriya.Candiriya
import kz.bejiihiu.candiriya.config.ConfigLoader
import org.apache.logging.log4j.LogManager

/**
 * Entry point. Parses --config and boots [Candiriya].
 */
public fun main(args: Array<String>) {
    val logger = LogManager.getLogger("Main")
    val configPath = parseConfigPath(args)
    logger.info("starting Candiriya with config {}", configPath)
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
    } catch (e: Exception) {
        logger.error("failed to start", e)
        System.exit(1)
        return
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
