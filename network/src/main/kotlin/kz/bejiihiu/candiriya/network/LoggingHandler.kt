package kz.bejiihiu.candiriya.network

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import org.apache.logging.log4j.LogManager

/**
 * Logs channel lifecycle, does nothing else.
 */
public class LoggingHandler : ChannelInboundHandlerAdapter() {
    private val logger = LogManager.getLogger(LoggingHandler::class.java)

    override fun channelActive(ctx: ChannelHandlerContext) {
        logger.info("channel active: {}", ctx.channel().remoteAddress())
        super.channelActive(ctx)
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.info("channel inactive: {}", ctx.channel().remoteAddress())
        super.channelInactive(ctx)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        logger.warn("exception on {}", ctx.channel().remoteAddress(), cause)
        ctx.close()
    }
}
