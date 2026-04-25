package moe.caa.multilogin.loader.exception

/**
 * 加载初始化异常
 */
class InitialFailedException : RuntimeException {
    constructor(message: String?) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause)

    constructor(cause: Throwable?) : super(cause)
}
