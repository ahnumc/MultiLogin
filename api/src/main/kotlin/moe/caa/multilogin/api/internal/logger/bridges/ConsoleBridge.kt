package moe.caa.multilogin.api.internal.logger.bridges

import moe.caa.multilogin.api.internal.logger.Level
import org.jetbrains.annotations.ApiStatus

/**
 * 控制台日志程序桥接
 */
@ApiStatus.Internal
class ConsoleBridge : BaseLoggerBridge() {
    override fun log(level: Level?, message: String?, throwable: Throwable?) {
        val out = if (level == Level.WARN || level == Level.ERROR) System.err else System.out
        out.println("[$level] $message")
        throwable?.printStackTrace(out)
    }
}
