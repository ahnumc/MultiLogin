package moe.caa.multilogin.api.internal.logger

import org.jetbrains.annotations.ApiStatus

/**
 * 一个日志记录程序
 */
@ApiStatus.Internal
interface Logger {
    /**
     * 记录一条日志
     * 
     * @param level     日志级别
     * @param message   日志信息
     * @param throwable 栈信息
     */
    fun log(level: Level?, message: String?, throwable: Throwable?)

    fun log(level: Level?, message: String?) {
        log(level, message, null)
    }

    fun log(level: Level?, throwable: Throwable?) {
        log(level, null, throwable)
    }

    fun debug(message: String?, throwable: Throwable?) {
        log(Level.DEBUG, message, throwable)
    }

    fun debug(message: String?) {
        log(Level.DEBUG, message)
    }

    fun debug(throwable: Throwable?) {
        log(Level.DEBUG, null, throwable)
    }

    fun info(message: String?, throwable: Throwable?) {
        log(Level.INFO, message, throwable)
    }

    fun info(message: String?) {
        log(Level.INFO, message)
    }

    fun info(throwable: Throwable?) {
        log(Level.INFO, null, throwable)
    }

    fun warn(message: String?, throwable: Throwable?) {
        log(Level.WARN, message, throwable)
    }

    fun warn(message: String?) {
        log(Level.WARN, message)
    }

    fun warn(throwable: Throwable?) {
        log(Level.WARN, null, throwable)
    }

    fun error(message: String?, throwable: Throwable?) {
        log(Level.ERROR, message, throwable)
    }

    fun error(message: String?) {
        log(Level.ERROR, message)
    }

    fun error(throwable: Throwable?) {
        log(Level.ERROR, null, throwable)
    }
}
