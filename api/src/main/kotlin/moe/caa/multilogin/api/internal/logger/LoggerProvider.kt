package moe.caa.multilogin.api.internal.logger

import moe.caa.multilogin.api.internal.logger.bridges.ConsoleBridge
import org.jetbrains.annotations.ApiStatus

/**
 * 日志提供程序
 */
@ApiStatus.Internal
object LoggerProvider {
    @JvmStatic
    var logger: Logger = ConsoleBridge()
}
