package moe.caa.multilogin.core.ohc

import java.io.IOException

/**
 * 代表线程休眠异常
 */
class InterruptedRetryException : IOException {
    constructor()

    constructor(message: String?) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause)

    constructor(cause: Throwable?) : super(cause)
}
