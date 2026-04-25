package moe.caa.multilogin.flows

/**
 * 加工异常
 */
class ProcessingFailedException : RuntimeException {
    constructor(message: String?) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause)

    constructor(cause: Throwable?) : super(cause)
}
