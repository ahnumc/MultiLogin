package moe.caa.multilogin.api.internal.logger.bridges

import moe.caa.multilogin.api.internal.logger.Level
import org.jetbrains.annotations.ApiStatus
import java.util.logging.Logger

/**
 * java.util.logging.Logger 日志程序桥接
 */
@ApiStatus.Internal
class JavaLoggerBridge(private val HANDLER: Logger) : BaseLoggerBridge() {
    private val levelMap = mapOf(
        Level.INFO to java.util.logging.Level.INFO,
        Level.WARN to java.util.logging.Level.WARNING,
        Level.ERROR to java.util.logging.Level.SEVERE
    )

    override fun log(level: Level?, message: String?, throwable: Throwable?) {
        levelMap[level]?.let { HANDLER.log(it, message, throwable) }
    }
}
