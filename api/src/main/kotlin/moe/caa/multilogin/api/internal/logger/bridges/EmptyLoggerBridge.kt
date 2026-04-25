package moe.caa.multilogin.api.internal.logger.bridges

import moe.caa.multilogin.api.internal.logger.Level
import org.jetbrains.annotations.ApiStatus

/**
 * 空日志程序桥接
 */
@ApiStatus.Internal
class EmptyLoggerBridge : BaseLoggerBridge() {
    override fun log(level: Level?, message: String?, throwable: Throwable?) {
    }
}
