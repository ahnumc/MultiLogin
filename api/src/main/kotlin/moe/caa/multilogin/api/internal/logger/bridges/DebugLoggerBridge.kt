package moe.caa.multilogin.api.internal.logger.bridges

import moe.caa.multilogin.api.internal.logger.Level
import moe.caa.multilogin.api.internal.logger.Logger
import moe.caa.multilogin.api.internal.logger.LoggerProvider
import org.jetbrains.annotations.ApiStatus

/**
 * 调试日志处理
 */
@ApiStatus.Internal
class DebugLoggerBridge(private val logger: Logger) : Logger {
    override fun log(level: Level?, message: String?, throwable: Throwable?) {
        var lvl = level
        var msg = message
        if (lvl == Level.DEBUG) {
            lvl = Level.INFO
            msg = "[DEBUG] $msg"
        }
        logger.log(lvl, msg, throwable)
    }

    companion object {
        @JvmStatic
        fun startDebugMode() {
            if (LoggerProvider.logger !is DebugLoggerBridge) {
                LoggerProvider.logger = DebugLoggerBridge(LoggerProvider.logger)
            }
        }

        @JvmStatic
        fun cancelDebugMode() {
            if (LoggerProvider.logger is DebugLoggerBridge) {
                LoggerProvider.logger = (LoggerProvider.logger as DebugLoggerBridge).logger
            }
        }
    }
}
