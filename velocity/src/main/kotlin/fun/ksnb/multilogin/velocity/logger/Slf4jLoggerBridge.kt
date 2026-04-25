package `fun`.ksnb.multilogin.velocity.logger

import moe.caa.multilogin.api.internal.logger.Level
import moe.caa.multilogin.api.internal.logger.bridges.BaseLoggerBridge
import org.slf4j.Logger

/**
 * Slf4J 日志桥接程序
 */
class Slf4jLoggerBridge(private val logger: Logger) : BaseLoggerBridge() {
    override fun log(level: Level?, message: String?, throwable: Throwable?) {
        if (level == Level.DEBUG) {
            logger.debug(message, throwable)
        } else if (level == Level.INFO) {
            logger.info(message, throwable)
        } else if (level == Level.WARN) {
            logger.warn(message, throwable)
        } else if (level == Level.ERROR) {
            logger.error(message, throwable)
        }
    }
}
