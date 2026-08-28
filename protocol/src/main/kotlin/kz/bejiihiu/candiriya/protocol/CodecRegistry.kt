package kz.bejiihiu.candiriya.protocol

import org.apache.logging.log4j.LogManager

/**
 * Placeholder for future Minecraft protocol codecs.
 * empty registry, just to reserve the module.
 */
public object CodecRegistry {
    private val logger = LogManager.getLogger(CodecRegistry::class.java)

    public fun init() {
        // nothing yet, just log so we know it's alive xd
        logger.debug("CodecRegistry init - no codecs registered yet")
    }
}
